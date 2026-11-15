package com.barakah.time;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Random;

/**
 * The pause screen shown before a limited app opens.
 * Challenge mode: intention question → Qur'an / hadith / dhikr → a short session.
 * Blocked mode: hourly/daily quota used up or a focus task running → go home (or a costly emergency pass).
 */
public class GateActivity extends Activity {

    public static final String EXTRA_PKG = "pkg";
    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_REASON = "reason";
    public static final String MODE_CHALLENGE = "challenge";
    public static final String MODE_BLOCKED = "blocked";

    private final Handler h = new Handler(Looper.getMainLooper());
    private final Random rng = new Random();
    private int renderToken;

    private String pkg, mode, reason, label;
    private AppRule rule;
    private LinearLayout col, challengeBox;
    private Button continueBtn;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        render(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        render(intent);
    }

    @Override
    public void onBackPressed() {
        goHome();
    }

    private void goHome() {
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(home);
        finish();
    }

    private void render(Intent i) {
        renderToken++;
        h.removeCallbacksAndMessages(null);
        pkg = i.getStringExtra(EXTRA_PKG);
        mode = i.getStringExtra(EXTRA_MODE);
        reason = i.getStringExtra(EXTRA_REASON);
        rule = Guard.rule(this, pkg);
        if (pkg == null || rule == null) { finish(); return; }
        label = rule.label;

        col = Ui.screen(this);
        Ui.title(this, col, MODE_BLOCKED.equals(mode) ? "এখন নয়" : "একটু থামি");
        Ui.body(this, col, "আপনি " + label + " খুলতে যাচ্ছেন।");

        int hourUsed = Usage.hourSecs(this, pkg), dayUsed = Usage.daySecs(this, pkg);
        String quota = rule.perHour < 60 ? " · এই ঘণ্টায় বাকি " + Fmt.bn(Guard.remainingMinutes(this, rule)) + " মিনিট" : "";
        Ui.muted(this, col, "এই ঘণ্টায় " + Fmt.dur(hourUsed) + " · আজ " + Fmt.dur(dayUsed) + quota);
        Ui.spacer(this, col, 8);

        if (MODE_BLOCKED.equals(mode)) renderBlocked();
        else renderIntention();

        renderNextTask();
    }

    // ---------------- challenge mode ----------------

    private void renderIntention() {
        LinearLayout card = Ui.card(this, col);
        Ui.h2(this, card, "কেন খুলছেন?");
        Ui.muted(this, card, "সৎ উত্তর দিন — নিয়তই আমলের মূল।");
        final TextView reply = Ui.body(this, card, "");
        reply.setTextColor(Ui.GREEN);
        LinearLayout r1 = Ui.row(this, card);
        Ui.small(this, r1, "জরুরি কাজ / মেসেজ", v -> chooseIntention(reply, 0));
        Ui.small(this, r1, "কিছু শিখতে", v -> chooseIntention(reply, 1));
        LinearLayout r2 = Ui.row(this, card);
        Ui.small(this, r2, "শুধু অভ্যাসে / বোরিং লাগছে", v -> chooseIntention(reply, 2));

        challengeBox = new LinearLayout(this);
        challengeBox.setOrientation(LinearLayout.VERTICAL);
        col.addView(challengeBox);

        Ui.button(this, col, "থাক, ফিরে যাই", true, v -> {
            Prefs.incDay(this, "returned", 1);
            Prefs.incTotal(this, "returned", 1);
            goHome();
        });
        int minutes = Math.min(Prefs.sessionMinutes(this), Guard.remainingMinutes(this, rule));
        continueBtn = Ui.button(this, col, "চালিয়ে যাই (" + Fmt.bn(minutes) + " মিনিট)", false, v -> {
            Prefs.incDay(this, "gate_" + pkg, 1);
            Prefs.incDay(this, "passed", 1);
            Guard.grant(pkg, Math.max(1, minutes));
            finish();
        });
        Ui.setEnabledLook(continueBtn, false);
    }

    private void chooseIntention(TextView reply, int which) {
        if (which == 0) reply.setText("ঠিক আছে। কাজ সেরে দ্রুত বের হয়ে আসবেন, ইনশাআল্লাহ।");
        else if (which == 1) reply.setText("ভালো নিয়ত। শুধু খেয়াল রাখবেন শেখা যেন স্ক্রলে না গড়ায়।");
        else {
            reply.setText("সৎ উত্তরের জন্য ধন্যবাদ। এই টান সাধারণত কয়েক মিনিটেই কমে যায়। পানি খান, একটু হাঁটুন বা দুই রাকাত নফল পড়ুন — নিচের সবুজ বাটনটাই আজকের জয়।");
            Prefs.incDay(this, "honest_habit", 1);
        }
        if (challengeBox.getChildCount() == 0) renderChallenge();
    }

    private void renderChallenge() {
        int level = Guard.level(this, pkg);
        String m = Prefs.challengeMode(this);
        if (Prefs.MODE_ROTATE.equals(m)) {
            int x = rng.nextInt(3);
            m = x == 0 ? Prefs.MODE_QURAN : x == 1 ? Prefs.MODE_HADITH : Prefs.MODE_DHIKR;
        }
        LinearLayout card = Ui.card(this, challengeBox);
        if (level > 0) Ui.muted(this, card, "আজ " + Fmt.bn(level) + " বার খুলেছেন — তাই এবারের থামা একটু লম্বা।");

        int readSecs = Math.min(60, 12 + 4 * level);
        if (Prefs.MODE_QURAN.equals(m)) {
            Content.Ayah a = Content.randomAyah();
            Ui.h2(this, card, "এই আয়াতটি মন দিয়ে পড়ুন");
            Ui.arabic(this, card, a.arabic);
            Ui.body(this, card, a.meaning);
            Ui.muted(this, card, a.ref);
            readTimer(card, readSecs);
        } else if (Prefs.MODE_HADITH.equals(m)) {
            Content.Hadith hd = Content.randomHadith();
            Ui.h2(this, card, "রাসূলুল্লাহ ﷺ বলেছেন");
            Ui.text(this, card, hd.meaning, 17, Ui.INK, false);
            Ui.muted(this, card, hd.ref);
            readTimer(card, readSecs);
        } else {
            int target = Math.min(100, Prefs.baseDhikr(this) + 5 * level);
            dhikrCounter(card, Content.randomDhikr(), target, () -> Ui.setEnabledLook(continueBtn, true));
        }
    }

    private void readTimer(LinearLayout card, int secs) {
        final Button b = Ui.button(this, card, "", false, null);
        Ui.setEnabledLook(b, false);
        final int token = renderToken;
        final int[] left = {secs};
        Runnable r = new Runnable() {
            @Override public void run() {
                if (token != renderToken) return;
                if (left[0] > 0) {
                    b.setText("পড়ছি… " + Fmt.bn(left[0]));
                    left[0]--;
                    h.postDelayed(this, 1000);
                } else {
                    b.setText("পড়েছি — আলহামদুলিল্লাহ");
                    Ui.setEnabledLook(continueBtn, true);
                }
            }
        };
        r.run();
    }

    private void dhikrCounter(LinearLayout card, Content.Dhikr d, int target, Runnable onDone) {
        Ui.h2(this, card, "মুখে বলে " + Fmt.bn(target) + " বার জিকির করুন");
        Ui.arabic(this, card, d.arabic);
        TextView sp = Ui.text(this, card, d.spoken, 18, Ui.GREEN, true);
        sp.setGravity(Gravity.CENTER);
        TextView mean = Ui.muted(this, card, d.meaning);
        mean.setGravity(Gravity.CENTER);
        final TextView hint = Ui.muted(this, card, "");
        hint.setGravity(Gravity.CENTER);
        final int[] count = {0};
        final long[] last = {0};
        final Button tap = Ui.button(this, card, "০ / " + Fmt.bn(target) + "  — চাপুন", true, null);
        tap.setTextSize(22);
        tap.setMinHeight(Ui.dp(this, 120));
        tap.setOnClickListener(v -> {
            if (count[0] >= target) return;
            long now = System.currentTimeMillis();
            if (now - last[0] < 450) { hint.setText("ধীরে — প্রতিবার মুখে বলুন"); return; }
            last[0] = now;
            hint.setText("");
            count[0]++;
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            if (count[0] >= target) {
                tap.setText("সম্পন্ন — " + d.spoken);
                Prefs.incDay(this, "dhikr", target);
                Prefs.incTotal(this, "dhikr", target);
                onDone.run();
            } else {
                tap.setText(Fmt.bn(count[0]) + " / " + Fmt.bn(target));
            }
        });
    }

    // ---------------- blocked mode ----------------

    private void renderBlocked() {
        LinearLayout card = Ui.card(this, col);
        if (Guard.REASON_FOCUS.equals(reason)) {
            TaskItem f = TaskStore.activeFocus(this);
            Ui.h2(this, card, "এখন ফোকাস সময়");
            if (f != null) {
                Ui.text(this, card, f.title, 20, Ui.GREEN, true);
                Ui.muted(this, card, Fmt.range(f) + " — এই সময় " + label + " বন্ধ।");
            }
        } else if (Guard.REASON_HOUR.equals(reason)) {
            int secs = Usage.secsToNextHour();
            int nextAt = (TaskStore.nowMinutes() + (secs + 59) / 60) % 1440;
            Ui.h2(this, card, "এই ঘণ্টার কোটা শেষ");
            Ui.body(this, card, label + "-এর জন্য প্রতি ঘণ্টায় " + Fmt.bn(rule.perHour) + " মিনিট ঠিক করেছিলেন। আবার খুলবে "
                    + Fmt.time(nextAt) + "-এ — " + Fmt.bn((secs + 59) / 60) + " মিনিট পর।");
        } else if (Guard.REASON_DAY.equals(reason)) {
            Ui.h2(this, card, "আজকের কোটা শেষ");
            Ui.body(this, card, "আজ " + label + "-এ " + Fmt.bn(rule.perDay) + " মিনিট পূর্ণ হয়েছে। আগামীকাল আবার, ইনশাআল্লাহ।");
        } else {
            Ui.h2(this, card, label + " বন্ধ রাখা আছে");
            Ui.body(this, card, "অ্যাপ লিমিট থেকে আপনি এটি পুরোপুরি বন্ধ রেখেছেন।");
        }

        LinearLayout q = Ui.card(this, col);
        Content.Ayah a = Content.randomAyah();
        Ui.arabic(this, q, a.arabic);
        Ui.body(this, q, a.meaning);
        Ui.muted(this, q, a.ref);

        Ui.button(this, col, "হোমে ফিরে যাই", true, v -> {
            Prefs.incDay(this, "returned", 1);
            Prefs.incTotal(this, "returned", 1);
            goHome();
        });

        final int left = Prefs.EMERGENCY_PER_DAY - Prefs.getDay(this, "emerg");
        if (left > 0) {
            final LinearLayout emBox = new LinearLayout(this);
            emBox.setOrientation(LinearLayout.VERTICAL);
            Ui.button(this, col, "সত্যিই জরুরি দরকার (আজ " + Fmt.bn(left) + " বার বাকি)", false, v -> {
                v.setVisibility(android.view.View.GONE);
                renderEmergency(emBox);
            });
            col.addView(emBox);
        } else {
            Ui.muted(this, col, "আজকের জরুরি পাস শেষ।");
        }
    }

    private void renderEmergency(LinearLayout box) {
        LinearLayout card = Ui.card(this, box);
        Ui.muted(this, card, "জরুরি পাসের দাম: ৩৩ বার ইস্তিগফার এবং ৬০ সেকেন্ড অপেক্ষা। তারপর ৩ মিনিট খুলবে।");
        final boolean[] dhikrDone = {false}, waitDone = {false};
        final Button open = Ui.button(this, box, "৩ মিনিটের জন্য খুলুন", false, v -> {
            Prefs.incDay(this, "emerg", 1);
            Guard.grantEmergency(pkg, 3);
            finish();
        });
        Ui.setEnabledLook(open, false);
        final TextView wait = Ui.muted(this, card, "");
        final int token = renderToken;
        final int[] secs = {60};
        h.post(new Runnable() {
            @Override public void run() {
                if (token != renderToken) return;
                if (secs[0] > 0) {
                    wait.setText("অপেক্ষা: " + Fmt.bn(secs[0]) + " সেকেন্ড");
                    secs[0]--;
                    h.postDelayed(this, 1000);
                } else {
                    wait.setText("অপেক্ষা শেষ");
                    waitDone[0] = true;
                    if (dhikrDone[0]) Ui.setEnabledLook(open, true);
                }
            }
        });
        dhikrCounter(card, Content.ISTIGHFAR, 33, () -> {
            dhikrDone[0] = true;
            if (waitDone[0]) Ui.setEnabledLook(open, true);
        });
    }

    // ---------------- shared ----------------

    private void renderNextTask() {
        TaskItem t = TaskStore.nextPending(this);
        if (t == null) return;
        LinearLayout card = Ui.card(this, col);
        Ui.muted(this, card, "এখন যা করার কথা");
        Ui.text(this, card, t.title, 18, Ui.INK, true);
        if (t.start >= 0) Ui.muted(this, card, Fmt.range(t));
        Ui.small(this, card, "টাস্কে যাই", v -> {
            startActivity(new Intent(this, TasksActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        h.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
