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
import java.util.Locale;

public class MainActivity extends Activity {
    private boolean cycleActive = false;
    private Button cycleButton;
    private TextView status;
    private TextView selectedFile;
    private static final int PICK_AUDIO = 10;
    private final int BG = Color.BLACK;
    private final int PANEL = Color.rgb(16, 16, 16);
    private final int GREEN = Color.rgb(255, 215, 0);
    private final int WHITE = Color.WHITE;
    private final int MUTED = Color.rgb(190, 190, 190);
    private final Button[] configButtons = new Button[8];

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
        page.addView(playerCard);

        Button audioSettings = makeButton("♫  TONALIDADE • CANTO MAIS GROSSO OU MAIS FINO", PANEL, WHITE, v -> showAudioSettings());
        LinearLayout.LayoutParams audioSettingsParams = match();
        audioSettingsParams.topMargin = dp(12);
        page.addView(audioSettings, audioSettingsParams);

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
        cycleActive = prefs.getBoolean("auto_active", false);
        if (cycleActive) cycleButton.setText("■  PARAR CICLO AUTOMÁTICO");
        setContentView(scroll);
        restoreSavedAudio();
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
            if (uri != null) {
                if (cycleActive) {
                    cycleActive = false;
                    if (cycleButton != null) cycleButton.setText("▶  INICIAR CICLO AUTOMÁTICO");
                    sendPlaybackCommand(PlaybackService.ACTION_AUTO_STOP, false);
                }
                try {
                    getContentResolver().takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) { }
                getSharedPreferences("programacao", MODE_PRIVATE).edit()
                        .putString("audio_uri", uri.toString())
                        .apply();
                loadAudio(uri);
            }
        }
    }

    // Restaura o arquivo escolhido anteriormente usando a permissão persistente
    // concedida pelo seletor de documentos do Android.
    private void restoreSavedAudio() {
        String saved = getSharedPreferences("programacao", MODE_PRIVATE)
                .getString("audio_uri", null);
        if (saved == null || saved.isEmpty()) return;
        try {
            loadAudio(Uri.parse(saved), false);
        } catch (Exception e) {
            status.setText("O áudio salvo não está mais disponível. Escolha novamente.");
        }
    }

    private void loadAudio(Uri uri) {
        loadAudio(uri, false);
    }

    private void loadAudio(Uri uri, boolean restoring) {
        // A seleção apenas prepara e guarda o áudio. Nunca envia comando PLAY.
        try {
            Intent command = new Intent(this, PlaybackService.class);
            command.setAction(PlaybackService.ACTION_LOAD);
            command.setData(uri);
            command.putExtra(PlaybackService.EXTRA_RESTORE, restoring);
            startService(command);
            status.setText("Áudio selecionado • pronto para a programação automática.");
            selectedFile.setText("♫  " + uri.getLastPathSegment());
        } catch (Exception e) {
            status.setText("Não foi possível carregar o áudio selecionado.");
            Toast.makeText(this, "Erro ao carregar áudio", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendPlaybackCommand(String action, boolean foreground) {
        Intent command = new Intent(this, PlaybackService.class);
        command.setAction(action);
        try {
            if (foreground && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(command);
            } else {
                startService(command);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível executar o comando de áudio", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasSavedAudio() {
        String uri = getSharedPreferences("programacao", MODE_PRIVATE).getString("audio_uri", null);
        return uri != null && !uri.isEmpty();
    }

    private void toggleCycle() {
        if (cycleActive) {
            stopCycle();
            status.setText("Ciclo automático parado");
            return;
        }
        if (!hasSavedAudio()) {
            Toast.makeText(this, "Escolha um áudio primeiro", Toast.LENGTH_SHORT).show();
            return;
        }
        cycleActive = true;
        if (cycleButton != null) cycleButton.setText("■  PARAR CICLO AUTOMÁTICO");
        status.setText("Automático ativo • aguardando programação");
        sendPlaybackCommand(PlaybackService.ACTION_AUTO_START, true);
    }

    private void stopCycle() {
        cycleActive = false;
        if (cycleButton != null) cycleButton.setText("▶  INICIAR CICLO AUTOMÁTICO");
        sendPlaybackCommand(PlaybackService.ACTION_AUTO_STOP, false);
    }

    private void applyPitch(int tonalidade) {
        Intent command = new Intent(this, PlaybackService.class);
        command.setAction(PlaybackService.ACTION_SET_PITCH);
        command.putExtra(PlaybackService.EXTRA_PITCH, tonalidade);
        try { startService(command); } catch (Exception ignored) { }
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

    @Override
    protected void onDestroy() {
        // O serviço é dono do MediaPlayer e continua ativo fora da tela.
        super.onDestroy();
    }
}
