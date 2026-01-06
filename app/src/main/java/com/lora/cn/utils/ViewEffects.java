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
            ensureRipple(v);
            applyPressedAnimator(v);
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

    private static void applyPressedAnimator(View v) {
        try {
            if (Build.VERSION.SDK_INT >= 21) {
                if (v.getStateListAnimator() != null && v.getTag(TAG_ID) != null) return;
                android.animation.StateListAnimator sla = new android.animation.StateListAnimator();
                android.animation.ObjectAnimator pressX = android.animation.ObjectAnimator.ofFloat(v, "scaleX", 0.90f);
                android.animation.ObjectAnimator pressY = android.animation.ObjectAnimator.ofFloat(v, "scaleY", 0.90f);
                android.animation.ObjectAnimator pressA = android.animation.ObjectAnimator.ofFloat(v, "alpha", 0.6f);
                pressX.setDuration(80);
                pressY.setDuration(80);
                pressA.setDuration(80);
                android.animation.AnimatorSet pressSet = new android.animation.AnimatorSet();
                pressSet.playTogether(pressX, pressY, pressA);
                sla.addState(new int[]{android.R.attr.state_pressed}, pressSet);

                android.animation.ObjectAnimator normalX = android.animation.ObjectAnimator.ofFloat(v, "scaleX", 1f);
                android.animation.ObjectAnimator normalY = android.animation.ObjectAnimator.ofFloat(v, "scaleY", 1f);
                android.animation.ObjectAnimator normalA = android.animation.ObjectAnimator.ofFloat(v, "alpha", 1f);
                normalX.setDuration(120);
                normalY.setDuration(120);
                normalA.setDuration(120);
                android.animation.AnimatorSet normalSet = new android.animation.AnimatorSet();
                normalSet.playTogether(normalX, normalY, normalA);
                sla.addState(new int[]{}, normalSet);

                v.setStateListAnimator(sla);
                v.setTag(TAG_ID, Boolean.TRUE);
            }
        } catch (Throwable ignored) {}
    }
}
