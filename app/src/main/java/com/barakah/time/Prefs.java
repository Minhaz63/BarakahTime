package com.barakah.time;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** All settings, rules and daily counters live here (SharedPreferences + JSON). */
public final class Prefs {
    private Prefs() {}

    public static final String MODE_ROTATE = "rotate";
    public static final String MODE_QURAN = "quran";
    public static final String MODE_HADITH = "hadith";
    public static final String MODE_DHIKR = "dhikr";

    public static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences("barakah", Context.MODE_PRIVATE);
    }

    public static String today() {
        return new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
    }

    // ---------- settings ----------
    public static int sessionMinutes(Context c) { return sp(c).getInt("session_min", 5); }
    public static void setSessionMinutes(Context c, int v) { sp(c).edit().putInt("session_min", clamp(v, 1, 30)).apply(); }

    public static int baseDhikr(Context c) { return sp(c).getInt("base_dhikr", 11); }
    public static void setBaseDhikr(Context c, int v) { sp(c).edit().putInt("base_dhikr", clamp(v, 3, 100)).apply(); }

    public static int reminderMinutes(Context c) { return sp(c).getInt("reminder_min", 10); }
    public static void setReminderMinutes(Context c, int v) { sp(c).edit().putInt("reminder_min", clamp(v, 3, 60)).apply(); }

    public static String challengeMode(Context c) { return sp(c).getString("challenge", MODE_ROTATE); }
    public static void setChallengeMode(Context c, String v) { sp(c).edit().putString("challenge", v).apply(); }

    public static boolean strict(Context c) { return sp(c).getBoolean("strict", false); }
    public static void setStrict(Context c, boolean v) { sp(c).edit().putBoolean("strict", v).apply(); }

    public static boolean flag(Context c, String key) { return sp(c).getBoolean(key, true); }
    public static void setFlag(Context c, String key, boolean v) { sp(c).edit().putBoolean(key, v).apply(); }

    public static final String BLOCK_YT_SHORTS = "block_yt_shorts";
    public static final String BLOCK_FB_REELS = "block_fb_reels";
    public static final String BLOCK_IG_REELS = "block_ig_reels";
    public static final String BLOCK_TIKTOK = "block_tiktok";

    public static final int EMERGENCY_PER_DAY = 3;

    // ---------- daily counters ----------
    public static int getDay(Context c, String key) {
        return sp(c).getInt("d_" + today() + "_" + key, 0);
    }

    public static int incDay(Context c, String key, int by) {
        String k = "d_" + today() + "_" + key;
        int v = sp(c).getInt(k, 0) + by;
        sp(c).edit().putInt(k, v).apply();
        return v;
    }

    public static long getTotal(Context c, String key) { return sp(c).getLong("t_" + key, 0); }

    public static void incTotal(Context c, String key, long by) {
        sp(c).edit().putLong("t_" + key, getTotal(c, key) + by).apply();
    }

    // ---------- app rules ----------
    public static List<AppRule> rules(Context c) {
        String raw = sp(c).getString("rules", null);
        List<AppRule> out = new ArrayList<>();
        if (raw == null) {
            out.add(new AppRule("com.facebook.katana", "Facebook", 10, 0, true));
            out.add(new AppRule("com.facebook.lite", "Facebook Lite", 10, 0, true));
            out.add(new AppRule("com.google.android.youtube", "YouTube", 15, 0, true));
            out.add(new AppRule("com.instagram.android", "Instagram", 10, 0, true));
            saveRules(c, out);
            return out;
        }
        try {
            JSONArray a = new JSONArray(raw);
            for (int i = 0; i < a.length(); i++) out.add(AppRule.fromJson(a.getJSONObject(i)));
        } catch (Exception ignored) {}
        return out;
    }

    public static void saveRules(Context c, List<AppRule> rules) {
        JSONArray a = new JSONArray();
        for (AppRule r : rules) a.put(r.toJson());
        sp(c).edit().putString("rules", a.toString()).apply();
        Guard.invalidateRules();
    }

    public static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    /** Remove counters and usage buckets older than ~8 days. */
    public static void cleanup(Context c) {
        String cutoff = new SimpleDateFormat("yyyyMMdd", Locale.US)
                .format(new Date(System.currentTimeMillis() - 8L * 24 * 3600 * 1000));
        SharedPreferences.Editor e = sp(c).edit();
        for (String k : sp(c).getAll().keySet()) {
            if ((k.startsWith("d_") || k.startsWith("u_") || k.startsWith("h_")) && k.length() >= 10) {
                String date = k.substring(2, 10);
                if (date.compareTo(cutoff) < 0) e.remove(k);
            }
        }
        e.apply();
    }

    static JSONObject obj(String raw) {
        try { return raw == null ? new JSONObject() : new JSONObject(raw); } catch (Exception e) { return new JSONObject(); }
    }
}
