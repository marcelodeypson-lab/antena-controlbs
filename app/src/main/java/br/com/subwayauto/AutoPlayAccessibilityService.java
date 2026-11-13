package br.com.subwayauto;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;

public class AutoPlayAccessibilityService extends AccessibilityService {
    private static volatile boolean connected = false;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        connected = true;
    }

    @Override
    public void onDestroy() {
        connected = false;
        super.onDestroy();
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { connected = false; }

    public static boolean isConnected() {
        return connected;
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
