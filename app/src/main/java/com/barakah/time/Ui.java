package com.barakah.time;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Tiny helper to build screens in code (no XML layouts, no AndroidX). */
public final class Ui {
    private Ui() {}

    public static final int BG = Color.parseColor("#F5F1E6");
    public static final int GREEN = Color.parseColor("#0F5132");
    public static final int GREEN_SOFT = Color.parseColor("#E3EFE7");
    public static final int GOLD = Color.parseColor("#B8901F");
    public static final int INK = Color.parseColor("#1E2A23");
    public static final int MUTED = Color.parseColor("#5F6B64");
    public static final int RED = Color.parseColor("#9B2C2C");
    public static final int CARD = Color.WHITE;

    public static int dp(Context c, int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }

    /** Root: a scrolling vertical column. Returns the column to add views into. */
    public static LinearLayout screen(Activity a) {
        ScrollView sv = new ScrollView(a);
        sv.setBackgroundColor(BG);
        sv.setFillViewport(true);
        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        int p = dp(a, 16);
        col.setPadding(p, dp(a, 24), p, dp(a, 32));
        sv.addView(col, new ViewGroup.LayoutParams(-1, -2));
        a.setContentView(sv);
        a.getWindow().setStatusBarColor(GREEN);
        return col;
    }

    public static LinearLayout card(Context c, LinearLayout parent) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(CARD);
        g.setCornerRadius(dp(c, 16));
        card.setBackground(g);
        int p = dp(c, 16);
        card.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(c, 12);
        card.setElevation(dp(c, 1));
        parent.addView(card, lp);
        return card;
    }

    public static TextView text(Context c, ViewGroup parent, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setLineSpacing(0, 1.15f);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(c, 6);
        parent.addView(t, lp);
        return t;
    }

    public static TextView title(Context c, ViewGroup parent, String s) { return text(c, parent, s, 24, GREEN, true); }
    public static TextView h2(Context c, ViewGroup parent, String s) { return text(c, parent, s, 17, INK, true); }
    public static TextView body(Context c, ViewGroup parent, String s) { return text(c, parent, s, 15, INK, false); }
    public static TextView muted(Context c, ViewGroup parent, String s) { return text(c, parent, s, 13, MUTED, false); }

    public static TextView arabic(Context c, ViewGroup parent, String s) {
        TextView t = text(c, parent, s, 24, INK, false);
        t.setGravity(Gravity.CENTER);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        t.setLineSpacing(0, 1.4f);
        return t;
    }

    public static Button button(Context c, ViewGroup parent, String s, boolean primary, View.OnClickListener l) {
        Button b = new Button(c);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTextColor(primary ? Color.WHITE : GREEN);
        GradientDrawable g = new GradientDrawable();
        g.setColor(primary ? GREEN : GREEN_SOFT);
        g.setCornerRadius(dp(c, 12));
        b.setBackground(g);
        b.setPadding(dp(c, 12), dp(c, 12), dp(c, 12), dp(c, 12));
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(c, 6);
        lp.bottomMargin = dp(c, 6);
        parent.addView(b, lp);
        return b;
    }

    public static void setEnabledLook(Button b, boolean enabled) {
        b.setEnabled(enabled);
        b.setAlpha(enabled ? 1f : 0.45f);
    }

    public static LinearLayout row(Context c, ViewGroup parent) {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        parent.addView(r, new LinearLayout.LayoutParams(-1, -2));
        return r;
    }

    public static Button small(Context c, ViewGroup parent, String s, View.OnClickListener l) {
        Button b = new Button(c);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTextColor(GREEN);
        GradientDrawable g = new GradientDrawable();
        g.setColor(GREEN_SOFT);
        g.setCornerRadius(dp(c, 10));
        b.setBackground(g);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(dp(c, 14), dp(c, 6), dp(c, 14), dp(c, 6));
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(dp(c, 4), dp(c, 4), dp(c, 4), dp(c, 4));
        parent.addView(b, lp);
        return b;
    }

    public static View spacer(Context c, ViewGroup parent, int h) {
        View v = new View(c);
        parent.addView(v, new LinearLayout.LayoutParams(-1, dp(c, h)));
        return v;
    }

    /** A weighted filler inside a horizontal row. */
    public static TextView grow(Context c, LinearLayout row, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        return t;
    }
}
