package com.reprodutor.inteligente;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private MediaPlayer player;
    private final Handler cycleHandler = new Handler(Looper.getMainLooper());
    private boolean playerReady = false;
    private boolean cycleActive = false;
    private boolean cyclePlaying = false;
    private Button cycleButton;
    private final Runnable cycleStep = new Runnable() {
        @Override public void run() {
            if (!cycleActive || player == null || !playerReady) return;
            SharedPreferences prefs = getSharedPreferences("programacao", MODE_PRIVATE);
            if (cyclePlaying) {
                try { if (player.isPlaying()) player.pause(); } catch (Exception ignored) { }
                cyclePlaying = false;
                status.setText("Ciclo automático: pausa");
                cycleHandler.postDelayed(this, prefs.getInt("parado", 5) * 60L * 1000L);
            } else {
                try {
                    if (player.getCurrentPosition() >= player.getDuration()) player.seekTo(0);
                    player.start();
                    cyclePlaying = true;
                    status.setText("Ciclo automático: reproduzindo");
                    cycleHandler.postDelayed(this, prefs.getInt("tocando", 5) * 60L * 1000L);
                } catch (Exception e) {
                    stopCycle();
                    status.setText("Não foi possível iniciar o ciclo");
                }
            }
        }
    };
    private final Button[] configButtons = new Button[4];
    private TextView status;
    private TextView selectedFile;
    private static final int PICK_AUDIO = 10;
    private static final int SPEECH = 11;
    private final int BG = Color.BLACK;
    private final int PANEL = Color.rgb(16, 16, 16);
    private final int GREEN = Color.WHITE;
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
        logo.setText("🎶");
        logo.setTextSize(29);
        logo.setGravity(Gravity.CENTER);
        logo.setTextColor(Color.BLACK);
        logo.setBackground(round(Color.WHITE, 100));
        brand.addView(logo, new LinearLayout.LayoutParams(dp(58), dp(58)));
        LinearLayout brandText = new LinearLayout(this);
        brandText.setOrientation(LinearLayout.VERTICAL);
        brandText.setPadding(dp(14), 0, 0, 0);
        TextView title = label("REPRODUTOR", 19, GREEN, true);
        TextView sub = label("INTELIGENTE  •  CANTOS", 11, MUTED, true);
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
        disc.setBackground(round(Color.rgb(35, 35, 35), 100));
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

        TextView manualTitle = label("CONTROLE MANUAL", 12, MUTED, true);
        manualTitle.setPadding(0, dp(22), 0, dp(10));
        page.addView(manualTitle);
        TextView scheduleTitle = label("PROGRAMAÇÕES AUTOMÁTICAS", 12, MUTED, true);
        scheduleTitle.setPadding(0, dp(25), 0, dp(10));
        page.addView(scheduleTitle);
        TextView scheduleInfo = label("Configure os horários e os ciclos de áudio.", 13, WHITE, false);
        scheduleInfo.setPadding(dp(16), dp(16), dp(16), dp(16));
        scheduleInfo.setBackground(round(PANEL, 16));
        page.addView(scheduleInfo);

        SharedPreferences prefs = getSharedPreferences("programacao", MODE_PRIVATE);
        configButtons[0] = makeButton("◷  HORÁRIO PARA LIGAR: " + displayAmPm(prefs.getString("inicio", "Não definido")), WHITE, BG,
                v -> chooseTime("inicio", "Horário para ligar", configButtons[0]));
        configButtons[1] = makeButton("◷  HORÁRIO PARA DESLIGAR: " + displayAmPm(prefs.getString("fim", "Não definido")), WHITE, BG,
                v -> chooseTime("fim", "Horário para desligar", configButtons[1]));
        configButtons[2] = makeButton("▶  TEMPO TOCANDO: " + prefs.getInt("tocando", 5) + " MIN", PANEL, WHITE,
                v -> chooseMinutes("tocando", "Tempo tocando (minutos)", configButtons[2]));
        configButtons[3] = makeButton("Ⅱ  TEMPO PARADO: " + prefs.getInt("parado", 5) + " MIN", PANEL, WHITE,
                v -> chooseMinutes("parado", "Tempo parado (minutos)", configButtons[3]));
        for (Button b : configButtons) {
            LinearLayout.LayoutParams configParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(50));
            configParams.topMargin = dp(8);
            page.addView(b, configParams);
        }
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

    private void chooseTime(String key, String title, Button target) {
        SharedPreferences prefs = getSharedPreferences("programacao", MODE_PRIVATE);
        String current = prefs.getString(key, "08:00");
        String[] parts = current.split(":");
        int hour = 8, minute = 0;
        try { hour = Integer.parseInt(parts[0]); minute = Integer.parseInt(parts[1]); }
        catch (Exception ignored) { }
        new TimePickerDialog(this, (view, h, m) -> {
            String value = String.format(Locale.getDefault(), "%02d:%02d", h, m);
            prefs.edit().putString(key, value).apply();
            String prefix = key.equals("inicio") ? "◷  HORÁRIO PARA LIGAR: " : "◷  HORÁRIO PARA DESLIGAR: ";
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
        b.setBackground(round(background, 16));
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
                releasePlayer();
                playerReady = false;
                player = new MediaPlayer();
                player.setDataSource(this, uri);
                player.setOnPreparedListener(mp -> {
                    playerReady = true;
                    status.setText("Áudio carregado. Pronto para reproduzir.");
                    selectedFile.setText("♫  " + uri.getLastPathSegment());
                });
                player.setOnCompletionListener(mp -> {
                    if (cycleActive && cyclePlaying) {
                        try { mp.seekTo(0); mp.start(); } catch (Exception ignored) { }
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
        cycleHandler.removeCallbacks(cycleStep);
        cycleStep.run();
    }

    private void stopCycle() {
        cycleActive = false;
        cyclePlaying = false;
        cycleHandler.removeCallbacks(cycleStep);
        if (cycleButton != null) cycleButton.setText("▶  INICIAR CICLO AUTOMÁTICO");
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
        try { player.start(); status.setText("Reproduzindo áudio"); }
        catch (IllegalStateException e) { status.setText("Selecione o áudio novamente"); }
    }

    private void pauseAudio() {
        if (cycleActive) {
            stopCycle();
            status.setText("Modo manual");
        }
        if (player != null && player.isPlaying()) {
            player.pause();
            status.setText("Áudio pausado");
        } else status.setText("Nenhum áudio está tocando");
    }

    private void stopAudio() {
        stopCycle();
        if (player != null) {
            try {
                if (player.isPlaying()) player.pause();
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
        releasePlayer();
        super.onDestroy();
    }
}