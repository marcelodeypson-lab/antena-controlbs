package br.com.subwayauto;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView status;

    private int dp(float value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(32), dp(24), dp(24));
        root.setBackgroundColor(0xFF101820);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("SUBWAY AUTO");
        title.setTextSize(30);
        title.setTextColor(0xFFFFFFFF);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(20), 0, dp(12));
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Assistente experimental para controle do jogo.");
        subtitle.setTextSize(16);
        subtitle.setTextColor(0xFFCFD8DC);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle);

        status = new TextView(this);
        status.setTextSize(15);
        status.setTextColor(0xFF80CBC4);
        status.setPadding(0, dp(28), 0, dp(24));
        root.addView(status);

        Button access = new Button(this);
        access.setText("ATIVAR PERMISSÃO DE ACESSIBILIDADE");
        access.setOnClickListener(v ->
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        );
        root.addView(access, new LinearLayout.LayoutParams(-1, -2));

        Button refresh = new Button(this);
        refresh.setText("ATUALIZAR STATUS");
        refresh.setOnClickListener(v -> refreshStatus());
        root.addView(refresh, new LinearLayout.LayoutParams(-1, -2));

        TextView note = new TextView(this);
        note.setText("Importante: esta versão ainda não reconhece obstáculos nem controla o jogo automaticamente. A captura da tela e a lógica de jogo serão adicionadas nas próximas etapas.");
        note.setTextColor(0xFFB0BEC5);
        note.setTextSize(13);
        note.setPadding(0, dp(28), 0, 0);
        root.addView(note);

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void refreshStatus() {
        boolean enabled = AutoPlayAccessibilityService.isEnabled(this);
        boolean connected = AutoPlayAccessibilityService.isConnected();

        if (connected) {
            status.setText("PERMISSÃO ATIVA E SERVIÇO CONECTADO. A base de gestos está pronta para os próximos testes.");
            status.setTextColor(0xFF80CBC4);
        } else if (enabled) {
            status.setText("PERMISSÃO CONCEDIDA, MAS O SERVIÇO AINDA NÃO CONECTOU. Desative e ative novamente o serviço nas configurações do Android.");
            status.setTextColor(0xFFFFCC80);
        } else {
            status.setText("PERMISSÃO NÃO ATIVADA. Toque no botão abaixo e habilite SUBWAY AUTO na lista de acessibilidade.");
            status.setTextColor(0xFFFFCC80);
        }
    }
}
