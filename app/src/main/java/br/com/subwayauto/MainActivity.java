package br.com.subwayauto;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String GAME_PACKAGE = "com.kiloo.subwaysurf";
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
        subtitle.setText("Assistente experimental para Subway Surfers.");
        subtitle.setTextSize(16);
        subtitle.setTextColor(0xFFCFD8DC);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle);

        status = new TextView(this);
        status.setTextSize(15);
        status.setTextColor(0xFFFFCC80);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(28), 0, dp(24));
        root.addView(status);

        Button access = new Button(this);
        access.setText("CONFIGURAR ACESSIBILIDADE");
        access.setOnClickListener(v ->
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        );
        root.addView(access, new LinearLayout.LayoutParams(-1, -2));

        Button refresh = new Button(this);
        refresh.setText("VERIFICAR CONEXÃO");
        refresh.setOnClickListener(v -> refreshStatus());
        root.addView(refresh, new LinearLayout.LayoutParams(-1, -2));

        Button launchGame = new Button(this);
        launchGame.setText("ABRIR SUBWAY SURFERS");
        launchGame.setOnClickListener(v -> openGame());
        root.addView(launchGame, new LinearLayout.LayoutParams(-1, -2));

        TextView note = new TextView(this);
        note.setText("ATENÇÃO: esta versão ainda não joga sozinha. Ela verifica a conexão do serviço e permite abrir o jogo. O reconhecimento de obstáculos, a captura autorizada da tela e os comandos automáticos serão implementados nas próximas etapas.");
        note.setTextColor(0xFFB0BEC5);
        note.setTextSize(13);
        note.setPadding(0, dp(24), 0, 0);
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
            status.setText("SERVIÇO CONECTADO ✓\nAcessibilidade ativa. Você já pode abrir o jogo para o teste inicial.");
            status.setTextColor(0xFF80CBC4);
        } else if (enabled) {
            status.setText("PERMISSÃO ATIVADA, MAS SERVIÇO NÃO CONECTADO. Volte às configurações, desligue e ligue SUBWAY AUTO; depois retorne aqui.");
            status.setTextColor(0xFFFFCC80);
        } else {
            status.setText("ACESSIBILIDADE DESATIVADA. Toque em CONFIGURAR ACESSIBILIDADE e habilite SUBWAY AUTO.");
            status.setTextColor(0xFFFFCC80);
        }
    }

    private void openGame() {
        PackageManager pm = getPackageManager();
        Intent launch = pm.getLaunchIntentForPackage(GAME_PACKAGE);
        if (launch == null) {
            Toast.makeText(this, "Subway Surfers não foi encontrado. Verifique se está instalado.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!AutoPlayAccessibilityService.isConnected()) {
            Toast.makeText(this, "O serviço de acessibilidade ainda não está conectado. Confira o status antes de continuar.", Toast.LENGTH_LONG).show();
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(launch);
    }
}
