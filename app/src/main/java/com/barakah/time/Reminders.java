package com.barakah.time;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

/** Notifications and task alarms. */
public final class Reminders {
    private Reminders() {}

    static final String CHANNEL = "barakah_reminders";

    public static void ensureChannel(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(CHANNEL) == null) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "টাস্ক ও স্ক্রিন টাইম রিমাইন্ডার",
                    NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("কাজের সময় হলে এবং বেশি স্ক্রল করলে মনে করিয়ে দেয়");
            nm.createNotificationChannel(ch);
        }
    }

    public static void notify(Context c, int id, String title, String text) {
        ensureChannel(c);
        Intent open = new Intent(c, TasksActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(c, id, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(c.getApplicationInfo().icon)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();
        try {
            c.getSystemService(NotificationManager.class).notify(id, n);
        } catch (SecurityException ignored) {
            // notification permission not granted
        }
    }

    private static long atToday(int minutesOfDay) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis() + minutesOfDay * 60_000L;
    }

    private static PendingIntent pi(Context c, TaskItem t, boolean end) {
        Intent i = new Intent(c, ReminderReceiver.class)
                .putExtra("id", t.id).putExtra("end", end);
        int code = (int) ((t.id % 1_000_000) * 2 + (end ? 1 : 0));
        return PendingIntent.getBroadcast(c, code, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void schedule(Context c, TaskItem t) {
        if (t.done || t.start < 0 || !Prefs.today().equals(t.date)) return;
        AlarmManager am = c.getSystemService(AlarmManager.class);
        long now = System.currentTimeMillis();
        long start = atToday(t.start);
        if (start > now) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, start, pi(c, t, false));
        if (t.end > t.start) {
            long end = atToday(t.end);
            if (end > now) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, pi(c, t, true));
        }
    }

    public static void cancel(Context c, TaskItem t) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        am.cancel(pi(c, t, false));
        am.cancel(pi(c, t, true));
    }

    public static void scheduleAll(Context c) {
        for (TaskItem t : TaskStore.today(c)) schedule(c, t);
    }
}
