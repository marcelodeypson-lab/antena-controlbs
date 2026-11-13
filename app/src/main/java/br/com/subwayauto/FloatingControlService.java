package br.com.subwayauto;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.pm.ServiceInfo;

public class FloatingControlService extends Service {
    private static final String CHANNEL_ID = "subway_auto_running";
    private static final int NOTIFICATION_ID = 4101;
    private static final String PREFS = "subway_auto_controls";
    private static final String KEY_ENABLED = "app_enabled";
    private static final String KEY_PAUSED = "app_paused";

    private WindowManager windowManager;
    private TextView bubble;
    private TextView visionBadge;
    private Handler uiHandler;
    private final Runnable visionTicker = new Runnable() {
        @Override public void run() {
            updateVisionBadge();
            if (uiHandler != null) uiHandler.postDelayed(this, 600);
        }
    };
    private LinearLayout menu;
    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams menuParams;
    private WindowManager.LayoutParams badgeParams;
    private boolean menuVisible = false;
    private boolean appEnabled = false;
    private boolean appPaused = false;
    private Button pauseButton;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        appEnabled = prefs.getBoolean(KEY_ENABLED, false);
        appPaused = prefs.getBoolean(KEY_PAUSED, false);

        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(
            this, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0)
        );
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("SUBWAY AUTO em execução")
            .setContentText("O atalho flutuante continua disponível sobre outros aplicativos")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pending)
            .setOngoing(true)
            .build();

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
        uiHandler = new Handler(Looper.getMainLooper());
        showBubble();
        uiHandler.post(visionTicker);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "SUBWAY AUTO em execução", NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Mantém o atalho flutuante disponível");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private int dp(float value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int overlayType() {
        return Build.VERSION.SDK_INT >= 26
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private WindowManager.LayoutParams makeParams(int gravity, int x, int y) {
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        );
        p.gravity = gravity;
        p.x = x;
        p.y = y;
        return p;
    }

    private void showBubble() {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }
        windowManager = (WindowManager)getSystemService(Context.WINDOW_SERVICE);

        bubble = new TextView(this);
        bubble.setText("SA");
        bubble.setTextColor(Color.WHITE);
        bubble.setTextSize(12);
        bubble.setTypeface(null, android.graphics.Typeface.BOLD);
        bubble.setGravity(Gravity.CENTER);
        GradientDrawable bubbleShape = new GradientDrawable();
        bubbleShape.setShape(GradientDrawable.OVAL);
        bubbleShape.setColor(0xEE1565C0);
        bubble.setBackground(bubbleShape);
        bubble.setElevation(dp(5));
        applyOverlayOpacity();
        bubbleParams = makeParams(Gravity.TOP | Gravity.LEFT, dp(8), dp(90));
        bubbleParams.width = dp(42);
        bubbleParams.height = dp(42);

        bubble.setOnClickListener(v -> toggleMenu());
        bubble.setOnTouchListener(new View.OnTouchListener() {
            private int startX, startY;
            private float touchX, touchY;
            private boolean moved;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startX = bubbleParams.x;
                    startY = bubbleParams.y;
                    touchX = event.getRawX();
                    touchY = event.getRawY();
                    moved = false;
                    return false;
                }
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    int dx = (int)(touchX - event.getRawX());
                    int dy = (int)(event.getRawY() - touchY);
                    if (Math.abs(dx) > dp(6) || Math.abs(dy) > dp(6)) moved = true;
                    if (moved) {
                        bubbleParams.x = Math.max(0, startX + dx);
                        bubbleParams.y = Math.max(0, startY + dy);
                        try { windowManager.updateViewLayout(bubble, bubbleParams); } catch (Exception ignored) { }
                        if (badgeParams != null && visionBadge != null) {
                            badgeParams.x = bubbleParams.x + dp(46);
                            badgeParams.y = bubbleParams.y + dp(6);
                            try { windowManager.updateViewLayout(visionBadge, badgeParams); } catch (Exception ignored) { }
                        }
                        if (menuVisible) positionMenu();
                        return true;
                    }
                }
                if (event.getAction() == MotionEvent.ACTION_UP && moved) return true;
                return false;
            }
        });

        visionBadge = new TextView(this);
        visionBadge.setText("L-- C-- R--");
        visionBadge.setTextColor(Color.WHITE);
        visionBadge.setTextSize(9);
        visionBadge.setTypeface(null, android.graphics.Typeface.BOLD);
        visionBadge.setGravity(Gravity.CENTER);
        visionBadge.setPadding(dp(5), dp(3), dp(5), dp(3));
        GradientDrawable badgeShape = new GradientDrawable();
        badgeShape.setColor(0xDD263238);
        badgeShape.setCornerRadius(dp(8));
        visionBadge.setBackground(badgeShape);
        visionBadge.setElevation(dp(4));
        badgeParams = makeParams(Gravity.TOP | Gravity.LEFT, dp(54), dp(96));
        badgeParams.width = dp(92);
        badgeParams.height = dp(30);
        try {
            windowManager.addView(bubble, bubbleParams);
            windowManager.addView(visionBadge, badgeParams);
        } catch (Exception error) {
            stopSelf();
            return;
        }
        createMenu();
    }

    private void createMenu() {
        menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(6), dp(6), dp(6), dp(6));
        menu.setBackgroundColor(0xF21B2733);
        menu.setElevation(dp(10));

        TextView heading = new TextView(this);
        heading.setText("SUBWAY AUTO");
        heading.setTextColor(Color.WHITE);
        heading.setTextSize(14);
        heading.setGravity(Gravity.CENTER);
        heading.setPadding(0, 0, 0, dp(8));
        menu.addView(heading, new LinearLayout.LayoutParams(dp(148), -2));

        Button on = new Button(this);
        on.setText("LIGAR");
        on.setOnClickListener(v -> setAppEnabled(true));
        menu.addView(on, new LinearLayout.LayoutParams(dp(148), -2));

        Button off = new Button(this);
        off.setText("DESLIGAR");
        off.setOnClickListener(v -> setAppEnabled(false));
        menu.addView(off, new LinearLayout.LayoutParams(dp(148), -2));

        Button home = new Button(this);
        home.setText("MENU INICIAL");
        home.setOnClickListener(v -> {
            Intent open = new Intent(this, MainActivity.class);
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(open);
            hideMenu();
        });
        menu.addView(home, new LinearLayout.LayoutParams(dp(148), -2));

        pauseButton = new Button(this);
        pauseButton.setOnClickListener(v -> togglePaused());
        menu.addView(pauseButton, new LinearLayout.LayoutParams(dp(148), -2));

        TextView state = new TextView(this);
        state.setTag("state");
        state.setTextColor(0xFFCFD8DC);
        state.setTextSize(12);
        state.setGravity(Gravity.CENTER);
        state.setPadding(0, dp(6), 0, 0);
        menu.addView(state, new LinearLayout.LayoutParams(dp(148), -2));
        updateMenuState();

        menuParams = makeParams(Gravity.TOP | Gravity.LEFT, dp(8), dp(140));
    }

    private void updateVisionBadge() {
        if (visionBadge == null) return;
        String summary = AutoPlayAccessibilityService.getVisionStatus();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("L(\\d+) C(\\d+) R(\\d+)").matcher(summary == null ? "" : summary);
        if (matcher.find()) {
            visionBadge.setText("L" + matcher.group(1) + " C" + matcher.group(2) + " R" + matcher.group(3));
        } else {
            visionBadge.setText("VISÃO aguardando");
        }
        applyOverlayOpacity();
    }

    private void applyOverlayOpacity() {
        int percent = getSharedPreferences(PREFS, MODE_PRIVATE).getInt("overlay_opacity", 92);
        float alpha = Math.max(0.35f, Math.min(1.0f, percent / 100f));
        if (bubble != null) bubble.setAlpha(alpha);
        if (visionBadge != null) visionBadge.setAlpha(alpha);
    }

    private void positionMenu() {
        if (menu == null || !menuVisible) return;
        menuParams.x = bubbleParams.x;
        menuParams.y = bubbleParams.y + dp(48);
        try { windowManager.updateViewLayout(menu, menuParams); } catch (Exception ignored) { }
    }

    private void toggleMenu() {
        if (menuVisible) hideMenu();
        else {
            try {
                menuParams.x = bubbleParams.x;
                menuParams.y = bubbleParams.y + dp(48);
                windowManager.addView(menu, menuParams);
                menuVisible = true;
            } catch (Exception ignored) { }
        }
    }

    private void hideMenu() {
        if (menuVisible && windowManager != null && menu != null) {
            try { windowManager.removeView(menu); } catch (Exception ignored) { }
        }
        menuVisible = false;
    }

    private void togglePaused() {
        appPaused = !appPaused;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_PAUSED, appPaused).apply();
        updateMenuState();
    }

    private void setAppEnabled(boolean enabled) {
        appEnabled = enabled;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply();
        updateMenuState();
        // LIGAR/DESLIGAR alteram o estado do app, mas preservam o serviço e o atalho.
        // O serviço só é encerrado pelo comando de parar na tela principal.
    }

    private void updateMenuState() {
        if (menu == null) return;
        View v = menu.findViewWithTag("state");
        if (v instanceof TextView) {
            ((TextView)v).setText(!appEnabled ? "ESTADO: DESLIGADO" : (appPaused ? "ESTADO: PAUSADO" : "ESTADO: LIGADO"));
        if (pauseButton != null) pauseButton.setText(appPaused ? "RETOMAR" : "PAUSAR");
        }
    }

    public static boolean isAppEnabled(Context context) {
        return context.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_ENABLED, false);
    }

    public static boolean isAppPaused(Context context) {
        return context.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_PAUSED, false);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "UPDATE_OVERLAY_STYLE".equals(intent.getAction())) applyOverlayOpacity();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (uiHandler != null) uiHandler.removeCallbacksAndMessages(null);
        hideMenu();
        if (windowManager != null && visionBadge != null) {
            try { windowManager.removeView(visionBadge); } catch (Exception ignored) { }
        }
        if (windowManager != null && bubble != null) {
            try { windowManager.removeView(bubble); } catch (Exception ignored) { }
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}