package com.barakah.time;

import android.content.Context;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

/**
 * Recognises short-video screens by their layout fingerprint (view ids / selected tab),
 * never by reading what the video is about. Apps change their layouts over time,
 * so these lists may need updating after a big app update.
 */
public final class ShortsDetector {
    private ShortsDetector() {}

    public static final String YT = "com.google.android.youtube";
    public static final String FB = "com.facebook.katana";
    public static final String FB_LITE = "com.facebook.lite";
    public static final String IG = "com.instagram.android";
    public static final String[] TIKTOK = {"com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.ss.android.ugc.aweme", "com.zhiliaoapp.musically.go"};

    private static final String[] YT_IDS = {
            "reel_recycler", "reel_player_page_container", "reel_watch_player",
            "reel_watch_fragment_root", "shorts_player_container", "reel_time_bar"};
    private static final String[] IG_IDS = {
            "clips_viewer_view_pager", "clips_viewer_container", "clips_video_container", "clips_viewer_fragment"};
    private static final String[] FB_IDS = {"reels_viewer", "reel_viewer_pager", "video_home_reels"};

    public static boolean isTikTok(String pkg) {
        for (String t : TIKTOK) if (t.equals(pkg)) return true;
        return false;
    }

    public static boolean watches(Context c, String pkg) {
        if (YT.equals(pkg)) return Prefs.flag(c, Prefs.BLOCK_YT_SHORTS);
        if (FB.equals(pkg) || FB_LITE.equals(pkg)) return Prefs.flag(c, Prefs.BLOCK_FB_REELS);
        if (IG.equals(pkg)) return Prefs.flag(c, Prefs.BLOCK_IG_REELS);
        return false;
    }

    public static boolean isShortsScreen(String pkg, AccessibilityNodeInfo root) {
        if (root == null) return false;
        if (YT.equals(pkg)) {
            return hasVisibleId(root, pkg, YT_IDS) || selectedTab(root, "Shorts", "শর্টস");
        }
        if (IG.equals(pkg)) {
            return hasVisibleId(root, pkg, IG_IDS) || selectedTab(root, "Reels", "রিলস");
        }
        if (FB.equals(pkg) || FB_LITE.equals(pkg)) {
            return hasVisibleId(root, pkg, FB_IDS) || selectedTab(root, "Reels", "রিলস", "Reel");
        }
        return false;
    }

    private static boolean hasVisibleId(AccessibilityNodeInfo root, String pkg, String[] ids) {
        for (String id : ids) {
            List<AccessibilityNodeInfo> l = root.findAccessibilityNodeInfosByViewId(pkg + ":id/" + id);
            if (l == null) continue;
            for (AccessibilityNodeInfo n : l) {
                if (n != null && n.isVisibleToUser()) return true;
            }
        }
        return false;
    }

    /** A bottom-bar tab whose label is e.g. "Shorts" and that is currently selected. */
    private static boolean selectedTab(AccessibilityNodeInfo root, String... labels) {
        for (String label : labels) {
            List<AccessibilityNodeInfo> l = root.findAccessibilityNodeInfosByText(label);
            if (l == null) continue;
            for (AccessibilityNodeInfo n : l) {
                if (n == null || !n.isVisibleToUser()) continue;
                CharSequence d = n.getContentDescription();
                CharSequence t = n.getText();
                String s = ((d != null ? d : "") + " " + (t != null ? t : "")).trim();
                boolean shortLabel = s.length() <= label.length() + 24; // a tab label, not a sentence
                if (shortLabel && (n.isSelected() || (n.getParent() != null && n.getParent().isSelected()))) return true;
            }
        }
        return false;
    }
}
