package com.reprodutor.inteligente;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioFocusRequest;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;

import java.util.Calendar;

public class PlaybackService extends Service {
    public static final String ACTION_LOAD = "com.reprodutor.inteligente.LOAD";
    public static final String ACTION_STOP = "com.reprodutor.inteligente.STOP";
    public static final String ACTION_AUTO_START = "com.reprodutor.inteligente.AUTO_START";
    public static final String ACTION_MANUAL_PLAY = "com.reprodutor.inteligente.MANUAL_PLAY";
    public static final String ACTION_AUTO_STOP = "com.reprodutor.inteligente.AUTO_STOP";
    public static final String ACTION_PAUSE = "com.reprodutor.inteligente.PAUSE";
    public static final String ACTION_SET_PITCH = "com.reprodutor.inteligente.SET_PITCH";
    public static final String EXTRA_PITCH = "pitch";
    public static final String EXTRA_RESTORE = "restore";

    private static final String CHANNEL_ID = "reprodutor_playback";
    private static final int NOTIFICATION_ID = 41;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private MediaPlayer player;
    private Uri currentUri;
    private boolean playerReady = false;
    private boolean autoActive = false;
    private boolean autoPaused = false;
    private boolean autoPlaying = false;
    private boolean scheduleWindowActive = false;
    private boolean manualOverride = false;
    private long nextCycleChangeAt = 0L;
    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener focusListener;
    private AudioFocusRequest focusRequest;
    private boolean resumeAfterFocusGain = false;

    private final Runnable scheduleTick = new Runnable() {
        @Override public void run() {
            if (!autoActive || autoPaused) return;
            if (!playerReady || player == null) {
                handler.postDelayed(this, 1000L);
                return;
            }
            if (manualOverride) {
                if (!autoPlaying) autoPlaying = true;
                if (!player.isPlaying()) startPlayer();
                handler.postDelayed(this, 1000L);
                return;
            }
            boolean inside = isWithinAnySchedule();
            long now = SystemClock.elapsedRealtime();
            SharedPreferences prefs = getSharedPreferences("programacao", MODE_PRIVATE);
            if (!inside) {
                pausePlayer();
                autoPlaying = false;
                scheduleWindowActive = false;
                updateNotification("Automático ativo • aguardando horário");
            } else if (!scheduleWindowActive) {
                scheduleWindowActive = true;
                autoPlaying = true;
                startPlayer();
                nextCycleChangeAt = now + prefs.getInt("tocando", 5) * 60_000L;
                updateNotification("Programação ativa • reproduzindo");
            } else if (now >= nextCycleChangeAt) {
                if (autoPlaying) {
                    pausePlayer();
                    autoPlaying = false;
                    nextCycleChangeAt = now + prefs.getInt("parado", 5) * 60_000L;
                    updateNotification("Programação ativa • intervalo");
                } else {
                    autoPlaying = true;
                    startPlayer();
                    nextCycleChangeAt = now + prefs.getInt("tocando", 5) * 60_000L;
                    updateNotification("Programação ativa • reproduzindo");
                }
            }
            handler.postDelayed(this, 1000L);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        focusListener = change -> {
            if (change == AudioManager.AUDIOFOCUS_LOSS) {
                resumeAfterFocusGain = false;
                pausePlayer(false);
            } else if (change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                resumeAfterFocusGain = player != null && playerReady && player.isPlaying();
                pausePlayer(false);
            } else if (change == AudioManager.AUDIOFOCUS_GAIN && resumeAfterFocusGain) {
                resumeAfterFocusGain = false;
                // Não iniciar no callback de preparação. Apenas scheduleTick pode iniciar.
            }
        };
        createNotificationChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (ACTION_AUTO_START.equals(action)) {
            startForeground(NOTIFICATION_ID, buildNotification("Preparando reprodução"));
        }

        if (ACTION_LOAD.equals(action)) {
            Uri uri = intent.getData();
            if (uri != null) getSharedPreferences("programacao", MODE_PRIVATE).edit()
                    .putString("audio_uri", uri.toString()).apply();
            boolean restoring = intent.getBooleanExtra(EXTRA_RESTORE, false);
            if (uri != null && restoring && currentUri == null) {
                SharedPreferences prefs = getSharedPreferences("programacao", MODE_PRIVATE);
                autoActive = prefs.getBoolean("auto_active", false);
                autoPlaying = false;
                scheduleWindowActive = false;
                if (autoActive) {
                    startForeground(NOTIFICATION_ID, buildNotification("Restaurando programação automática"));
                }
                loadAudio(uri, false);
                if (autoActive) {
                    handler.removeCallbacks(scheduleTick);
                    handler.post(scheduleTick);
                }
            } else if (uri != null) {
                // Selecionar/trocar arquivo nunca mantém o automático ligado.
                autoActive = false;
                autoPlaying = false;
                scheduleWindowActive = false;
                handler.removeCallbacks(scheduleTick);
                getSharedPreferences("programacao", MODE_PRIVATE).edit()
                        .putBoolean("auto_active", false)
                        .putBoolean("manual_playing", false)
                        .apply();
                pausePlayer();
                stopForegroundCompat();
                if (!uri.equals(currentUri) || player == null) loadAudio(uri, false);
            }
        } else if (ACTION_MANUAL_PLAY.equals(action)) {
            if (autoActive) {
                manualOverride = true;
                autoPlaying = true;
                if (player != null && playerReady) startPlayer();
                updateNotification("Automático ativo • reprodução manual");
            }
        } else if (ACTION_PAUSE.equals(action)) {
            autoPaused = true;
            handler.removeCallbacks(scheduleTick);
            pausePlayer();
            updateNotification("Programação pausada");
        } else if (ACTION_STOP.equals(action) || ACTION_AUTO_STOP.equals(action)) {
            // PARAR cancela também a programação automática para impedir que
            // o ciclo volte a iniciar o áudio logo após o comando de parada.
            autoActive = false;
            autoPaused = false;
            autoPlaying = false;
            scheduleWindowActive = false;
            manualOverride = false;
            handler.removeCallbacks(scheduleTick);
            getSharedPreferences("programacao", MODE_PRIVATE).edit()
                    .putBoolean("auto_active", false)
                    .putBoolean("manual_playing", false)
                    .apply();
            pausePlayer();
            if (player != null && playerReady) {
                try { player.seekTo(0); } catch (Exception ignored) { }
            }
            stopForegroundCompat();
            persistPlaybackState();
        } else if (ACTION_AUTO_START.equals(action)) {
            autoActive = true;
            autoPaused = false;
            autoPlaying = false;
            scheduleWindowActive = false;
            manualOverride = false;
            nextCycleChangeAt = 0L;
            getSharedPreferences("programacao", MODE_PRIVATE).edit()
                    .putBoolean("auto_active", true).apply();
            loadSavedAudio(false);
            handler.removeCallbacks(scheduleTick);
            handler.post(scheduleTick);
            updateNotification("Automático ativo • aguardando horário");
        } else if (ACTION_SET_PITCH.equals(action)) {
            applyPitch(intent.getIntExtra(EXTRA_PITCH, 50));
        } else if (intent == null) {
            restoreAfterRestart();
        }
        return START_STICKY;
    }

    private void restoreAfterRestart() {
        SharedPreferences prefs = getSharedPreferences("programacao", MODE_PRIVATE);
        autoActive = prefs.getBoolean("auto_active", false);
        if (autoActive) {
            startForeground(NOTIFICATION_ID, buildNotification("Restaurando programação automática"));
            loadSavedAudio(false);
            handler.removeCallbacks(scheduleTick);
            handler.post(scheduleTick);
        }
    }

    private void loadSavedAudio(boolean playWhenReady) {
        String saved = getSharedPreferences("programacao", MODE_PRIVATE).getString("audio_uri", null);
        if (saved == null || saved.isEmpty()) return;
        try { loadAudio(Uri.parse(saved), playWhenReady); }
        catch (Exception e) {
            persistPlaybackState();
        }
    }

    private void loadAudio(Uri uri, boolean playWhenReady) {
        currentUri = uri;
        releasePlayer();
        // A reprodução só é iniciada pelo agendador automático.
        try {
            player = new MediaPlayer();
            player.setVolume(1f, 1f);
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            player.setLooping(true);
            player.setWakeMode(this, android.os.PowerManager.PARTIAL_WAKE_LOCK);
            player.setDataSource(this, uri);
            player.setOnPreparedListener(mp -> {
                playerReady = true;
                try { mp.pause(); mp.seekTo(0); } catch (Exception ignored) { }
                applyPitch(getSharedPreferences("programacao", MODE_PRIVATE).getInt("tonalidade", 50));
                // Nunca iniciar áudio ao terminar o carregamento; somente scheduleTick controla o play.
            });
            player.setOnErrorListener((mp, what, extra) -> {
                playerReady = false;
                persistPlaybackState();
                return true;
            });
            player.prepareAsync();
        } catch (Exception e) {
            releasePlayer();
            persistPlaybackState();
        }
    }

    private void startPlayer() {
        // Bloqueio absoluto: somente o agendador automático pode autorizar áudio.
        // Carregar/selecionar/restaurar um arquivo nunca inicia a reprodução.
        if (!autoActive || !autoPlaying) return;
        if (player == null || !playerReady) return;
        try {
            if (audioManager != null) {
                int focusResult;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (focusRequest == null) {
                        AudioAttributes attrs = new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
                        focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                                .setAudioAttributes(attrs)
                                .setOnAudioFocusChangeListener(focusListener)
                                .setWillPauseWhenDucked(true)
                                .build();
                    }
                    focusResult = audioManager.requestAudioFocus(focusRequest);
                } else {
                    focusResult = audioManager.requestAudioFocus(
                            focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                }
                if (focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    resumeAfterFocusGain = focusResult == AudioManager.AUDIOFOCUS_REQUEST_DELAYED;
                    return;
                }
            }
            resumeAfterFocusGain = false;
            if (!player.isPlaying()) player.start();
            persistPlaybackState();
            updateNotification(autoActive ? "Programação automática ativa" : "Reproduzindo áudio");
        } catch (Exception ignored) { }
    }

    private void pausePlayer() {
        pausePlayer(true);
    }

    private void pausePlayer(boolean abandonFocus) {
        if (player != null && playerReady) {
            try { if (player.isPlaying()) player.pause(); } catch (Exception ignored) { }
        }
        if (abandonFocus && audioManager != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
                    audioManager.abandonAudioFocusRequest(focusRequest);
                    focusRequest = null;
                } else {
                    audioManager.abandonAudioFocus(focusListener);
                }
            } catch (Exception ignored) { }
            resumeAfterFocusGain = false;
        }
    }

    private void persistPlaybackState() {
        getSharedPreferences("programacao", MODE_PRIVATE).edit()
                .putBoolean("manual_playing", false).apply();
    }

    private boolean isWithinAnySchedule() {
        Calendar cal = Calendar.getInstance();
        int current = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        SharedPreferences prefs = getSharedPreferences("programacao", MODE_PRIVATE);
        for (int i = 1; i <= 4; i++) {
            String start = prefs.getString("inicio" + i, i == 1 ? prefs.getString("inicio", "Não definido") : "Não definido");
            String end = prefs.getString("fim" + i, i == 1 ? prefs.getString("fim", "Não definido") : "Não definido");
            if (start == null || end == null || "Não definido".equals(start) || "Não definido".equals(end)) continue;
            try {
                String[] a = start.split(":");
                String[] b = end.split(":");
                int from = Integer.parseInt(a[0]) * 60 + Integer.parseInt(a[1]);
                int to = Integer.parseInt(b[0]) * 60 + Integer.parseInt(b[1]);
                if (from == to) continue;
                if (from < to ? current >= from && current < to : current >= from || current < to) return true;
            } catch (Exception ignored) { }
        }
        return false;
    }

    private void applyPitch(int value) {
        if (player == null || !playerReady) return;
        try {
            float pitch = value < 50 ? 0.5f + (value / 50f) * 0.5f : 1.0f + ((value - 50) / 50f);
            PlaybackParams params = new PlaybackParams();
            params.setSpeed(1.0f);
            params.setPitch(pitch);
            player.setPlaybackParams(params);
        } catch (Exception ignored) { }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Reprodução de áudio", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Mantém o áudio e a programação ativos em segundo plano");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String message) {
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        return builder.setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("Reprodutor Inteligente")
                .setContentText(message).setOngoing(true).build();
    }

    private void updateNotification(String message) {
        if (autoActive) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) manager.notify(NOTIFICATION_ID, buildNotification(message));
        }
    }

    private void stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_REMOVE);
        else stopForeground(true);
    }

    private void releasePlayer() {
        playerReady = false;
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) { }
            try { player.release(); } catch (Exception ignored) { }
            player = null;
        }
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(scheduleTick);
        releasePlayer();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
