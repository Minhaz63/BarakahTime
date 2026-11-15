package com.barakah.time;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        render();
    }

    private void render() {
        LinearLayout col = Ui.screen(this);
        Ui.title(this, col, "সেটিংস");

        LinearLayout ch = Ui.card(this, col);
        Ui.h2(this, ch, "গেটে কী করতে হবে");
        String m = Prefs.challengeMode(this);
        LinearLayout r1 = Ui.row(this, ch);
        modeButton(r1, "পালাক্রমে", Prefs.MODE_ROTATE, m);
        modeButton(r1, "আয়াত", Prefs.MODE_QURAN, m);
        LinearLayout r2 = Ui.row(this, ch);
        modeButton(r2, "হাদিস", Prefs.MODE_HADITH, m);
        modeButton(r2, "জিকির", Prefs.MODE_DHIKR, m);
        Ui.muted(this, ch, "পালাক্রমে রাখলে প্রতিবার আলাদা কিছু আসে, তাই না ভেবে পার হওয়ার অভ্যাস তৈরি হয় না।");

        LinearLayout nums = Ui.card(this, col);
        Ui.h2(this, nums, "সংখ্যা");
        // more dhikr / shorter session / earlier reminder = tighter (instant); the opposite = looser (strict wait)
        number(nums, "শুরুর জিকির সংখ্যা", Prefs.baseDhikr(this), 1, false, v -> Prefs.setBaseDhikr(this, v));
        number(nums, "প্রতি গেটে সেশন (মিনিট)", Prefs.sessionMinutes(this), 1, true, v -> Prefs.setSessionMinutes(this, v));
        number(nums, "টানা স্ক্রলে রিমাইন্ডার (মিনিট)", Prefs.reminderMinutes(this), 1, true, v -> Prefs.setReminderMinutes(this, v));
        Ui.muted(this, nums, "প্রতিবার গেট পার হলে সেদিন জিকির ৫টা করে আর পড়ার সময় ৪ সেকেন্ড করে বাড়ে।");

        LinearLayout sh = Ui.card(this, col);
        Ui.h2(this, sh, "শর্টস ও রিলস ব্লক");
        toggle(sh, "YouTube Shorts", Prefs.BLOCK_YT_SHORTS);
        toggle(sh, "Facebook Reels", Prefs.BLOCK_FB_REELS);
        toggle(sh, "Instagram Reels", Prefs.BLOCK_IG_REELS);
        toggle(sh, "TikTok (পুরো অ্যাপ)", Prefs.BLOCK_TIKTOK);
        Ui.muted(this, sh, "অ্যাপের বড় আপডেটের পর কোনো স্ক্রিন ধরা না পড়লে জানাবেন — শনাক্ত করার তালিকা হালনাগাদ করতে হতে পারে।");

        LinearLayout st = Ui.card(this, col);
        Ui.h2(this, st, "কঠোর মোড");
        LinearLayout row = Ui.row(this, st);
        Ui.grow(this, row, "লিমিট ঢিলা করতে ৬০ সেকেন্ড অপেক্ষা", 15, Ui.INK, false);
        Switch sw = new Switch(this);
        sw.setChecked(Prefs.strict(this));
        sw.setOnCheckedChangeListener((b, on) -> {
            if (on) { Prefs.setStrict(this, true); return; }
            b.setChecked(true);
            StrictGate.loosen(this, "কঠোর মোড বন্ধ করবেন?", () -> { Prefs.setStrict(this, false); render(); });
        });
        row.addView(sw);
        Ui.muted(this, st, "হঠাৎ ইচ্ছা সাধারণত এক মিনিটেই কেটে যায়। শান্ত মাথায় নিজের জন্য যে নিয়ম করেছেন, সেটা দুর্বল মুহূর্তে ভাঙা কঠিন করে দেয়।");

        Ui.button(this, col, "ফিরে যান", false, v -> finish());
    }

    private void modeButton(LinearLayout row, String label, String value, String current) {
        Ui.small(this, row, (value.equals(current) ? "✓ " : "") + label, v -> {
            Prefs.setChallengeMode(this, value);
            render();
        });
    }

    interface IntSetter { void set(int v); }

    /** upIsLoose: whether increasing the number loosens the rule. */
    private void number(LinearLayout parent, String label, int value, int step, boolean upIsLoose, IntSetter s) {
        LinearLayout row = Ui.row(this, parent);
        Ui.grow(this, row, label, 15, Ui.INK, false);
        Ui.small(this, row, "−", v -> change(label, value - step, !upIsLoose, s));
        TextView t = new TextView(this);
        t.setText(Fmt.bn(value));
        t.setTextSize(16);
        t.setTextColor(Ui.GREEN);
        t.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 8), 0);
        row.addView(t);
        Ui.small(this, row, "+", v -> change(label, value + step, upIsLoose, s));
    }

    private void change(String label, int nv, boolean loosening, IntSetter s) {
        if (loosening) StrictGate.loosen(this, label + " পরিবর্তন করবেন?", () -> { s.set(nv); render(); });
        else { s.set(nv); render(); }
    }

    private void toggle(LinearLayout parent, String label, String key) {
        LinearLayout row = Ui.row(this, parent);
        Ui.grow(this, row, label, 15, Ui.INK, false);
        Switch sw = new Switch(this);
        sw.setChecked(Prefs.flag(this, key));
        sw.setOnCheckedChangeListener((b, on) -> {
            if (on) { Prefs.setFlag(this, key, true); return; }
            b.setChecked(true);
            StrictGate.loosen(this, label + " ব্লক বন্ধ করবেন?", () -> { Prefs.setFlag(this, key, false); render(); });
        });
        row.addView(sw);
    }
}
