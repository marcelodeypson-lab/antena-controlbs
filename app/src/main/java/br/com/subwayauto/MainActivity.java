package br.com.subwayauto;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView status;
    private int dp(float value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(32), dp(24), dp(24));
        root.setBackgroundColor(0xFF101820);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView title = new TextView(this); title.setText("SUBWAY AUTO"); title.setTextSize(30); title.setTextColor(0xFFFFFFFF); title.setGravity(Gravity.CENTER); title.setPadding(0, dp(20), 0, dp(12)); root.addView(title);
        TextView subtitle = new TextView(this); subtitle.setText("Assistente experimental para leitura de obstáculos e controle automático."); subtitle.setTextSize(16); subtitle.setTextColor(0xFFCFD8DC); subtitle.setGravity(Gravity.CENTER); root.addView(subtitle);
        status = new TextView(this); status.setText("Etapa 1: aplicativo instalado. O reconhecimento visual ainda será implementado."); status.setTextSize(15); status.setTextColor(0xFF80CBC4); status.setPadding(0, dp(28), 0, dp(24)); root.addView(status);
        Button access = new Button(this); access.setText("CONFIGURAR ACESSIBILIDADE"); access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))); root.addView(access, new LinearLayout.LayoutParams(-1, -2));
        Button test = new Button(this); test.setText("VERIFICAR SERVIÇO"); test.setOnClickListener(v -> { boolean enabled = AutoPlayAccessibilityService.isEnabled(this); status.setText(enabled ? "Serviço de gestos ativado. Próxima etapa: captura e leitura da tela." : "Serviço ainda não ativado. Abra Acessibilidade e habilite SUBWAY AUTO."); }); root.addView(test, new LinearLayout.LayoutParams(-1, -2));
        TextView note = new TextView(this); note.setText("Nenhum movimento será executado sem comando do usuário. A captura da tela exigirá autorização explícita do Android."); note.setTextColor(0xFFB0BEC5); note.setTextSize(13); note.setPadding(0, dp(28), 0, 0); root.addView(note);
        setContentView(root);
    }
}
