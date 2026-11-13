package br.com.subwayauto;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.HardwareBuffer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;

public class AutoPlayAccessibilityService extends AccessibilityService {
    private static volatile boolean connected = false;
    private static volatile String foregroundPackage = "";
    private static volatile String frameStatus = "Aguardando jogo ou navegador";
    private static volatile long framesCaptured = 0;
    private static volatile String visionStatus = "Análise visual aguardando imagem";
    private Handler handler;
    private boolean captureInFlight = false;

    private final Runnable captureLoop = new Runnable() {
        @Override public void run() {
            boolean enabled = FloatingControlService.isAppEnabled(AutoPlayAccessibilityService.this);
            boolean paused = FloatingControlService.isAppPaused(AutoPlayAccessibilityService.this);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                frameStatus = "Captura de tela exige Android 11 ou superior";
            } else if (!connected) {
                frameStatus = "Aguardando serviço de acessibilidade";
            } else if (!enabled) {
                frameStatus = "Toque em LIGAR no controle flutuante";
            } else if (paused) {
                frameStatus = "Captura pausada";
            } else if (!isSupportedCaptureTarget(foregroundPackage)) {
                frameStatus = "Abra Subway Surfers ou um navegador | pacote: " +
                    (foregroundPackage.isEmpty() ? "nenhum evento recebido" : foregroundPackage);
            } else if (!captureInFlight) {
                if (isBrowserPackage(foregroundPackage)) {
                    frameStatus = "Modo navegador (somente leitura): " + foregroundPackage;
                }
                captureFrame();
            }
            if (handler != null) handler.postDelayed(this, 350);
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        connected = true;
        handler = new Handler(Looper.getMainLooper());
        handler.post(captureLoop);
        frameStatus = "Acessibilidade conectada";
    }

    @Override
    public void onDestroy() {
        connected = false;
        if (handler != null) handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        foregroundPackage = event.getPackageName().toString();
    }

    @Override public void onInterrupt() {
        connected = false;
        frameStatus = "Acessibilidade interrompida";
    }

    public static boolean isConnected() { return connected; }
    public static String getFrameStatus() { return frameStatus; }
    public static long getFramesCaptured() { return framesCaptured; }
    public static String getVisionStatus() { return visionStatus; }
    public static String getForegroundPackage() { return foregroundPackage; }

    private static boolean isSupportedCaptureTarget(String packageName) {
        return "com.kiloo.subwaysurf".equals(packageName) || isBrowserPackage(packageName);
    }

    private static boolean isBrowserPackage(String packageName) {
        if (packageName == null) return false;
        return "com.android.chrome".equals(packageName)
            || "org.mozilla.firefox".equals(packageName)
            || "com.brave.browser".equals(packageName)
            || "com.microsoft.emmx".equals(packageName)
            || "com.sec.android.app.sbrowser".equals(packageName)
            || "com.opera.browser".equals(packageName)
            || "com.opera.mini.native".equals(packageName);
    }

    private void captureFrame() {
        captureInFlight = true;
        try {
            takeScreenshot(Display.DEFAULT_DISPLAY, getMainExecutor(),
                new TakeScreenshotCallback() {
                    @Override public void onSuccess(ScreenshotResult result) {
                        HardwareBuffer buffer = result.getHardwareBuffer();
                        Bitmap hardwareBitmap = null;
                        Bitmap softwareBitmap = null;
                        try {
                            hardwareBitmap = Bitmap.wrapHardwareBuffer(buffer, result.getColorSpace());
                            if (hardwareBitmap != null) {
                                softwareBitmap = hardwareBitmap.copy(Bitmap.Config.ARGB_8888, false);
                                if (softwareBitmap != null) {
                                    framesCaptured++;
                                    SubwayVisionAnalyzer.Result analysis =
                                        SubwayVisionAnalyzer.analyze(softwareBitmap);
                                    visionStatus = analysis.summary;
                                    frameStatus = "Captura OK: " + softwareBitmap.getWidth()
                                        + "x" + softwareBitmap.getHeight()
                                        + " | quadros: " + framesCaptured;
                                } else {
                                    frameStatus = "Não foi possível copiar a imagem";
                                }
                            } else {
                                frameStatus = "Imagem da tela indisponível";
                            }
                        } catch (Exception error) {
                            frameStatus = "Erro ao analisar imagem: " + error.getClass().getSimpleName();
                        } finally {
                            if (softwareBitmap != null) softwareBitmap.recycle();
                            if (hardwareBitmap != null) hardwareBitmap.recycle();
                            if (buffer != null) buffer.close();
                            captureInFlight = false;
                        }
                    }

                    @Override public void onFailure(int errorCode) {
                        frameStatus = "Falha na captura de tela: " + errorCode;
                        captureInFlight = false;
                    }
                });
        } catch (Exception error) {
            frameStatus = "Captura indisponível: " + error.getClass().getSimpleName();
            captureInFlight = false;
        }
    }

    public static boolean isEnabled(Context context) {
        String setting = Settings.Secure.getString(
            context.getContentResolver(),
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        if (TextUtils.isEmpty(setting)) return false;

        ComponentName expected = new ComponentName(context, AutoPlayAccessibilityService.class);
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(setting);
        for (String item : splitter) {
            ComponentName enabled = ComponentName.unflattenFromString(item);
            if (expected.equals(enabled)) return true;
        }
        return false;
    }

    public boolean swipe(float x1, float y1, float x2, float y2, long durationMs) {
        if (!connected || durationMs <= 0) return false;
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        GestureDescription.StrokeDescription stroke =
            new GestureDescription.StrokeDescription(path, 0, durationMs);
        return dispatchGesture(
            new GestureDescription.Builder().addStroke(stroke).build(),
            null,
            null
        );
    }
}
