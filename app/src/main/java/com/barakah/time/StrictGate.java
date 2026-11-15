package com.barakah.time;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;

/**
 * Commitment device: in strict mode, anything that LOOSENS a limit needs a 60-second wait first.
 * Tightening is always instant. The urge usually passes during the wait.
 */
public final class StrictGate {
    private StrictGate() {}

    public static void loosen(Activity a, String what, Runnable action) {
        if (!Prefs.strict(a)) { action.run(); return; }
        final Handler h = new Handler(Looper.getMainLooper());
        final AlertDialog d = new AlertDialog.Builder(a)
                .setTitle("কঠোর মোড চালু")
                .setMessage(what + "\n\nসিদ্ধান্তটা নফসের না নিজের — ৬০ সেকেন্ড ভেবে দেখুন। এই সময়ে একবার আস্তাগফিরুল্লাহ বলুন।")
                .setNegativeButton("থাক, বাদ দিন", null)
                .setPositiveButton("৬০", (dlg, w) -> action.run())
                .create();
        d.setOnShowListener(x -> {
            final Button ok = d.getButton(AlertDialog.BUTTON_POSITIVE);
            ok.setEnabled(false);
            final int[] left = {60};
            h.post(new Runnable() {
                @Override public void run() {
                    if (!d.isShowing()) return;
                    if (left[0] > 0) {
                        ok.setText(Fmt.bn(left[0]) + " সেকেন্ড");
                        left[0]--;
                        h.postDelayed(this, 1000);
                    } else {
                        ok.setText("তবুও পরিবর্তন করুন");
                        ok.setEnabled(true);
                    }
                }
            });
        });
        d.show();
    }
}
