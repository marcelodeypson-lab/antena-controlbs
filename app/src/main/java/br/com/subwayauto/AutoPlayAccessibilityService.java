package br.com.subwayauto;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;

public class AutoPlayAccessibilityService extends AccessibilityService {
    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { }
    public static boolean isEnabled(Context context) {
        String setting = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (setting == null) return false;
        android.content.ComponentName component = new android.content.ComponentName(context, AutoPlayAccessibilityService.class);
        return TextUtils.SimpleStringSplitter.class != null && setting.toLowerCase().contains(component.flattenToString().toLowerCase());
    }
    public void swipe(float x1, float y1, float x2, float y2, long durationMs) {
        android.graphics.Path path = new android.graphics.Path(); path.moveTo(x1, y1); path.lineTo(x2, y2);
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0, durationMs);
        dispatchGesture(new GestureDescription.Builder().addStroke(stroke).build(), null, null);
    }
}
