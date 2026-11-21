package com.reprodutor.painel;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private TextView status;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 36, 28, 28);
        root.setBackgroundColor(0xff101010);
        TextView title = new TextView(this);
        title.setText("PAINEL DE ATIVAÇÃO");
        title.setTextSize(22);
        title.setTextColor(0xffffd700);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        TextView info = new TextView(this);
        info.setText("Servidor local para solicitações, comprovantes e aprovação manual.");
        info.setTextColor(-1);
        info.setPadding(0,24,0,20);
        root.addView(info);
        Button password = new Button(this);
        password.setText("CRIAR / ALTERAR SENHA ADMINISTRATIVA");
        root.addView(password);
        password.setOnClickListener(v -> passwordDialog());
        Button start = new Button(this);
        start.setText("INICIAR SERVIDOR");
        root.addView(start);
        start.setOnClickListener(v -> {
            Intent i = new Intent(this, ServerService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
            status.setText("Solicitação de inicialização enviada. Confira a notificação do Android.");
        });
        status = new TextView(this);
        status.setTextColor(0xffdddddd);
        status.setPadding(0,24,0,0);
        status.setText("Servidor desligado");
        root.addView(status);
        TextView warning = new TextView(this);
        warning.setTextColor(0xffffcc66);
        warning.setPadding(0,28,0,0);
        warning.setText("Mantenha este celular ligado e conectado. Para acesso externo será necessário um túnel HTTPS seguro. A pré-análise não confirma o crédito do Pix.");
        root.addView(warning);
        setContentView(root);
    }
    private void passwordDialog() {
        EditText a = new EditText(this); a.setHint("Nova senha (mínimo 10 caracteres)");
        EditText b = new EditText(this); b.setHint("Repita a senha");
        LinearLayout box = new LinearLayout(this); box.setOrientation(1); box.setPadding(32,8,32,8); box.addView(a); box.addView(b);
        new AlertDialog.Builder(this).setTitle("Configurar administrador").setView(box)
            .setNegativeButton("Cancelar", null).setPositiveButton("Salvar", (d,w) -> {
                String x=a.getText().toString(), y=b.getText().toString();
                if (x.length()<10 || !x.equals(y)) { Toast.makeText(this,"Senhas diferentes ou muito curtas.",Toast.LENGTH_LONG).show(); return; }
                AdminAuth.save(this,x);
                Toast.makeText(this,"Senha administrativa configurada neste aparelho.",Toast.LENGTH_LONG).show();
            }).show();
    }
}