package com.reprodutor.inteligente;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding(28, 36, 28, 28);
        layout.setBackgroundColor(0xFFF4F6F8);

        TextView title = new TextView(this);
        title.setText("Reprodutor Inteligente");
        title.setTextSize(27);
        title.setTextColor(0xFF17365D);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Seus áudios e comandos por voz");
        subtitle.setTextSize(15);
        subtitle.setTextColor(0xFF526477);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 8, 0, 24);
        layout.addView(subtitle);

        selectedFile = new TextView(this);
        selectedFile.setText("Nenhum áudio selecionado");
        selectedFile.setTextSize(16);
        selectedFile.setTextColor(0xFF172B4D);
        selectedFile.setGravity(Gravity.CENTER);
        selectedFile.setPadding(8, 18, 8, 8);
        layout.addView(selectedFile);

        status = new TextView(this);
        status.setText("Pronto para começar");
        status.setTextSize(14);
        status.setTextColor(0xFF526477);
        status.setGravity(Gravity.CENTER);
        status.setPadding(8, 8, 8, 22);
        layout.addView(status);

        addButton(layout, "ESCOLHER ÁUDIO", v -> chooseAudio());
        addButton(layout, "REPRODUZIR", v -> playAudio());
        addButton(layout, "PAUSAR", v -> pauseAudio());
        addButton(layout, "PARAR", v -> stopAudio());
        addButton(layout, "COMANDO POR VOZ", v -> startSpeech());

        TextView help = new TextView(this);
        help.setText("Comandos: “reproduzir”, “pausar”, “parar” ou “escolher áudio”.\n\nA voz usa o serviço de reconhecimento disponível no Android e pode precisar de internet.");
        help.setTextSize(13);
        help.setTextColor(0xFF526477);
        help.setGravity(Gravity.CENTER);
        help.setPadding(8, 24, 8, 0);
        layout.addView(help);

        setContentView(layout);
    }

    private void addButton(LinearLayout layout, String label, View.OnClickListener action) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(action);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 5, 0, 5);
        layout.addView(button, params);
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
                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) { }
                releasePlayer();
                player = new MediaPlayer();
                player.setDataSource(this, uri);
                player.setOnPreparedListener(mp -> {
                    status.setText("Áudio carregado. Toque em REPRODUZIR ou fale o comando.");
                    selectedFile.setText("Áudio selecionado: " + uri.getLastPathSegment());
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
        try {
            player.start();
            status.setText("Reproduzindo áudio");
        } catch (IllegalStateException e) {
            status.setText("Selecione o áudio novamente");
        }
    }

    private void pauseAudio() {
        if (player != null && player.isPlaying()) {
            player.pause();
            status.setText("Áudio pausado");
        } else {
            status.setText("Nenhum áudio está tocando");
        }
    }

    private void stopAudio() {
        if (player != null) {
            try {
                if (player.isPlaying()) player.pause();
                player.seekTo(0);
                status.setText("Áudio parado");
            } catch (IllegalStateException e) {
                status.setText("Selecione o áudio novamente");
            }
        } else {
            status.setText("Escolha um áudio primeiro");
        }
    }

    private void handleVoiceCommand(String spoken) {
        String command = Normalizer.normalize(spoken.toLowerCase(new Locale("pt", "BR")), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        if (command.contains("escolher") || command.contains("abrir audio") || command.contains("selecionar audio")) {
            status.setText("Abrindo seus áudios");
            chooseAudio();
        } else if (command.contains("pausar") || command.equals("pause")) {
            pauseAudio();
        } else if (command.contains("parar")) {
            stopAudio();
        } else if (command.contains("reproduzir") || command.contains("tocar") || command.contains("continuar") || command.contains("play")) {
            playAudio();
        } else {
            status.setText("Você disse: " + spoken + ". Tente: reproduzir, pausar, parar ou escolher áudio.");
        }
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