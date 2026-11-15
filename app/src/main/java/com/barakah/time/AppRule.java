package com.barakah.time;

import org.json.JSONException;
import org.json.JSONObject;

/** A limit for one distracting app. perHour: minutes allowed per clock hour (60 = no hourly limit, 0 = fully blocked). */
public class AppRule {
    public String pkg;
    public String label;
    public int perHour;
    public int perDay; // 0 = no daily cap
    public boolean enabled;

    public AppRule(String pkg, String label, int perHour, int perDay, boolean enabled) {
        this.pkg = pkg;
        this.label = label;
        this.perHour = perHour;
        this.perDay = perDay;
        this.enabled = enabled;
    }

    JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("pkg", pkg).put("label", label).put("perHour", perHour)
             .put("perDay", perDay).put("enabled", enabled);
        } catch (JSONException ignored) {}
        return o;
    }

    static AppRule fromJson(JSONObject o) {
        return new AppRule(o.optString("pkg"), o.optString("label"), o.optInt("perHour", 10),
                o.optInt("perDay", 0), o.optBoolean("enabled", true));
    }
}
