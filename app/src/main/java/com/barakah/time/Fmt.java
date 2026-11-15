package com.barakah.time;

/** Bangla number and time formatting. */
public final class Fmt {
    private Fmt() {}

    public static String bn(long n) {
        String s = String.valueOf(n);
        StringBuilder b = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (ch >= '0' && ch <= '9') b.append((char) ('০' + (ch - '0')));
            else b.append(ch);
        }
        return b.toString();
    }

    /** 545 → "সকাল ৯:০৫" */
    public static String time(int minutesOfDay) {
        if (minutesOfDay < 0) return "";
        if (minutesOfDay >= 1440) minutesOfDay = 1439;
        int h = minutesOfDay / 60, m = minutesOfDay % 60;
        String period;
        if (h < 4) period = "রাত";
        else if (h < 6) period = "ভোর";
        else if (h < 12) period = "সকাল";
        else if (h < 15) period = "দুপুর";
        else if (h < 18) period = "বিকাল";
        else if (h < 20) period = "সন্ধ্যা";
        else period = "রাত";
        int h12 = h % 12;
        if (h12 == 0) h12 = 12;
        return period + " " + bn(h12) + ":" + (m < 10 ? "০" : "") + bn(m);
    }

    public static String range(TaskItem t) {
        if (t.start < 0) return "";
        if (t.end > t.start) return time(t.start) + " – " + time(t.end);
        return time(t.start);
    }

    /** seconds → "১২ মিনিট" / "১ ঘণ্টা ৫ মিনিট" */
    public static String dur(int secs) {
        int mins = secs / 60;
        if (mins < 60) return bn(mins) + " মিনিট";
        return bn(mins / 60) + " ঘণ্টা " + (mins % 60 > 0 ? bn(mins % 60) + " মিনিট" : "");
    }
}
