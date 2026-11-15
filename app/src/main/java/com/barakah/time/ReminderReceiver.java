package com.barakah.time;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(i.getAction())) {
            Reminders.scheduleAll(c);
            return;
        }
        long id = i.getLongExtra("id", -1);
        boolean end = i.getBooleanExtra("end", false);
        for (TaskItem t : TaskStore.all(c)) {
            if (t.id != id || t.done) continue;
            int nid = (int) (id % 100000);
            if (!end) {
                Reminders.notify(c, nid, "এখন সময়: " + t.title,
                        Fmt.range(t) + (t.focus ? "\nফোকাস মোড চালু — বিভ্রান্তিকর অ্যাপগুলো এই সময় বন্ধ থাকবে। বিসমিল্লাহ বলে শুরু করুন।" : ""));
            } else {
                Reminders.notify(c, nid, "সময় শেষ: " + t.title,
                        "কাজটা কি শেষ হয়েছে? হলে টাস্ক তালিকায় টিক দিন। আলহামদুলিল্লাহ!");
            }
        }
    }
}
