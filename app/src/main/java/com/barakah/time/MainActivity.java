package com.barakah.time;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.LinearLayout;

import java.util.List;

/** Home: guard status, today's numbers, ayah of the day, next task. */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Reminders.ensureChannel(this);
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Usage.flush(this);
        render();
    }

    static boolean guardEnabled(Activity a) {
        String list = Settings.Secure.getString(a.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (list == null) return false;
        String me = new ComponentName(a, GuardService.class).flattenToString();
        String meShort = new ComponentName(a, GuardService.class).flattenToShortString();
        for (String s : list.split(":")) if (s.equalsIgnoreCase(me) || s.equalsIgnoreCase(meShort)) return true;
        return false;
    }

    private void render() {
        LinearLayout col = Ui.screen(this);
        Ui.title(this, col, "বারাকাহ টাইম");
        Ui.muted(this, col, "সময় আমানত — স্ক্রিন কম, বারাকাহ বেশি");
        Ui.spacer(this, col, 4);

        if (!guardEnabled(this)) {
            LinearLayout c = Ui.card(this, col);
            Ui.text(this, c, "পাহারাদার এখনো বন্ধ", 18, Ui.RED, true);
            Ui.body(this, c, "লিমিট, শর্টস ব্লক আর ইবাদত-গেট চালাতে Accessibility-তে \"বারাকাহ টাইম\" চালু করুন। অ্যাপটি শুধু কোন অ্যাপ সামনে আছে আর শর্টস স্ক্রিন কিনা দেখে — কিছু রেকর্ড বা পাঠায় না।");
            Ui.button(this, c, "Accessibility সেটিংস খুলুন", true,
                    v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
            Ui.muted(this, c, "Android 13+ এ \"Restricted setting\" দেখালে: সেটিংস → Apps → বারাকাহ টাইম → উপরের ⋮ → \"Allow restricted settings\" চাপুন, তারপর আবার চেষ্টা করুন।");
            Ui.small(this, c, "অ্যাপ ইনফো খুলুন", v -> startActivity(
                    new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))));
        }

        TaskItem focus = TaskStore.activeFocus(this);
        if (focus != null) {
            LinearLayout c = Ui.card(this, col);
            Ui.muted(this, c, "ফোকাস চলছে — বিভ্রান্তিকর অ্যাপ বন্ধ");
            Ui.text(this, c, focus.title, 20, Ui.GREEN, true);
            Ui.muted(this, c, Fmt.range(focus));
        }

        // ayah of the day
        LinearLayout q = Ui.card(this, col);
        Ui.muted(this, q, "আজকের আয়াত");
        Content.Ayah a = Content.ayahOfDay();
        Ui.arabic(this, q, a.arabic);
        Ui.body(this, q, a.meaning);
        Ui.muted(this, q, a.ref);

        // today
        LinearLayout t = Ui.card(this, col);
        Ui.h2(this, t, "আজকের হিসাব");
        Ui.text(this, t, "লিমিটেড অ্যাপে মোট: " + Fmt.dur(Usage.dayTotal(this)), 16, Ui.INK, true);
        List<AppRule> rules = Prefs.rules(this);
        for (AppRule r : rules) {
            if (!r.enabled) continue;
            int day = Usage.daySecs(this, r.pkg), hour = Usage.hourSecs(this, r.pkg);
            if (day == 0 && !isInstalled(r.pkg)) continue;
            String lim = r.perHour < 60 ? " / " + Fmt.bn(r.perHour) : "";
            Ui.body(this, t, r.label + ": আজ " + Fmt.dur(day) + " · এই ঘণ্টায় " + Fmt.bn(hour / 60) + lim + " মিনিট");
        }
        Ui.spacer(this, t, 6);
        Ui.body(this, t, "গেট থেকে ফিরে এসেছেন: " + Fmt.bn(Prefs.getDay(this, "returned")) + " বার");
        Ui.body(this, t, "শর্টস/রিলস আটকানো: " + Fmt.bn(Prefs.getDay(this, "shorts")) + " বার");
        Ui.body(this, t, "আজকের জিকির: " + Fmt.bn(Prefs.getDay(this, "dhikr")) + " (মোট " + Fmt.bn(Prefs.getTotal(this, "dhikr")) + ")");
        int[] tc = TaskStore.counts(this);
        Ui.body(this, t, "টাস্ক: " + Fmt.bn(tc[0]) + " / " + Fmt.bn(tc[1]) + " সম্পন্ন");

        // next task
        LinearLayout n = Ui.card(this, col);
        TaskItem next = TaskStore.nextPending(this);
        Ui.muted(this, n, "পরের কাজ");
        if (next != null) {
            Ui.text(this, n, next.title, 18, Ui.INK, true);
            if (next.start >= 0) Ui.muted(this, n, Fmt.range(next));
        } else {
            Ui.body(this, n, "আজকের কোনো টাস্ক নেই। বলে যোগ করুন — যেমন: \"সকাল ৯টা থেকে ১১টা পড়াশোনা\"");
        }
        Ui.button(this, n, "ভয়েসে টাস্ক বলুন", true,
                v -> startActivity(new Intent(this, TasksActivity.class).putExtra(TasksActivity.EXTRA_VOICE, true)));

        Ui.button(this, col, "টাস্ক তালিকা", false, v -> startActivity(new Intent(this, TasksActivity.class)));
        Ui.button(this, col, "অ্যাপ লিমিট", false, v -> startActivity(new Intent(this, RulesActivity.class)));
        Ui.button(this, col, "সেটিংস", false, v -> startActivity(new Intent(this, SettingsActivity.class)));

        LinearLayout hd = Ui.card(this, col);
        Content.Hadith h = Content.hadithOfDay();
        Ui.muted(this, hd, "আজকের হাদিস");
        Ui.body(this, hd, h.meaning);
        Ui.muted(this, hd, h.ref);
    }

    private boolean isInstalled(String pkg) {
        try { getPackageManager().getApplicationInfo(pkg, 0); return true; } catch (Exception e) { return false; }
    }
}
