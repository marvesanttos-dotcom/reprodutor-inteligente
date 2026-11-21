package com.reprodutor.inteligente;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Versão somente visual: preserva a aparência do aplicativo, sem ações,
 * reprodução de áudio, seleção de arquivos ou programação funcional.
 */
public class MainActivity extends Activity {
    private final int BG = Color.BLACK;
    private final int PANEL = Color.rgb(16, 16, 16);
    private final int GOLD = Color.rgb(255, 215, 0);
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
        TextView logo = label("♫", 29, Color.rgb(255, 225, 80), true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(goldGradient(100));
        brand.addView(logo, new LinearLayout.LayoutParams(dp(58), dp(58)));
        LinearLayout brandText = new LinearLayout(this);
        brandText.setOrientation(LinearLayout.VERTICAL);
        brandText.setPadding(dp(14), 0, 0, 0);
        brandText.addView(label("PÁSSARO", 19, GOLD, true));
        brandText.addView(label("PROFESSOR  •  CANTOS", 11, MUTED, true));
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

        TextView disc = label("♫", 54, GOLD, true);
        disc.setGravity(Gravity.CENTER);
        disc.setBackground(goldGradient(100));
        LinearLayout.LayoutParams discParams = new LinearLayout.LayoutParams(dp(112), dp(112));
        discParams.bottomMargin = dp(18);
        playerCard.addView(disc, discParams);

        TextView selected = label("Nenhum áudio selecionado", 17, WHITE, true);
        selected.setGravity(Gravity.CENTER);
        playerCard.addView(selected);
        TextView status = label("Pronto para começar", 13, MUTED, false);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(8), 0, dp(20));
        playerCard.addView(status);
        Button choose = button("＋   ESCOLHER ÁUDIO", GOLD, BG);
        playerCard.addView(choose, match());
        page.addView(playerCard);

        Button audioSettings = button("♫  TONALIDADE • CANTO MAIS GROSSO OU MAIS FINO", PANEL, WHITE);
        LinearLayout.LayoutParams audioParams = match();
        audioParams.topMargin = dp(12);
        page.addView(audioSettings, audioParams);

        TextView scheduleTitle = label("PROGRAMAÇÕES AUTOMÁTICAS", 12, MUTED, true);
        scheduleTitle.setPadding(0, dp(25), 0, dp(10));
        page.addView(scheduleTitle);

        LinearLayout scheduleHeader = new LinearLayout(this);
        scheduleHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView scheduleInfo = label("Configure até 4 horários de funcionamento.", 13, WHITE, false);
        scheduleHeader.addView(scheduleInfo, new LinearLayout.LayoutParams(0, -2, 1));
        TextView reset = label("↻", 29, WHITE, false);
        reset.setGravity(Gravity.CENTER);
        scheduleHeader.addView(reset, new LinearLayout.LayoutParams(dp(48), dp(48)));
        page.addView(scheduleHeader);

        for (int i = 1; i <= 4; i++) {
            Button start = button("PROGRAMAÇÃO " + i + " • LIGA: Não definido", WHITE, BG);
            LinearLayout.LayoutParams p1 = match();
            p1.topMargin = dp(7);
            page.addView(start, p1);

            Button end = button("PROGRAMAÇÃO " + i + " • DESLIGA: Não definido", PANEL, WHITE);
            LinearLayout.LayoutParams p2 = match();
            p2.topMargin = dp(7);
            page.addView(end, p2);
        }

        Button playingTime = button("▶  TEMPO TOCANDO: 5 MIN", PANEL, WHITE);
        LinearLayout.LayoutParams playingParams = match();
        playingParams.topMargin = dp(7);
        page.addView(playingTime, playingParams);

        Button stoppedTime = button("Ⅱ  TEMPO PARADO: 5 MIN", PANEL, WHITE);
        LinearLayout.LayoutParams stoppedParams = match();
        stoppedParams.topMargin = dp(7);
        page.addView(stoppedTime, stoppedParams);

        LinearLayout autoHeader = new LinearLayout(this);
        autoHeader.setGravity(Gravity.CENTER_VERTICAL);
        autoHeader.setPadding(0, dp(22), 0, dp(4));
        TextView autoTitle = label("AUTOMÁTICO", 12, MUTED, true);
        autoHeader.addView(autoTitle, new LinearLayout.LayoutParams(0, dp(42), 1));
        Button autoToggle = button("●  DESLIGADO", PANEL, MUTED);
        autoHeader.addView(autoToggle, new LinearLayout.LayoutParams(dp(132), dp(38)));
        page.addView(autoHeader);

        Button play = button("▶  PLAY", PANEL, MUTED);
        LinearLayout.LayoutParams playParams = match();
        playParams.topMargin = dp(6);
        page.addView(play, playParams);

        TextView footer = label("MAIS RECURSOS INTELIGENTES EM DESENVOLVIMENTO", 9, MUTED, true);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(28), 0, 0);
        page.addView(footer);

        setContentView(scroll);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable goldGradient(int radius) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[] { Color.rgb(255, 248, 170), GOLD, Color.rgb(218, 145, 0), Color.rgb(255, 236, 105) });
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

    private TextView label(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private LinearLayout.LayoutParams match() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
    }

    private Button button(String value, int background, int foreground) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextSize(13);
        b.setTextColor(foreground);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(background == GOLD ? goldGradient(16) : round(background, 16));
        b.setElevation(dp(2));
        b.setClickable(false);
        b.setFocusable(false);
        return b;
    }
}
