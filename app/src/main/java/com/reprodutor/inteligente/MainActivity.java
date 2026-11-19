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
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private MediaPlayer player;
    private TextView status;
    private static final int PICK_AUDIO = 10;
    private static final int SPEECH = 11;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(32, 32, 32, 32);
        layout.setBackgroundColor(0xFFF4F6F8);

        TextView title = new TextView(this);
        title.setText("Reprodutor Inteligente");
        title.setTextSize(26);
        title.setTextColor(0xFF172B4D);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        status = new TextView(this);
        status.setText("Escolha um áudio para começar");
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 28, 0, 28);
        layout.addView(status);

        addButton(layout, "Escolher áudio", v -> chooseAudio());
        addButton(layout, "Reproduzir / Pausar", v -> togglePlayback());
        addButton(layout, "Comando por voz", v -> startSpeech());

        TextView note = new TextView(this);
        note.setText("Versão inicial: reprodução de arquivos de áudio do celular e reconhecimento de fala fornecido pelo Android.");
        note.setTextSize(13);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0, 28, 0, 0);
        layout.addView(note);

        setContentView(layout);
    }

    private void addButton(LinearLayout layout, String label, View.OnClickListener action) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(action);
        layout.addView(button);
    }

    private void chooseAudio() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        startActivityForResult(intent, PICK_AUDIO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_AUDIO && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                if (player != null) player.release();
                player = new MediaPlayer();
                player.setDataSource(this, uri);
                player.setOnPreparedListener(mp -> {
                    status.setText("Reproduzindo áudio");
                    mp.start();
                });
                player.setOnErrorListener((mp, what, extra) -> {
                    status.setText("Não foi possível reproduzir este áudio");
                    return true;
                });
                status.setText("Carregando áudio...");
                player.prepareAsync();
            } catch (Exception e) {
                status.setText("Erro ao abrir áudio");
                Toast.makeText(this, "Não foi possível abrir o arquivo", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == SPEECH && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) status.setText("Você disse: " + results.get(0));
        }
    }

    private void togglePlayback() {
        if (player == null) {
            Toast.makeText(this, "Escolha um áudio primeiro", Toast.LENGTH_SHORT).show();
        } else if (player.isPlaying()) {
            player.pause();
            status.setText("Áudio pausado");
        } else {
            player.start();
            status.setText("Reproduzindo áudio");
        }
    }

    private void startSpeech() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, new Locale("pt", "BR"));
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale um comando");
            startActivityForResult(intent, SPEECH);
        } catch (Exception e) {
            Toast.makeText(this, "Reconhecimento de voz não disponível neste aparelho", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }
}
