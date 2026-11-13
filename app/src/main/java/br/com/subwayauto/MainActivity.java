package br.com.subwayauto;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView status;
    private final Handler statusHandler = new Handler(Looper.getMainLooper());
    private final Runnable statusTicker = new Runnable() {
        @Override public void run() {
            refreshStatus();
            statusHandler.postDelayed(this, 1000);
        }
    };

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
        subtitle.setText("Controle flutuante para usar sobre jogos e navegadores.");
        subtitle.setTextSize(16);
        subtitle.setTextColor(0xFFCFD8DC);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle);

        status = new TextView(this);
        status.setTextSize(15);
        status.setTextColor(0xFFFFCC80);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(24), 0, dp(20));
        root.addView(status);

        Button access = new Button(this);
        access.setText("CONFIGURAR ACESSIBILIDADE");
        access.setOnClickListener(v ->
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        );
        root.addView(access, new LinearLayout.LayoutParams(-1, -2));

        Button overlay = new Button(this);
        overlay.setText("ATIVAR MODO FLUTUANTE");
        overlay.setOnClickListener(v -> enableFloatingMode());
        root.addView(overlay, new LinearLayout.LayoutParams(-1, -2));

        Button stop = new Button(this);
        stop.setText("PARAR SERVIÇO FLUTUANTE");
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, FloatingControlService.class));
            refreshStatus();
        });
        root.addView(stop, new LinearLayout.LayoutParams(-1, -2));

        TextView note = new TextView(this);
        note.setText("O modo flutuante mantém um botão sobre outros aplicativos e uma notificação enquanto estiver ativo. A captura de tela do Subway Surfers é analisada para comparar visualmente as três pistas. Os movimentos automáticos ainda estão desativados enquanto validamos o reconhecimento.");
        note.setTextColor(0xFFB0BEC5);
        note.setTextSize(13);
        note.setPadding(0, dp(24), 0, 0);
        root.addView(note);

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        statusHandler.removeCallbacks(statusTicker);
        statusHandler.post(statusTicker);
    }

    @Override
    protected void onPause() {
        statusHandler.removeCallbacks(statusTicker);
        super.onPause();
    }

    private void enableFloatingMode() {
        if (!Settings.canDrawOverlays(this)) {
            Intent permission = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())
            );
            startActivity(permission);
            return;
        }
        Intent service = new Intent(this, FloatingControlService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(service);
        } else {
            startService(service);
        }
        refreshStatus();
    }

    private void refreshStatus() {
        boolean overlay = Settings.canDrawOverlays(this);
        boolean accessibility = AutoPlayAccessibilityService.isEnabled(this);
        String message = overlay ? "PERMISSÃO DE JANELA FLUTUANTE: ATIVA" : "PERMISSÃO DE JANELA FLUTUANTE: DESATIVADA";
        message += accessibility ? "\nACESSIBILIDADE: ATIVADA" : "\nACESSIBILIDADE: DESATIVADA";
        message += "\n" + AutoPlayAccessibilityService.getFrameStatus();
        message += "\n" + AutoPlayAccessibilityService.getVisionStatus();
        message += "\nQuadros analisados: " + AutoPlayAccessibilityService.getFramesCaptured();
        status.setText(message);
        status.setTextColor(overlay && accessibility ? 0xFF80CBC4 : 0xFFFFCC80);
    }
}
