package com.barakah.time;

import android.content.Context;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class TaskStore {
    private TaskStore() {}

    private static List<TaskItem> cache;

    public static synchronized List<TaskItem> all(Context c) {
        if (cache == null) {
            cache = new ArrayList<>();
            try {
                JSONArray a = new JSONArray(Prefs.sp(c).getString("tasks", "[]"));
                for (int i = 0; i < a.length(); i++) cache.add(TaskItem.fromJson(a.getJSONObject(i)));
            } catch (Exception ignored) {}
        }
        return cache;
    }

    public static synchronized void save(Context c) {
        JSONArray a = new JSONArray();
        String cutoff = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
                .format(new java.util.Date(System.currentTimeMillis() - 30L * 24 * 3600 * 1000));
        List<TaskItem> keep = new ArrayList<>();
        for (TaskItem t : all(c)) {
            // drop finished tasks older than ~a month
            if (t.done && t.date.compareTo(cutoff) < 0) continue;
            keep.add(t);
            a.put(t.toJson());
        }
        cache = keep;
        Prefs.sp(c).edit().putString("tasks", a.toString()).apply();
    }

    public static synchronized void add(Context c, TaskItem t) {
        t.id = System.currentTimeMillis() + all(c).size();
        if (t.date == null) t.date = Prefs.today();
        all(c).add(t);
        save(c);
        Reminders.schedule(c, t);
    }

    public static synchronized void delete(Context c, TaskItem t) {
        Reminders.cancel(c, t);
        all(c).remove(t);
        save(c);
    }

    /** Today's tasks, plus unfinished untimed tasks carried over from earlier days. Sorted by time. */
    public static synchronized List<TaskItem> today(Context c) {
        String today = Prefs.today();
        List<TaskItem> out = new ArrayList<>();
        for (TaskItem t : all(c)) {
            if (today.equals(t.date)) out.add(t);
            else if (t.date.compareTo(today) < 0 && !t.done && t.start < 0) out.add(t);
        }
        Collections.sort(out, new Comparator<TaskItem>() {
            @Override public int compare(TaskItem a, TaskItem b) {
                if (a.done != b.done) return a.done ? 1 : -1;
                int sa = a.start < 0 ? 9999 : a.start, sb = b.start < 0 ? 9999 : b.start;
                return sa - sb;
            }
        });
        return out;
    }

    public static int nowMinutes() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
    }

    /** A focus task running right now, if any. */
    public static synchronized TaskItem activeFocus(Context c) {
        String today = Prefs.today();
        int now = nowMinutes();
        for (TaskItem t : all(c)) {
            if (!t.done && t.focus && t.hasRange() && today.equals(t.date) && now >= t.start && now < t.end) return t;
        }
        return null;
    }

    /** The task the person should be doing now / next. */
    public static synchronized TaskItem nextPending(Context c) {
        TaskItem focus = activeFocus(c);
        if (focus != null) return focus;
        int now = nowMinutes();
        TaskItem best = null;
        for (TaskItem t : today(c)) {
            if (t.done) continue;
            if (t.start >= 0 && t.end > 0 && t.end <= now) continue; // window already passed
            if (best == null) best = t;
        }
        if (best != null) return best;
        for (TaskItem t : today(c)) if (!t.done) return t;
        return null;
    }

    public static synchronized int[] counts(Context c) {
        int done = 0, total = 0;
        for (TaskItem t : today(c)) { total++; if (t.done) done++; }
        return new int[]{done, total};
    }
}
