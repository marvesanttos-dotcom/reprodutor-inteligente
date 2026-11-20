package com.reprodutor.inteligente;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.PlaybackParams;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Build;
import java.util.Calendar;
import android.speech.RecognizerIntent;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private MediaPlayer player;
    private boolean playerReady = false;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private boolean resumeAfterFocusLoss = false;
    private final AudioManager.OnAudioFocusChangeListener audioFocusListener = change -> {
        // Sons breves do sistema/notificações não devem interromper o canto.
        // O Android pode reduzir o volume automaticamente durante o ducking.
        if (change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
                || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            return;
        }
        if (change == AudioManager.AUDIOFOCUS_GAIN) {
            if (resumeAfterFocusLoss && player != null && playerReady) {
                try {
                    if (!player.isPlaying()) player.start();
                    startBackgroundPlayback();
                } catch (Exception ignored) { }
            }
            resumeAfterFocusLoss = false;
        } else if (change == AudioManager.AUDIOFOCUS_LOSS) {
            resumeAfterFocusLoss = false;
            if (player != null && playerReady && player.isPlaying()) {
                try { player.pause(); } catch (Exception ignored) { }
            }
        }
    };
    private Equalizer equalizer;
    private short equalizerMin = -1500;
    private short equalizerMax = 1500;
    private final Handler cycleHandler = new Handler(Looper.getMainLooper());
    private boolean cycleActive = false;
    private boolean cyclePlaying = false;
    private boolean scheduleWindowActive = false;
    private long nextCycleChangeAt = 0L;
    private Button cycleButton;
    private final Runnable scheduleTick = new Runnable() {
        @Override public void run() {
            if (!cycleActive) return;
            if (player == null || !playerReady) { stopCycle(); return; }
            boolean inside = isWithinAnySchedule();
            long now = SystemClock.elapsedRealtime();
            SharedPreferences prefs = getSharedPreferences("programacao", MODE_PRIVATE);
            if (!inside) {
                if (player.isPlaying()) { try { player.pause(); } catch (Exception ignored) { } }
                cyclePlaying = false;
                scheduleWindowActive = false;
                status.setText("Automático ativo • aguardando próximo horário");
            } else if (!scheduleWindowActive) {
                scheduleWindowActive = true;
                cyclePlaying = true;
                try {
                    if (player.getCurrentPosition() >= player.getDuration()) player.seekTo(0);
                    startPlayerWithFocus();
                    nextCycleChangeAt = now + prefs.getInt("tocando", 5) * 60L * 1000L;
                    status.setText("Programação ativa • reproduzindo");
                } catch (Exception e) { stopCycle(); status.setText("Não foi possível iniciar o áudio"); return; }
            } else if (now >= nextCycleChangeAt) {
                if (cyclePlaying) {
                    try { if (player.isPlaying()) player.pause(); } catch (Exception ignored) { }
                    cyclePlaying = false;
                    nextCycleChangeAt = now + prefs.getInt("parado", 5) * 60L * 1000L;
                    status.setText("Programação ativa • intervalo de pausa");
                } else {
                    try {
                        if (player.getCurrentPosition() >= player.getDuration()) player.seekTo(0);
                        startPlayerWithFocus();
                        cyclePlaying = true;
                        nextCycleChangeAt = now + prefs.getInt("tocando", 5) * 60L * 1000L;
                        status.setText("Programação ativa • reproduzindo");
                    } catch (Exception e) { stopCycle(); status.setText("Não foi possível iniciar o áudio"); return; }
                }
            }
            cycleHandler.postDelayed(this, 1000L);
        }
    };
    private final Button[] configButtons = new Button[8];
    private TextView status;
    private TextView selectedFile;
    private static final int PICK_AUDIO = 10;
    private static final int SPEECH = 11;
    private final int BG = Color.BLACK;
    private final int PANEL = Color.rgb(16, 16, 16);
    private final int GREEN = Color.rgb(255, 215, 0);
    private final int WHITE = Color.WHITE;
    private final int MUTED = Color.rgb(190, 190, 190);

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(28), dp(22), dp(26));
        scroll.addView(page);

        LinearLayout brand = new LinearLayout(this);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        brand.setOrientation(LinearLayout.HORIZONTAL);
        TextView logo = new TextView(this);
        logo.setText("♫");
        logo.setTextSize(29);
        logo.setGravity(Gravity.CENTER);
        logo.setTextColor(Color.rgb(255, 225, 80));
        logo.setBackground(goldGradient(100));
        brand.addView(logo, new LinearLayout.LayoutParams(dp(58), dp(58)));
        LinearLayout brandText = new LinearLayout(this);
        brandText.setOrientation(LinearLayout.VERTICAL);
        brandText.setPadding(dp(14), 0, 0, 0);
        TextView title = label("PÁSSARO", 19, GREEN, true);
        TextView sub = label("PROFESSOR  •  CANTOS", 11, MUTED, true);
        brandText.addView(title);
        brandText.addView(sub);
        brand.addView(brandText);
        page.addView(brand);

        TextView section = label("SEU ESTÚDIO DE SONS", 12, MUTED, true);
        section.setPadding(0, dp(30), 0, dp(12));
        page.addView(section);

        LinearLayout playerCard = new LinearLayout(this);
        playerCard.setOrientation(LinearLayout.VERTICAL);
        playerCard.setGravity(Gravity.CENTER_HORIZONTAL);
        playerCard.setPadding(dp(18), dp(24), dp(18), dp(22));
        playerCard.setBackground(round(PANEL, 24));
        TextView disc = label("♫", 54, GREEN, true);
        disc.setGravity(Gravity.CENTER);
        disc.setBackground(goldGradient(100));
        LinearLayout.LayoutParams discParams = new LinearLayout.LayoutParams(dp(112), dp(112));
        discParams.bottomMargin = dp(18);
        playerCard.addView(disc, discParams);

        selectedFile = label("Nenhum áudio selecionado", 17, WHITE, true);
        selectedFile.setGravity(Gravity.CENTER);
        playerCard.addView(selectedFile);
        status = label("Pronto para começar", 13, MUTED, false);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(8), 0, dp(20));
        playerCard.addView(status);

        Button choose = makeButton("＋   ESCOLHER ÁUDIO", GREEN, BG, v -> chooseAudio());
        playerCard.addView(choose, match());
        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        Button stop = makeButton("■", Color.rgb(35, 35, 35), WHITE, v -> stopAudio());
        Button play = makeButton("▶  REPRODUZIR", GREEN, BG, v -> playAudio());
        Button pause = makeButton("Ⅱ", Color.rgb(35, 35, 35), WHITE, v -> pauseAudio());
        LinearLayout.LayoutParams side = new LinearLayout.LayoutParams(0, dp(54), 1);
        side.setMargins(dp(4), dp(12), dp(4), 0);
        controls.addView(stop, side);
        LinearLayout.LayoutParams middle = new LinearLayout.LayoutParams(0, dp(54), 2);
        middle.setMargins(dp(4), dp(12), dp(4), 0);
        controls.addView(play, middle);
        LinearLayout.LayoutParams side2 = new LinearLayout.LayoutParams(0, dp(54), 1);
        side2.setMargins(dp(4), dp(12), dp(4), 0);
        controls.addView(pause, side2);
        playerCard.addView(controls);
        page.addView(playerCard);

        Button audioSettings = makeButton("♫  TONALIDADE • CANTO MAIS GROSSO OU MAIS FINO", PANEL, WHITE, v -> showAudioSettings());
        LinearLayout.LayoutParams audioSettingsParams = match();
        audioSettingsParams.topMargin = dp(12);
        page.addView(audioSettings, audioSettingsParams);

        Button voiceControl = makeButton("🎙  CONTROLE POR VOZ", PANEL, WHITE, v -> startSpeech());
        LinearLayout.LayoutParams voiceControlParams = match();
        voiceControlParams.topMargin = dp(8);
        page.addView(voiceControl, voiceControlParams);

        TextView manualTitle = label("CONTROLE MANUAL", 12, MUTED, true);
        manualTitle.setPadding(0, dp(22), 0, dp(10));
        page.addView(manualTitle);
        TextView scheduleTitle = label("PROGRAMAÇÕES AUTOMÁTICAS", 12, MUTED, true);
        scheduleTitle.setPadding(0, dp(25), 0, dp(10));
        page.addView(scheduleTitle);
        LinearLayout scheduleHeader = new LinearLayout(this);
        scheduleHeader.setOrientation(LinearLayout.HORIZONTAL);
        scheduleHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView scheduleInfo = label("Configure até 4 horários de funcionamento.", 13, WHITE, false);
        scheduleHeader.addView(scheduleInfo, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button resetSchedules = makeButton("↻", Color.TRANSPARENT, WHITE, v -> confirmResetSchedules());
        resetSchedules.setTextSize(29);
        resetSchedules.setBackgroundColor(Color.TRANSPARENT);
        resetSchedules.setElevation(0);
        resetSchedules.setPadding(0, 0, 0, 0);
        scheduleHeader.addView(resetSchedules, new LinearLayout.LayoutParams(dp(48), dp(48)));
        page.addView(scheduleHeader);

        SharedPreferences prefs = getSharedPreferences("programacao", MODE_PRIVATE);
        for (int i = 1; i <= 4; i++) {
            final int slot = i;
            String startKey = "inicio" + i;
            String endKey = "fim" + i;
            String startValue = prefs.getString(startKey, i == 1 ? prefs.getString("inicio", "Não definido") : "Não definido");
            String endValue = prefs.getString(endKey, i == 1 ? prefs.getString("fim", "Não definido") : "Não definido");
            configButtons[(i - 1) * 2] = makeButton("PROGRAMAÇÃO " + i + " • LIGA: " + displayAmPm(startValue), WHITE, BG,
                    v -> chooseTime("inicio" + slot, "Programação " + slot + " • horário para ligar", configButtons[(slot - 1) * 2]));
            configButtons[(i - 1) * 2 + 1] = makeButton("PROGRAMAÇÃO " + i + " • DESLIGA: " + displayAmPm(endValue), PANEL, WHITE,
                    v -> chooseTime("fim" + slot, "Programação " + slot + " • horário para desligar", configButtons[(slot - 1) * 2 + 1]));
        }
        for (Button b : configButtons) {
            LinearLayout.LayoutParams configParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
            configParams.topMargin = dp(7);
            page.addView(b, configParams);
        }
        Button playingTime = makeButton("▶  TEMPO TOCANDO: " + prefs.getInt("tocando", 5) + " MIN", PANEL, WHITE,
                v -> chooseMinutes("tocando", "Tempo tocando (minutos)", configButtons[0]));
        page.addView(playingTime, match());
        Button stoppedTime = makeButton("Ⅱ  TEMPO PARADO: " + prefs.getInt("parado", 5) + " MIN", PANEL, WHITE,
                v -> chooseMinutes("parado", "Tempo parado (minutos)", configButtons[0]));
        LinearLayout.LayoutParams stoppedParams = match();
        stoppedParams.topMargin = dp(7);
        page.addView(stoppedTime, stoppedParams);
        TextView autoTitle = label("CONTROLE AUTOMÁTICO", 12, MUTED, true);
        autoTitle.setPadding(0, dp(22), 0, dp(4));
        page.addView(autoTitle);
        cycleButton = makeButton("▶  INICIAR CICLO AUTOMÁTICO", WHITE, BG, v -> toggleCycle());
        LinearLayout.LayoutParams cycleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
        cycleParams.topMargin = dp(16);
        page.addView(cycleButton, cycleParams);

        TextView footer = label("MAIS RECURSOS INTELIGENTES EM DESENVOLVIMENTO", 9, MUTED, true);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(28), 0, 0);
        page.addView(footer);
        setContentView(scroll);
    }

    private String displayAmPm(String value) {
        if (value == null || value.equals("Não definido")) return value;
        try {
            String[] parts = value.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            String suffix = hour < 12 ? "AM" : "PM";
            int hour12 = hour % 12;
            if (hour12 == 0) hour12 = 12;
            return String.format(Locale.US, "%02d:%02d %s", hour12, minute, suffix);
        } catch (Exception e) { return value; }
    }

    private void confirmResetSchedules() {
        new AlertDialog.Builder(this)
                .setTitle("Zerar programações?")
                .setMessage("Isso apagará os horários de ligar e desligar das quatro programações. Os tempos tocando e parado serão mantidos.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Zerar", (dialog, which) -> {
                    SharedPreferences prefs = getSharedPreferences("programacao", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    for (int i = 1; i <= 4; i++) {
                        editor.remove("inicio" + i);
                        editor.remove("fim" + i);
                    }
                    editor.remove("inicio");
                    editor.remove("fim");
                    editor.apply();
                    for (int i = 0; i < 8; i++) {
                        int slot = i / 2 + 1;
                        String label = i % 2 == 0
                                ? "PROGRAMAÇÃO " + slot + " • LIGA: Não definido"
                                : "PROGRAMAÇÃO " + slot + " • DESLIGA: Não definido";
                        configButtons[i].setText(label);
                    }
                    if (cycleActive) {
                        stopCycle();
                        status.setText("Programações zeradas • automático parado");
                    }
                    Toast.makeText(this, "Horários apagados", Toast.LENGTH_SHORT).show();
                }).show();
    }

    private void chooseTime(String key, String title, Button target) {
        SharedPreferences prefs = getSharedPreferences("programacao", MODE_PRIVATE);
        String fallback = key.equals("inicio1") ? prefs.getString("inicio", "08:00") :
                key.equals("fim1") ? prefs.getString("fim", "08:00") : "08:00";
        String current = prefs.getString(key, fallback);
        if (current.equals("Não definido")) current = "08:00";
        String[] parts = current.split(":");
        int hour = 8, minute = 0;
        try { hour = Integer.parseInt(parts[0]); minute = Integer.parseInt(parts[1]); }
        catch (Exception ignored) { }
        new TimePickerDialog(this, (view, h, m) -> {
            String value = String.format(Locale.getDefault(), "%02d:%02d", h, m);
            prefs.edit().putString(key, value).apply();
            String number = key.substring(key.length() - 1);
            String prefix = key.startsWith("inicio") ? "PROGRAMAÇÃO " + number + " • LIGA: " : "PROGRAMAÇÃO " + number + " • DESLIGA: ";
            target.setText(prefix + displayAmPm(value));
        }, hour, minute, false).show();
    }

    private void chooseMinutes(String key, String title, Button target) {
        SharedPreferences prefs = getSharedPreferences("programacao", MODE_PRIVATE);
        EditText input = new EditText(this);
        input.setInputType(2);
        input.setSingleLine(true);
        input.setHint("Minutos");
        input.setText(String.valueOf(prefs.getInt(key, 5)));
        new AlertDialog.Builder(this).setTitle(title).setView(input)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", (dialog, which) -> {
                    try {
                        int value = Integer.parseInt(input.getText().toString().trim());
                        if (value < 1 || value > 1440) throw new NumberFormatException();
                        prefs.edit().putInt(key, value).apply();
                        String prefix = key.equals("tocando") ? "▶  TEMPO TOCANDO: " : "Ⅱ  TEMPO PARADO: ";
                        target.setText(prefix + value + " MIN");
                    } catch (Exception e) {
                        Toast.makeText(this, "Digite um valor entre 1 e 1440 minutos", Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }

    private GradientDrawable goldGradient(int radius) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[] { Color.rgb(255, 248, 170), Color.rgb(255, 215, 0), Color.rgb(218, 145, 0), Color.rgb(255, 236, 105) });
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), Color.rgb(255, 242, 160));
        return d;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private LinearLayout.LayoutParams match() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
    }

    private Button makeButton(String text, int background, int foreground, View.OnClickListener action) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(13);
        b.setTextColor(foreground);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(background == GREEN ? goldGradient(16) : round(background, 16));
        b.setElevation(dp(2));
        b.setOnClickListener(action);
        return b;
    }

    private void chooseAudio() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_AUDIO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_AUDIO && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                if (uri == null) return;
                try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
                catch (Exception ignored) { }
                stopCycle();
                releaseEqualizer();
                releasePlayer();
                playerReady = false;
                player = new MediaPlayer();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build();
                    player.setAudioAttributes(playbackAttributes);
                }
                player.setLooping(true);
                player.setWakeMode(this, android.os.PowerManager.PARTIAL_WAKE_LOCK);
                player.setDataSource(this, uri);
                player.setOnPreparedListener(mp -> {
                    playerReady = true;
                    initializeEqualizer(mp.getAudioSessionId());
                    applyPitch(getSharedPreferences("programacao", MODE_PRIVATE).getInt("tonalidade", 50));
                    status.setText("Áudio carregado. Pronto para reproduzir.");
                    selectedFile.setText("♫  " + uri.getLastPathSegment());
                });
                player.setOnCompletionListener(mp -> {
                    if (cycleActive && cyclePlaying) {
                        try { mp.seekTo(0); startPlayerWithFocus(); } catch (Exception ignored) { }
                    } else status.setText("Reprodução concluída");
                });
                player.setOnErrorListener((mp, what, extra) -> {
                    status.setText("Não foi possível reproduzir este áudio");
                    return true;
                });
                status.setText("Carregando áudio...");
                player.prepareAsync();
            } catch (Exception e) {
                releasePlayer();
                status.setText("Erro ao abrir áudio");
                Toast.makeText(this, "Não foi possível abrir esse arquivo", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == SPEECH && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) handleVoiceCommand(results.get(0));
        }
    }

    private boolean isWithinAnySchedule() {
        Calendar cal = Calendar.getInstance();
        int current = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        SharedPreferences prefs = getSharedPreferences("programacao", MODE_PRIVATE);
        for (int i = 1; i <= 4; i++) {
            String start = prefs.getString("inicio" + i, i == 1 ? prefs.getString("inicio", "Não definido") : "Não definido");
            String end = prefs.getString("fim" + i, i == 1 ? prefs.getString("fim", "Não definido") : "Não definido");
            if (start.equals("Não definido") || end.equals("Não definido")) continue;
            try {
                String[] a = start.split(":");
                String[] b = end.split(":");
                int from = Integer.parseInt(a[0]) * 60 + Integer.parseInt(a[1]);
                int to = Integer.parseInt(b[0]) * 60 + Integer.parseInt(b[1]);
                if (from == to) continue;
                boolean active = from < to ? current >= from && current < to : current >= from || current < to;
                if (active) return true;
            } catch (Exception ignored) { }
        }
        return false;
    }

    private void toggleCycle() {
        if (cycleActive) {
            stopCycle();
            status.setText("Ciclo automático parado");
            return;
        }
        if (player == null || !playerReady) {
            Toast.makeText(this, "Escolha um áudio e aguarde carregar", Toast.LENGTH_SHORT).show();
            return;
        }
        cycleActive = true;
        cyclePlaying = false;
        cycleButton.setText("■  PARAR CICLO AUTOMÁTICO");
        scheduleWindowActive = false;
        nextCycleChangeAt = 0L;
        cycleHandler.removeCallbacks(scheduleTick);
        status.setText("Automático ativo • aguardando programação");
        scheduleTick.run();
    }

    private void stopCycle() {
        cycleActive = false;
        cyclePlaying = false;
        cycleHandler.removeCallbacks(scheduleTick);
        scheduleWindowActive = false;
        if (cycleButton != null) cycleButton.setText("▶  INICIAR CICLO AUTOMÁTICO");
    }

    private void requestPlaybackAudioFocus() {
        if (audioManager == null) audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest == null) {
                AudioAttributes attributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(attributes)
                        .setAcceptsDelayedFocusGain(true)
                        .setWillPauseWhenDucked(false)
                        .setOnAudioFocusChangeListener(audioFocusListener)
                        .build();
            }
            audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            audioManager.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    private void abandonPlaybackAudioFocus() {
        resumeAfterFocusLoss = false;
        if (audioManager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
        } else {
            audioManager.abandonAudioFocus(audioFocusListener);
        }
    }

    private void startPlayerWithFocus() {
        requestPlaybackAudioFocus();
        player.start();
        startBackgroundPlayback();
    }

    private void playAudio() {
        if (cycleActive) {
            stopCycle();
            status.setText("Modo manual");
        }
        if (player == null) {
            Toast.makeText(this, "Escolha um áudio primeiro", Toast.LENGTH_SHORT).show();
            return;
        }
        try { startPlayerWithFocus(); status.setText("Reproduzindo áudio"); }
        catch (IllegalStateException e) { status.setText("Selecione o áudio novamente"); }
    }

    private void pauseAudio() {
        if (cycleActive) {
            stopCycle();
            status.setText("Modo manual");
        }
        if (player != null && player.isPlaying()) {
            player.pause();
            stopBackgroundPlayback();
            abandonPlaybackAudioFocus();
            status.setText("Áudio pausado");
        } else status.setText("Nenhum áudio está tocando");
    }

    private void stopAudio() {
        stopCycle();
        if (player != null) {
            try {
                if (player.isPlaying()) player.pause();
                stopBackgroundPlayback();
                abandonPlaybackAudioFocus();
                player.seekTo(0);
                status.setText("Áudio parado");
            } catch (IllegalStateException e) { status.setText("Selecione o áudio novamente"); }
        } else status.setText("Escolha um áudio primeiro");
    }

    private void handleVoiceCommand(String spoken) {
        String command = Normalizer.normalize(spoken.toLowerCase(new Locale("pt", "BR")), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        if (command.contains("escolher") || command.contains("abrir audio") || command.contains("selecionar audio")) {
            status.setText("Abrindo seus áudios"); chooseAudio();
        } else if (command.contains("pausar") || command.equals("pause")) pauseAudio();
        else if (command.contains("parar")) stopAudio();
        else if (command.contains("reproduzir") || command.contains("tocar") || command.contains("continuar") || command.contains("play")) playAudio();
        else status.setText("Você disse: " + spoken + ". Tente reproduzir, pausar, parar ou escolher áudio.");
    }

    private void startSpeech() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR");
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Diga: reproduzir, pausar, parar ou escolher áudio");
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
            startActivityForResult(intent, SPEECH);
        } catch (Exception e) {
            Toast.makeText(this, "Reconhecimento de voz não disponível neste aparelho", Toast.LENGTH_LONG).show();
        }
    }

    private void initializeEqualizer(int audioSessionId) {
        releaseEqualizer();
        try {
            equalizer = new Equalizer(0, audioSessionId);
            equalizer.setEnabled(true);
            equalizerMin = equalizer.getBandLevelRange()[0];
            equalizerMax = equalizer.getBandLevelRange()[1];
            SharedPreferences prefs = getSharedPreferences("programacao", MODE_PRIVATE);
            applyEqualizer(prefs.getInt("graves", 50), prefs.getInt("agudos", 50));
        } catch (Exception e) {
            releaseEqualizer();
            status.setText("Áudio carregado • equalizador indisponível neste aparelho");
        }
    }

    private void applyEqualizer(int bass, int treble) {
        if (equalizer == null) return;
        try {
            short bands = equalizer.getNumberOfBands();
            if (bands < 1) return;

            // 50% é neutro. Os extremos usam toda a faixa suportada pelo aparelho.
            int bassLevel = bass >= 50
                    ? (equalizerMax * (bass - 50)) / 50
                    : (equalizerMin * (50 - bass)) / 50;
            int trebleLevel = treble >= 50
                    ? (equalizerMax * (treble - 50)) / 50
                    : (equalizerMin * (50 - treble)) / 50;

            for (short band = 0; band < bands; band++) {
                int centerHz = equalizer.getCenterFreq(band) / 1000;
                if (centerHz <= 250) {
                    equalizer.setBandLevel(band, (short) bassLevel);
                } else if (centerHz >= 4000) {
                    equalizer.setBandLevel(band, (short) trebleLevel);
                } else {
                    equalizer.setBandLevel(band, (short) 0);
                }
            }
        } catch (Exception ignored) { }
    }

    private void releaseEqualizer() {
        if (equalizer != null) {
            try { equalizer.setEnabled(false); } catch (Exception ignored) { }
            try { equalizer.release(); } catch (Exception ignored) { }
            equalizer = null;
        }
    }

    private void applyPitch(int tonalidade) {
        if (player == null || !playerReady) return;
        try {
            // 50% mantém o tom original; pitch muda sem alterar a velocidade.
            float pitch = tonalidade < 50
                    ? 0.5f + (tonalidade / 50f) * 0.5f
                    : 1.0f + ((tonalidade - 50) / 50f);
            PlaybackParams params = new PlaybackParams();
            params.setSpeed(1.0f);
            params.setPitch(pitch);
            player.setPlaybackParams(params);
        } catch (Exception e) {
            Toast.makeText(this, "Este aparelho não permite alterar o tom mantendo a velocidade.", Toast.LENGTH_LONG).show();
        }
    }

    private void showAudioSettings() {
        SharedPreferences prefs = getSharedPreferences("programacao", MODE_PRIVATE);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), dp(12), dp(12), dp(8));

        final int[] value = {prefs.getInt("tonalidade", 50)};
        AudioDial pitchDial = new AudioDial(this, "TONALIDADE", value[0], next -> {
            value[0] = next;
            applyPitch(next);
        });
        content.addView(pitchDial, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(170)));

        TextView hint = label("Grosso  ←  50% original  →  Fino. A velocidade permanece normal.", 12, MUTED, false);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, dp(8), 0, 0);
        content.addView(hint);

        new AlertDialog.Builder(this).setTitle("Tonalidade do canto")
                .setView(content)
                .setNegativeButton("Cancelar", (dialog, which) ->
                        applyPitch(prefs.getInt("tonalidade", 50)))
                .setNeutralButton("Padrão", (dialog, which) -> {
                    value[0] = 50;
                    prefs.edit().putInt("tonalidade", 50).apply();
                    applyPitch(50);
                })
                .setPositiveButton("Salvar", (dialog, which) -> {
                    prefs.edit().putInt("tonalidade", value[0]).apply();
                    applyPitch(value[0]);
                    Toast.makeText(this, "Tonalidade salva", Toast.LENGTH_SHORT).show();
                }).show();
    }

    private interface DialChangeListener {
        void onChanged(int value);
    }

    private class AudioDial extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF arc = new RectF();
        private final String title;
        private final DialChangeListener listener;
        private int value;
        private float downAngle;
        private int startValue;

        AudioDial(Activity context, String title, int initialValue, DialChangeListener listener) {
            super(context);
            this.title = title;
            this.value = Math.max(0, Math.min(100, initialValue));
            this.listener = listener;
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = dp(88);
            float radius = dp(43);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(7));
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(0xFF333333);
            arc.set(cx - radius, cy - radius, cx + radius, cy + radius);
            canvas.drawArc(arc, 135, 270, false, paint);
            paint.setShader(new LinearGradient(cx, cy - radius, cx, cy + radius,
                    new int[] { Color.rgb(255, 248, 170), Color.rgb(255, 215, 0), Color.rgb(218, 145, 0) },
                    null, Shader.TileMode.CLAMP));
            canvas.drawArc(arc, 135, 270f * value / 100f, false, paint);
            paint.setShader(null);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF202020);
            canvas.drawCircle(cx, cy, dp(34), paint);
            double angle = Math.toRadians(135 + 270f * value / 100f);
            float nx = cx + (float)Math.cos(angle) * dp(27);
            float ny = cy + (float)Math.sin(angle) * dp(27);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(nx, ny, dp(4), paint);

            paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(dp(18));
            canvas.drawText(value + "%", cx, cy + dp(6), paint);
            paint.setTextSize(dp(12));
            paint.setColor(Color.WHITE);
            canvas.drawText(title, cx, dp(153), paint);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            float cx = getWidth() / 2f;
            float cy = dp(88);
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                downAngle = (float)Math.toDegrees(Math.atan2(event.getY() - cy, event.getX() - cx));
                startValue = value;
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_UP) {
                float angle = (float)Math.toDegrees(Math.atan2(event.getY() - cy, event.getX() - cx));
                float delta = angle - downAngle;
                while (delta < -180) delta += 360;
                while (delta > 180) delta -= 360;
                int next = Math.max(0, Math.min(100, startValue + Math.round(delta * 100f / 270f)));
                if (next != value) {
                    value = next;
                    listener.onChanged(value);
                    invalidate();
                }
                return true;
            }
            return true;
        }
    }

    private void startBackgroundPlayback() {
        Intent intent = new Intent(this, PlaybackService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
        else startService(intent);
    }

    private void stopBackgroundPlayback() {
        stopService(new Intent(this, PlaybackService.class));
    }

    private void releasePlayer() {
        playerReady = false;
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) { }
            player.release();
            player = null;
        }
    }

    @Override
    protected void onDestroy() {
        stopCycle();
        stopBackgroundPlayback();
        abandonPlaybackAudioFocus();
        releaseEqualizer();
        releasePlayer();
        super.onDestroy();
    }
}