package com.reprodutor.inteligente;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private MediaPlayer player;
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
        logo.setText("🐦");
        logo.setTextSize(29);
        logo.setGravity(Gravity.CENTER);
        logo.setTextColor(Color.BLACK);\n        logo.setBackground(round(Color.WHITE, 100));
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

        TextView voiceTitle = label("CONTROLE POR VOZ", 12, MUTED, true);
        voiceTitle.setPadding(0, dp(25), 0, dp(10));
        page.addView(voiceTitle);
        Button voice = makeButton("🎙   FALAR COMANDO", Color.rgb(25, 48, 35), GREEN, v -> startSpeech());
        page.addView(voice, match());
        TextView help = label("Diga: reproduzir, pausar, parar ou escolher áudio.", 12, MUTED, false);
        help.setGravity(Gravity.CENTER);
        help.setPadding(dp(8), dp(15), dp(8), 0);
        page.addView(help);

        TextView footer = label("MAIS RECURSOS INTELIGENTES EM DESENVOLVIMENTO", 9, MUTED, true);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(28), 0, 0);
        page.addView(footer);
        setContentView(scroll);
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
                releasePlayer();
                player = new MediaPlayer();
                player.setDataSource(this, uri);
                player.setOnPreparedListener(mp -> {
                    status.setText("Áudio carregado. Pronto para reproduzir.");
                    selectedFile.setText("♫  " + uri.getLastPathSegment());
                });
                player.setOnCompletionListener(mp -> status.setText("Reprodução concluída"));
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

    private void playAudio() {
        if (player == null) {
            Toast.makeText(this, "Escolha um áudio primeiro", Toast.LENGTH_SHORT).show();
            return;
        }
        try { player.start(); status.setText("Reproduzindo áudio"); }
        catch (IllegalStateException e) { status.setText("Selecione o áudio novamente"); }
    }

    private void pauseAudio() {
        if (player != null && player.isPlaying()) {
            player.pause();
            status.setText("Áudio pausado");
        } else status.setText("Nenhum áudio está tocando");
    }

    private void stopAudio() {
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
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) { }
            player.release();
            player = null;
        }
    }

    @Override
    protected void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }
}