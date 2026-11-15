package com.barakah.time;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

/**
 * The guard. Watches which app is in front (once a second), counts time in limited apps,
 * opens the gate, enforces hourly limits and focus windows, and backs out of Shorts/Reels.
 */
public class GuardService extends AccessibilityService {

    static volatile boolean running;

    private final Handler h = new Handler(Looper.getMainLooper());
    private final Set<String> ignore = new HashSet<>();
    private String current;
    private long lastGateAt;
    private String lastGatePkg;
    private long lastShortsCheck, lastShortsAction, lastToast;
    private int distractSecs;
    private long lastDistractAt, lastReminderAt;
    private int ticks;

    @Override
    protected void onServiceConnected() {
        running = true;
        ignore.add("com.android.systemui");
        try {
            InputMethodManager imm = getSystemService(InputMethodManager.class);
            for (InputMethodInfo i : imm.getInputMethodList()) ignore.add(i.getPackageName());
        } catch (Exception ignored) {}
        Reminders.ensureChannel(this);
        Reminders.scheduleAll(this);
        h.removeCallbacks(tick);
        h.post(tick);
    }

    @Override
    public void onDestroy() {
        running = false;
        h.removeCallbacks(tick);
        Usage.flush(this);
        super.onDestroy();
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e == null || e.getPackageName() == null) return;
        String pkg = e.getPackageName().toString();
        if (e.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && !ignore.contains(pkg)) {
            onForeground(pkg);
        }
        if (ShortsDetector.watches(this, pkg)) {
            long now = System.currentTimeMillis();
            if (now - lastShortsCheck > 350) {
                lastShortsCheck = now;
                checkShorts(pkg);
            }
        }
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            try { onTick(); } catch (Exception ignored) {}
            h.postDelayed(this, 1000);
        }
    };

    private String activePackage() {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null && root.getPackageName() != null) return root.getPackageName().toString();
        } catch (Exception ignored) {}
        return null;
    }

    private void onTick() {
        ticks++;
        PowerManager pm = getSystemService(PowerManager.class);
        boolean screenOn = pm != null && pm.isInteractive();
        String pkg = activePackage();
        if (pkg != null && !ignore.contains(pkg) && !pkg.equals(current)) onForeground(pkg);

        long now = System.currentTimeMillis();
        if (screenOn && current != null) {
            AppRule r = Guard.rule(this, current);
            if (r != null) {
                Usage.add(this, current, 1);
                distractSecs++;
                lastDistractAt = now;
                enforce(current, r);
            } else if (now - lastDistractAt > 120_000) {
                distractSecs = 0; // two quiet minutes reset the streak
            }
            if (ShortsDetector.isTikTok(current) && Prefs.flag(this, Prefs.BLOCK_TIKTOK)) {
                blockShortForm(true);
            }
        }

        int limit = Prefs.reminderMinutes(this) * 60;
        if (distractSecs >= limit && now - lastReminderAt > 10 * 60_000L) {
            lastReminderAt = now;
            TaskItem next = TaskStore.nextPending(this);
            String text = next != null
                    ? "পরের কাজ: " + next.title + (next.start >= 0 ? " (" + Fmt.range(next) + ")" : "")
                    : "একটু বিরতি নিন। ৩৩ বার সুবহানাল্লাহ বলে উঠে দাঁড়ান?";
            Reminders.notify(this, 7001, "টানা " + Fmt.bn(distractSecs / 60) + " মিনিট স্ক্রল হলো", text);
        }

        if (ticks % 15 == 0) Usage.flush(this);
    }

    private void onForeground(String pkg) {
        current = pkg;
        AppRule r = Guard.rule(this, pkg);
        if (r != null) enforce(pkg, r);
        else if (ShortsDetector.isTikTok(pkg) && Prefs.flag(this, Prefs.BLOCK_TIKTOK)) blockShortForm(true);
    }

    private void enforce(String pkg, AppRule r) {
        if (Guard.hasEmergency(pkg)) return;
        String reason = Guard.blockReason(this, r);
        if (reason != null) {
            Guard.endSession(pkg);
            openGate(pkg, GateActivity.MODE_BLOCKED, reason);
        } else if (!Guard.hasSession(pkg)) {
            openGate(pkg, GateActivity.MODE_CHALLENGE, null);
        }
    }

    private void openGate(String pkg, String mode, String reason) {
        long now = System.currentTimeMillis();
        if (pkg.equals(lastGatePkg) && now - lastGateAt < 2500) return;
        lastGateAt = now;
        lastGatePkg = pkg;
        Intent i = new Intent(this, GateActivity.class)
                .putExtra(GateActivity.EXTRA_PKG, pkg)
                .putExtra(GateActivity.EXTRA_MODE, mode)
                .putExtra(GateActivity.EXTRA_REASON, reason)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(i);
    }

    private void checkShorts(String pkg) {
        AccessibilityNodeInfo root;
        try { root = getRootInActiveWindow(); } catch (Exception e) { return; }
        if (root == null || root.getPackageName() == null || !pkg.equals(root.getPackageName().toString())) return;
        if (ShortsDetector.isShortsScreen(pkg, root)) blockShortForm(false);
    }

    private void blockShortForm(boolean goHome) {
        long now = System.currentTimeMillis();
        if (now - lastShortsAction < 800) return;
        lastShortsAction = now;
        performGlobalAction(goHome ? GLOBAL_ACTION_HOME : GLOBAL_ACTION_BACK);
        Prefs.incDay(this, "shorts", 1);
        if (now - lastToast > 4000) {
            lastToast = now;
            Toast.makeText(this, Content.nudge(), Toast.LENGTH_SHORT).show();
        }
    }
}
