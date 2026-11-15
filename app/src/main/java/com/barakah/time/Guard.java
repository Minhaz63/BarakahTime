package com.barakah.time;

import android.content.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shared in-memory state + the blocking policy. */
public final class Guard {
    private Guard() {}

    public static final String REASON_FOCUS = "focus";
    public static final String REASON_HOUR = "hour";
    public static final String REASON_DAY = "day";
    public static final String REASON_ZERO = "zero";

    private static final Map<String, Long> sessions = new HashMap<>();
    private static final Map<String, Long> emergency = new HashMap<>();
    private static List<AppRule> cachedRules;

    public static synchronized void invalidateRules() { cachedRules = null; }

    public static synchronized AppRule rule(Context c, String pkg) {
        if (pkg == null) return null;
        if (cachedRules == null) cachedRules = Prefs.rules(c);
        for (AppRule r : cachedRules) if (r.enabled && pkg.equals(r.pkg)) return r;
        return null;
    }

    public static synchronized void grant(String pkg, int minutes) {
        sessions.put(pkg, System.currentTimeMillis() + minutes * 60_000L);
    }

    public static synchronized void grantEmergency(String pkg, int minutes) {
        emergency.put(pkg, System.currentTimeMillis() + minutes * 60_000L);
    }

    public static synchronized boolean hasSession(String pkg) {
        Long t = sessions.get(pkg);
        return t != null && t > System.currentTimeMillis();
    }

    public static synchronized boolean hasEmergency(String pkg) {
        Long t = emergency.get(pkg);
        return t != null && t > System.currentTimeMillis();
    }

    public static synchronized void endSession(String pkg) { sessions.remove(pkg); }

    /** Why this app must be blocked right now, or null if it may be used (after a gate). */
    public static String blockReason(Context c, AppRule r) {
        if (TaskStore.activeFocus(c) != null) return REASON_FOCUS;
        if (r.perHour <= 0) return REASON_ZERO;
        if (r.perHour < 60 && Usage.hourSecs(c, r.pkg) >= r.perHour * 60) return REASON_HOUR;
        if (r.perDay > 0 && Usage.daySecs(c, r.pkg) >= r.perDay * 60) return REASON_DAY;
        return null;
    }

    /** Minutes still available for this app in this hour / today (whichever is smaller). */
    public static int remainingMinutes(Context c, AppRule r) {
        int rem = Integer.MAX_VALUE;
        if (r.perHour < 60) rem = r.perHour * 60 - Usage.hourSecs(c, r.pkg);
        if (r.perDay > 0) rem = Math.min(rem, r.perDay * 60 - Usage.daySecs(c, r.pkg));
        if (rem == Integer.MAX_VALUE) return 60;
        return Math.max(0, (rem + 59) / 60);
    }

    /** How many times the gate was passed today for this app: used to escalate the price. */
    public static int level(Context c, String pkg) { return Prefs.getDay(c, "gate_" + pkg); }
}
