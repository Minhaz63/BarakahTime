package com.barakah.time;

import org.json.JSONException;
import org.json.JSONObject;

public class TaskItem {
    public long id;
    public String title;
    public String date;      // yyyyMMdd
    public int start = -1;   // minutes of day, -1 = no time
    public int end = -1;
    public boolean done;
    public boolean focus;    // block distracting apps between start and end

    public boolean hasRange() { return start >= 0 && end > start; }

    JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id).put("title", title).put("date", date).put("start", start)
             .put("end", end).put("done", done).put("focus", focus);
        } catch (JSONException ignored) {}
        return o;
    }

    static TaskItem fromJson(JSONObject o) {
        TaskItem t = new TaskItem();
        t.id = o.optLong("id");
        t.title = o.optString("title");
        t.date = o.optString("date");
        t.start = o.optInt("start", -1);
        t.end = o.optInt("end", -1);
        t.done = o.optBoolean("done");
        t.focus = o.optBoolean("focus");
        return t;
    }
}
