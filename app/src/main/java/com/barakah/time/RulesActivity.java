package com.barakah.time;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Per-app limits: minutes per clock hour + optional daily cap. */
public class RulesActivity extends Activity {

    private List<AppRule> rules;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        render();
    }

    private void save() { Prefs.saveRules(this, rules); }

    private void render() {
        rules = Prefs.rules(this);
        LinearLayout col = Ui.screen(this);
        Ui.title(this, col, "অ্যাপ লিমিট");
        Ui.muted(this, col, "প্রতি ঘণ্টায় কত মিনিট (যেমন ১০:০০–১০:৫৯ এর মধ্যে)। ০ = পুরো বন্ধ, ৬০ = ঘণ্টার লিমিট নেই। খোলার আগে সবসময় ইবাদত-গেট আসবে।");

        for (final AppRule r : rules) {
            LinearLayout c = Ui.card(this, col);
            LinearLayout head = Ui.row(this, c);
            Ui.grow(this, head, r.label, 18, Ui.INK, true);
            Switch sw = new Switch(this);
            sw.setChecked(r.enabled);
            sw.setOnCheckedChangeListener((btn, on) -> {
                if (on == r.enabled) return;
                if (on) { r.enabled = true; save(); render(); return; }
                btn.setChecked(true); // stays on until the (strict) confirmation completes
                StrictGate.loosen(this, r.label + "-এর লিমিট বন্ধ করবেন?", () -> { r.enabled = false; save(); render(); });
            });
            head.addView(sw);
            Ui.muted(this, c, r.pkg);

            stepper(c, "প্রতি ঘণ্টায়", r.perHour, 60, " মিনিট", 1, v -> { r.perHour = v; });
            stepper(c, "দিনে সর্বোচ্চ", r.perDay, 600, r.perDay == 0 ? " (সীমা নেই)" : " মিনিট", 10, v -> { r.perDay = v; });

            Ui.small(this, c, "তালিকা থেকে সরান", v -> StrictGate.loosen(this, r.label + " লিমিট তালিকা থেকে সরাবেন?",
                    () -> { rules.remove(r); save(); render(); }));
        }

        Ui.button(this, col, "আরেকটা অ্যাপ যোগ করুন", true, v -> pickApp());
        Ui.button(this, col, "ফিরে যান", false, v -> finish());
    }

    interface IntSetter { void set(int v); }

    /** "–  10 মিনিট  +" row. Increasing a limit (or 0 → unlimited daily) is a loosening. */
    private void stepper(LinearLayout parent, String label, final int value, final int max, String unit, final int step, final IntSetter setter) {
        LinearLayout row = Ui.row(this, parent);
        Ui.grow(this, row, label, 15, Ui.INK, false);
        Ui.small(this, row, "−", v -> {
            int nv = Math.max(0, value - step);
            // lowering a daily cap to 0 means "no cap", which is a loosening
            if (step == 10 && nv == 0 && value > 0) {
                StrictGate.loosen(this, "দৈনিক সীমা তুলে দেবেন?", () -> { setter.set(0); save(); render(); });
            } else { setter.set(nv); save(); render(); }
        });
        TextView t = new TextView(this);
        t.setText(Fmt.bn(value) + unit);
        t.setTextSize(16);
        t.setTextColor(Ui.GREEN);
        t.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 8), 0);
        row.addView(t);
        Ui.small(this, row, "+", v -> {
            final int nv = Math.min(max, value + step);
            boolean loosening = !(step == 10 && value == 0); // 0 → 10 daily is a tightening
            if (loosening) StrictGate.loosen(this, label + " লিমিট বাড়াবেন?", () -> { setter.set(nv); save(); render(); });
            else { setter.set(nv); save(); render(); }
        });
    }

    private void pickApp() {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> infos = pm.queryIntentActivities(main, 0);
        final List<String[]> apps = new ArrayList<>();
        for (ResolveInfo ri : infos) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            boolean exists = false;
            for (AppRule r : rules) if (r.pkg.equals(pkg)) exists = true;
            for (String[] a : apps) if (a[0].equals(pkg)) exists = true;
            if (!exists) apps.add(new String[]{pkg, ri.loadLabel(pm).toString()});
        }
        Collections.sort(apps, (a, b) -> a[1].compareToIgnoreCase(b[1]));
        String[] names = new String[apps.size()];
        for (int i = 0; i < apps.size(); i++) names[i] = apps.get(i)[1];
        new AlertDialog.Builder(this).setTitle("কোন অ্যাপ সীমিত করবেন?")
                .setItems(names, (d, which) -> {
                    String[] a = apps.get(which);
                    rules.add(new AppRule(a[0], a[1], 10, 0, true));
                    save();
                    render();
                }).show();
    }
}
