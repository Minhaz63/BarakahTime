package com.barakah.time;

import android.content.Context;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

/** Seconds spent per app, bucketed by clock hour and by day. Cached in memory, flushed to prefs. */
public final class Usage {
    private Usage() {}

    private static String hourKey, dayKey;
    private static JSONObject hour, day;
    private static boolean dirty;

    private static synchronized void ensure(Context c) {
        Date now = new Date();
        String h = new SimpleDateFormat("yyyyMMddHH", Locale.US).format(now);
        String d = h.substring(0, 8);
        if (!h.equals(hourKey)) {
            flush(c);
            hourKey = h;
            hour = Prefs.obj(Prefs.sp(c).getString("h_" + h, null));
        }
        if (!d.equals(dayKey)) {
            flush(c);
            dayKey = d;
            day = Prefs.obj(Prefs.sp(c).getString("u_" + d, null));
            Prefs.cleanup(c);
        }
    }

    public static synchronized void add(Context c, String pkg, int secs) {
        ensure(c);
        try {
            hour.put(pkg, hour.optInt(pkg, 0) + secs);
            day.put(pkg, day.optInt(pkg, 0) + secs);
            dirty = true;
        } catch (Exception ignored) {}
    }

    public static synchronized int hourSecs(Context c, String pkg) {
        ensure(c);
        return hour.optInt(pkg, 0);
    }

    public static synchronized int daySecs(Context c, String pkg) {
        ensure(c);
        return day.optInt(pkg, 0);
    }

    public static synchronized int dayTotal(Context c) {
        ensure(c);
        int t = 0;
        Iterator<String> it = day.keys();
        while (it.hasNext()) t += day.optInt(it.next(), 0);
        return t;
    }

    public static synchronized void flush(Context c) {
        if (!dirty || hourKey == null || dayKey == null) return;
        Prefs.sp(c).edit()
                .putString("h_" + hourKey, hour.toString())
                .putString("u_" + dayKey, day.toString())
                .apply();
        dirty = false;
    }

    /** Seconds until the next clock hour starts. */
    public static int secsToNextHour() {
        long now = System.currentTimeMillis();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(now);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        cal.add(java.util.Calendar.HOUR_OF_DAY, 1);
        return (int) ((cal.getTimeInMillis() - now) / 1000);
    }
}
