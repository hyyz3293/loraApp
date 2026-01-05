package com.lora.cn.utils;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Build;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;

public final class ViewEffects {
    private static final int TAG_ID = 0x7f0a0001;

    private ViewEffects() {}

    public static void registerGlobal(Application app) {
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) { apply(activity); }
            @Override public void onActivityStarted(Activity activity) { apply(activity); }
            @Override public void onActivityResumed(Activity activity) { apply(activity); }
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });
    }

    public static void apply(Activity activity) {
        View root = activity.findViewById(android.R.id.content);
        if (root instanceof ViewGroup) {
            traverse((ViewGroup) root);
        }
    }

    private static void traverse(ViewGroup group) {
        int count = group.getChildCount();
        for (int i = 0; i < count; i++) {
            View v = group.getChildAt(i);
            if (v instanceof ViewGroup) {
                traverse((ViewGroup) v);
            }
            if (!v.isEnabled()) continue;
            boolean candidate = (v instanceof Button) || (v instanceof TextView) || (v instanceof ImageView);
            if (!candidate) continue;
            if (!v.isClickable()) continue;
            Object tag = v.getTag(TAG_ID);
            if (tag != null) continue;
            v.setTag(TAG_ID, Boolean.TRUE);
            ensureRipple(v);
            v.setOnTouchListener(new View.OnTouchListener() {
                boolean pressed;
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            pressed = true;
                            view.animate().scaleX(0.92f).scaleY(0.92f).alpha(0.6f).setDuration(70).start();
                            break;
                        case MotionEvent.ACTION_UP:
                            float x = event.getX();
                            float y = event.getY();
                            boolean inside = x >= 0 && y >= 0 && x <= view.getWidth() && y <= view.getHeight();
                            view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(110).withEndAction(() -> {
                                if (pressed && inside) {
                                    view.animate().scaleX(1.08f).scaleY(1.08f).setDuration(70).withEndAction(() ->
                                            view.animate().scaleX(1f).scaleY(1f).setDuration(70).start()
                                    ).start();
                                }
                            }).start();
                            pressed = false;
                            break;
                        case MotionEvent.ACTION_CANCEL:
                            view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(90).start();
                            pressed = false;
                            break;
                    }
                    return false;
                }
            });
        }
    }

    private static void ensureRipple(View v) {
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                if (v.getForeground() == null) {
                    TypedValue tv = new TypedValue();
                    boolean ok = v.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
                    if (ok && tv.resourceId != 0) {
                        v.setForeground(ContextCompat.getDrawable(v.getContext(), tv.resourceId));
                    }
                }
            }
        } catch (Throwable ignored) {}
    }
}
