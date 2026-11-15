package com.barakah.time;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.InputType;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Daily tasks: add by voice (Bangla/English) or typing, tick off, delete. */
public class TasksActivity extends Activity {

    public static final String EXTRA_VOICE = "voice";
    private static final int REQ_VOICE = 42;

    private boolean bangla = true;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        bangla = Prefs.sp(this).getBoolean("voice_bn", true);
        render();
        if (b == null && getIntent().getBooleanExtra(EXTRA_VOICE, false)) startVoice();
    }

    private void render() {
        LinearLayout col = Ui.screen(this);
        Ui.title(this, col, "আজকের কাজ");
        Ui.muted(this, col, "সময় বললে (যেমন ৯টা থেকে ১১টা) ওই সময় ফেসবুক-ইউটিউব বন্ধ থাকবে, আর সময় হলে মনে করিয়ে দেবে।");

        LinearLayout add = Ui.card(this, col);
        Ui.button(this, add, "বলে যোগ করুন (" + (bangla ? "বাংলা" : "English") + ")", true, v -> startVoice());
        LinearLayout lr = Ui.row(this, add);
        Ui.small(this, lr, bangla ? "English-এ বলব" : "বাংলায় বলব", v -> {
            bangla = !bangla;
            Prefs.sp(this).edit().putBoolean("voice_bn", bangla).apply();
            render();
        });
        Ui.muted(this, add, "উদাহরণ: \"সকাল ৯টা থেকে ১১টা অ্যাসাইনমেন্ট, তারপর বিকেল ৪টা থেকে ৫টা জিম\"");
        final EditText input = new EditText(this);
        input.setHint("অথবা লিখুন…");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        add.addView(input);
        Ui.small(this, add, "যোগ করুন", v -> {
            addFromText(input.getText().toString());
            input.setText("");
        });

        List<TaskItem> list = TaskStore.today(this);
        if (list.isEmpty()) Ui.muted(this, col, "এখনো কোনো টাস্ক নেই।");
        for (final TaskItem t : list) {
            LinearLayout c = Ui.card(this, col);
            LinearLayout r = Ui.row(this, c);
            CheckBox cb = new CheckBox(this);
            cb.setChecked(t.done);
            cb.setOnCheckedChangeListener((btn, checked) -> {
                if (checked == t.done) return;
                if (!checked || TaskStore.activeFocus(this) != t) { setDone(t, checked); return; }
                // finishing a running focus task also ends its block, so strict mode asks for a pause first
                render();
                StrictGate.loosen(this, "\"" + t.title + "\" কি সত্যিই শেষ? টিক দিলে ফোকাস ব্লক উঠে যাবে।",
                        () -> setDone(t, true));
            });
            r.addView(cb);
            Ui.grow(this, r, t.title, 17, t.done ? Ui.MUTED : Ui.INK, !t.done);
            Ui.small(this, r, "মুছুন", v -> confirmDelete(t));
            String meta = Fmt.range(t);
            if (t.focus && t.hasRange()) meta += "  ·  ফোকাস ব্লক";
            if (!Prefs.today().equals(t.date)) meta += (meta.isEmpty() ? "" : "  ·  ") + "আগের দিনের বাকি";
            if (!meta.isEmpty()) Ui.muted(this, c, meta);
            if (t.hasRange()) {
                Ui.small(this, c, t.focus ? "ফোকাস ব্লক বন্ধ করুন" : "ফোকাস ব্লক চালু করুন", v -> {
                    if (t.focus) {
                        StrictGate.loosen(this, "\"" + t.title + "\"-এর ফোকাস ব্লক বন্ধ করবেন?", () -> {
                            t.focus = false; TaskStore.save(this); render();
                        });
                    } else {
                        t.focus = true; TaskStore.save(this); render();
                    }
                });
            }
        }
        Ui.spacer(this, col, 8);
        Ui.button(this, col, "হোমে ফিরুন", false, v -> finish());
    }

    private void setDone(TaskItem t, boolean done) {
        t.done = done;
        TaskStore.save(this);
        if (done) {
            Reminders.cancel(this, t);
            Toast.makeText(this, "আলহামদুলিল্লাহ! একটা কাজ শেষ।", Toast.LENGTH_SHORT).show();
        } else {
            Reminders.schedule(this, t);
        }
        render();
    }

    private void confirmDelete(TaskItem t) {
        Runnable del = () -> { TaskStore.delete(this, t); render(); };
        if (t.focus && t.hasRange() && TaskStore.activeFocus(this) == t) {
            StrictGate.loosen(this, "চলমান ফোকাস টাস্ক মুছে ফেলবেন?", del);
        } else {
            new AlertDialog.Builder(this).setMessage("\"" + t.title + "\" মুছে ফেলবেন?")
                    .setPositiveButton("মুছুন", (d, w) -> del.run())
                    .setNegativeButton("না", null).show();
        }
    }

    private void startVoice() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, bangla ? "bn-BD" : "en-US")
                .putExtra(RecognizerIntent.EXTRA_PROMPT, bangla
                        ? "কাজ ও সময় বলুন — যেমন: সকাল ৯টা থেকে ১১টা পড়াশোনা"
                        : "Say tasks and times — e.g. study from 9 to 11");
        try {
            startActivityForResult(i, REQ_VOICE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "ভয়েস ইনপুটের জন্য Google অ্যাপ (Speech Services) লাগবে। আপাতত লিখে যোগ করুন।", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_VOICE || res != RESULT_OK || data == null) return;
        ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (results == null || results.isEmpty()) return;
        addFromText(results.get(0));
    }

    private void addFromText(String text) {
        List<TaskItem> tasks = TaskParser.parseMany(text);
        if (tasks.isEmpty()) {
            Toast.makeText(this, "কিছু বুঝতে পারিনি, আবার বলুন", Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (TaskItem t : tasks) {
            TaskStore.add(this, t);
            sb.append("• ").append(t.title);
            if (t.start >= 0) sb.append(" — ").append(Fmt.range(t));
            if (t.focus) sb.append(" (ফোকাস)");
            sb.append("\n");
        }
        render();
        new AlertDialog.Builder(this)
                .setTitle("যোগ হলো")
                .setMessage(sb.toString().trim() + "\n\nভুল হলে মুছে আবার বলুন।")
                .setPositiveButton("ঠিক আছে", null).show();
    }
}
