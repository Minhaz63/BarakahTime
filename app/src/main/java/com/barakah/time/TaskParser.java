package com.barakah.time;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns spoken/typed Bangla or English into tasks.
 * "সকাল ৯টা থেকে ১১টা অ্যাসাইনমেন্ট, তারপর বিকেল ৪টা থেকে ৫টা জিম"
 * "study 9 to 11 then gym at 5pm for 1 hour"
 */
public final class TaskParser {
    private TaskParser() {}

    static String norm(String s) {
        if (s == null) return "";
        s = Normalizer.normalize(s, Normalizer.Form.NFC);
        // NFC decomposes these Bangla letters (composition exclusions); recompose for simple regex.
        s = s.replace("ড়", "ড়").replace("ঢ়", "ঢ়").replace("য়", "য়");
        StringBuilder b = new StringBuilder(s.length());
        for (char ch : s.toCharArray()) {
            if (ch >= '০' && ch <= '৯') b.append((char) ('0' + (ch - '০')));
            else b.append(ch);
        }
        return b.toString();
    }

    private static final String PERIOD = "(ভোর|সকাল|দুপুর|বিকাল|বিকেল|সন্ধ্যা|সন্ধা|রাত)";
    private static final String MOD = "(সাড়ে|পৌনে|সোয়া)";

    // groups: 1 period, 2 mod, 3 hour, 4 minute, 5 bangla suffix, 6 am/pm
    private static final Pattern TOKEN = Pattern.compile(norm(
            "(?:" + PERIOD + "(?:ে|য়|র|বেলা)?\\s*)?(?:" + MOD + "\\s*)?(\\d{1,2})(?:\\s*[:.]\\s*(\\d{2}))?"
            + "\\s*(টায়|টার|টাই|টা|টে)?\\s*(a\\.m\\.|p\\.m\\.|am|pm)?"),
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern DURATION_AFTER = Pattern.compile(norm(
            "^\\s*(মিনিট|ঘণ্টা|ঘন্টা|minutes|minute|mins|min|hours|hour|hrs|hr)"),
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern SEP = Pattern.compile(norm(
            "^\\s*(থেকে|হতে|to|till|until|-|–|—)\\s*$"), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern DURATION = Pattern.compile(norm(
            "(\\d+(?:\\.\\d)?|আধা|আধ|এক|দুই|দু|তিন|চার|half an?|one|two|three)\\s*(মিনিট|ঘণ্টা|ঘন্টা|minutes?|mins|min|hours?|hrs?)"),
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern SPLIT = Pattern.compile(norm(
            "\\s*(?:[,;।\\n]|\\bএবং তারপর\\b|তারপরে|তারপর|এরপরে|এরপর|\\band then\\b|\\bthen\\b)\\s*"),
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern FILLER = Pattern.compile(norm(
            "(^|\\s)(আজকে|আজ|আমি|আমার|from|at|today|i will|i'll|থেকে|পর্যন্ত|মধ্যে|পর্যন্ত|for)(?=\\s|$)"),
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    static class Tok {
        int s, e;
        String period, mod, suffix, ampm;
        int h, m;
        boolean timeLike() { return period != null || mod != null || suffix != null || ampm != null || m > 0; }
    }

    public static List<TaskItem> parseMany(String raw) {
        List<TaskItem> out = new ArrayList<>();
        String s = norm(raw).trim();
        if (s.isEmpty()) return out;
        int after = -1;
        for (String part : SPLIT.split(s)) {
            part = part.trim();
            if (part.length() < 2) continue;
            TaskItem t = parseOne(part, after);
            if (t != null) {
                out.add(t);
                if (t.end > 0) after = t.end; else if (t.start >= 0) after = t.start;
            }
        }
        return out;
    }

    public static TaskItem parseOne(String text) { return parseOne(text, -1); }

    /** after: end of the previous task in the same sentence, so "৭-৮ ..., এরপর ৯-১০" keeps the evening. */
    public static TaskItem parseOne(String text, int after) {
        String s = norm(text);
        List<Tok> toks = new ArrayList<>();
        Matcher m = TOKEN.matcher(s);
        while (m.find()) {
            if (m.group(3) == null) continue;
            if (DURATION_AFTER.matcher(s.substring(m.end())).find()) continue; // "30 মিনিট" is a length, not a time
            Tok t = new Tok();
            t.s = m.start(); t.e = m.end();
            t.period = m.group(1); t.mod = m.group(2); t.suffix = m.group(5); t.ampm = m.group(6);
            t.h = Integer.parseInt(m.group(3));
            t.m = m.group(4) != null ? Integer.parseInt(m.group(4)) : 0;
            if (t.h > 23 || t.m > 59) continue;
            toks.add(t);
        }

        TaskItem task = new TaskItem();
        task.date = Prefs.today();
        StringBuilder title = new StringBuilder(s);
        int now = TaskStore.nowMinutes();

        Tok a = null, b = null;
        for (int i = 0; i + 1 < toks.size(); i++) {
            String between = s.substring(toks.get(i).e, toks.get(i + 1).s);
            if (SEP.matcher(between).matches()) { a = toks.get(i); b = toks.get(i + 1); break; }
        }
        if (a != null) {
            int[] r = resolveRange(a, b, now, after);
            task.start = r[0];
            task.end = r[1];
            task.focus = true;
            title.replace(a.s, b.e, " ");
        } else {
            Tok single = null;
            for (Tok t : toks) {
                boolean prefixedAt = t.s >= 3 && s.substring(Math.max(0, t.s - 3), t.s).toLowerCase().trim().endsWith("at");
                if (t.timeLike() || prefixedAt) { single = t; break; }
            }
            if (single != null) {
                task.start = resolveSingle(single, now, after);
                int dur = -1;
                Matcher dm = DURATION.matcher(s);
                String durText = null;
                if (dm.find()) { dur = durationMinutes(dm.group(1), dm.group(2)); durText = dm.group(); }
                if (dur > 0) {
                    task.end = Math.min(1440, task.start + dur);
                    task.focus = true;
                }
                title.replace(single.s, single.e, " ");
                if (durText != null) {
                    int idx = title.indexOf(durText);
                    if (idx >= 0) title.replace(idx, idx + durText.length(), " ");
                }
            }
        }

        String t = FILLER.matcher(title.toString()).replaceAll(" ");
        t = t.replaceAll("\\s+", " ").replaceAll("^[\\s,.:;\\-–]+|[\\s,.:;\\-–]+$", "").trim();
        if (t.isEmpty()) t = "কাজ";
        task.title = t.substring(0, 1).toUpperCase() + t.substring(1);
        return task;
    }

    static int durationMinutes(String n, String unit) {
        double v;
        String x = n.toLowerCase();
        if (x.startsWith("আধ") || x.startsWith("half")) v = 0.5;
        else if (x.equals("এক") || x.equals("one")) v = 1;
        else if (x.equals("দুই") || x.equals("দু") || x.equals("two")) v = 2;
        else if (x.equals("তিন") || x.equals("three")) v = 3;
        else if (x.equals("চার")) v = 4;
        else { try { v = Double.parseDouble(x); } catch (Exception e) { return -1; } }
        String u = unit.toLowerCase();
        boolean minutes = u.startsWith("min") || u.equals("মিনিট");
        return (int) Math.round(minutes ? v : v * 60);
    }

    /** Returns minutes of day, or -(raw minutes) - 1 when no AM/PM hint was given. */
    static int withPeriod(Tok t) {
        int h = t.h, m = t.m;
        if (t.mod != null) {
            if (t.mod.equals(norm("সাড়ে"))) m = 30;
            else if (t.mod.equals(norm("সোয়া"))) m = 15;
            else if (t.mod.equals(norm("পৌনে"))) { h = h - 1; m = 45; if (h < 0) h = 23; }
        }
        String p = t.period;
        if (p != null) {
            if (p.equals("ভোর") || p.equals("সকাল")) { if (h == 12) h = 0; }
            else if (p.equals("দুপুর") || p.equals("বিকাল") || p.equals("বিকেল") || p.equals(norm("সন্ধ্যা")) || p.equals("সন্ধা")) { if (h < 12) h += 12; }
            else if (p.equals("রাত")) { if (h == 12) h = 0; else if (h >= 6 && h < 12) h += 12; }
            return h * 60 + m;
        }
        if (t.ampm != null) {
            boolean pm = t.ampm.toLowerCase().startsWith("p");
            if (pm && h < 12) h += 12;
            if (!pm && h == 12) h = 0;
            return h * 60 + m;
        }
        if (h > 12 || h == 0) return h * 60 + m; // 24h clock: unambiguous
        return -(h * 60 + m) - 1;
    }

    static int resolveSingle(Tok t, int now, int after) {
        int v = withPeriod(t);
        if (v >= 0) return v;
        int raw = -v - 1;
        if (after >= 0) return (raw < after && raw + 720 < 1440) ? raw + 720 : raw;
        int h = raw / 60;
        if (h >= 1 && h <= 6) return raw + 720;              // "৩টা" usually means 3 PM
        if (raw < now && raw + 720 < 1440) return raw + 720;  // already past this morning → evening
        return raw;
    }

    static int[] resolveRange(Tok a, Tok b, int now, int after) {
        int va = withPeriod(a), vb = withPeriod(b);
        int start, end;
        if (va >= 0 && vb >= 0) { start = va; end = vb; }
        else if (va >= 0) {
            start = va;
            int rb = -vb - 1;
            end = rb;
            while (end <= start && end + 720 <= 1440) end += 720;
        } else if (vb >= 0) {
            end = vb;
            int ra = -va - 1;
            start = ra;
            if (start + 720 < end) start += 720;
        } else {
            int ra = -va - 1, rb = -vb - 1;
            int h = ra / 60;
            start = ra; end = rb;
            if (after >= 0) {
                if (start < after && start + 720 < 1440) { start += 720; end += 720; }
            } else {
                if (h >= 1 && h <= 6) { start += 720; end += 720; }
            }
            if (end <= start && end + 720 <= 1440) end += 720;
            if (after < 0 && end <= now && start + 720 < 1440 && end + 720 <= 1440) { start += 720; end += 720; }
        }
        if (end <= start) end = Math.min(1440, start + 60);
        return new int[]{start, Math.min(end, 1440)};
    }
}
