package com.gathin.tvhome;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

/**
 * The Home media card: shown while some app (Spotify, YouTube...) has a playing or paused media session, with
 * its artwork, title, artist and previous / play-pause / next. Needs MediaListener enabled; without it the
 * card simply stays hidden.
 */
final class MediaCard {
    private final Context context;
    private final Handler handler;
    private final View card;
    private final ImageView art, playPause;
    private final TextView title, artist, app, elapsed, duration;
    private final ProgressBar progress;
    private final float density;
    private final ComponentName listener;
    private MediaSessionManager msm;
    private MediaController current;
    private Bitmap lastArt;
    private String lastArtPackage;
    /** The app rows are scrolled up past the dock: keep the card out of the way even while media plays. */
    private boolean scrolledAway;

    private final MediaSessionManager.OnActiveSessionsChangedListener sessionsChanged = this::pick;
    private final MediaController.Callback callback = new MediaController.Callback() {
        @Override public void onPlaybackStateChanged(PlaybackState state) { pickAll(); }
        @Override public void onMetadataChanged(MediaMetadata metadata) { show(); }
        @Override public void onSessionDestroyed() { pickAll(); }
    };

    private final View mini, topButtons;
    private final ImageView miniArt, miniPlay;
    private final TextView miniTitle;
    /** How far the Wi-Fi and Settings buttons move up while the mini player shows under them. */
    static final int TOP_LIFT_DP = 22;

    MediaCard(Context context, View card, View mini, View topButtons, Handler handler) {
        this.context = context;
        this.card = card;
        this.mini = mini;
        this.topButtons = topButtons;
        miniArt = mini.findViewById(R.id.miniArt);
        miniPlay = mini.findViewById(R.id.miniPlay);
        miniTitle = mini.findViewById(R.id.miniTitle);
        // The XML clipToOutline attribute only exists from Android 12; clip the artwork to its rounded background here.
        miniArt.setClipToOutline(true);
        this.handler = handler;
        art = card.findViewById(R.id.mediaArt);
        art.setClipToOutline(true);
        title = card.findViewById(R.id.mediaTitle);
        artist = card.findViewById(R.id.mediaArtist);
        app = card.findViewById(R.id.mediaApp);
        elapsed = card.findViewById(R.id.mediaElapsed);
        duration = card.findViewById(R.id.mediaDuration);
        progress = card.findViewById(R.id.mediaProgress);
        density = context.getResources().getDisplayMetrics().density;
        playPause = card.findViewById(R.id.mediaPlay);
        listener = new ComponentName(context, MediaListener.class);
        card.findViewById(R.id.mediaPrev).setOnClickListener(v -> {
            if (current != null) current.getTransportControls().skipToPrevious();
        });
        card.findViewById(R.id.mediaNext).setOnClickListener(v -> {
            if (current != null) current.getTransportControls().skipToNext();
        });
        View.OnClickListener toggle = v -> {
            if (current == null) return;
            if (isPlaying(current)) current.getTransportControls().pause();
            else current.getTransportControls().play();
        };
        playPause.setOnClickListener(toggle);
        miniPlay.setOnClickListener(toggle);
    }

    void start() {
        msm = context.getSystemService(MediaSessionManager.class);
        try {
            msm.addOnActiveSessionsChangedListener(sessionsChanged, listener, handler);
            pickAll();
        } catch (SecurityException e) {
            msm = null;   // listener not enabled
            card.setVisibility(View.GONE);
        }
    }

    void stop() {
        handler.removeCallbacks(tick);
        if (msm != null) msm.removeOnActiveSessionsChangedListener(sessionsChanged);
        setCurrent(null);
        msm = null;
    }

    private void pickAll() {
        if (msm == null) return;
        try {
            pick(msm.getActiveSessions(listener));
        } catch (SecurityException e) {
            setCurrent(null);
        }
    }

    /** A playing session first, else a paused one; none of either hides the card. */
    private void pick(List<MediaController> sessions) {
        MediaController playing = null, paused = null;
        if (sessions != null) for (MediaController c : sessions) {
            PlaybackState s = c.getPlaybackState();
            if (s == null) continue;
            if (isPlaying(c) && playing == null) playing = c;
            else if (s.getState() == PlaybackState.STATE_PAUSED && paused == null) paused = c;
        }
        setCurrent(playing != null ? playing : paused);
    }

    private void setCurrent(MediaController c) {
        if (current != null && (c == null || !current.getSessionToken().equals(c.getSessionToken()))) {
            current.unregisterCallback(callback);
            current = null;
        }
        if (c != null && current == null) {
            current = c;
            current.registerCallback(callback, handler);
        }
        show();
    }

    private void show() {
        handler.removeCallbacks(tick);
        if (current == null) {
            if (card.hasFocus() || mini.hasFocus()) card.getRootView().findViewById(R.id.grid).requestFocus();
            card.setVisibility(View.GONE);
            showMini(false);
            return;
        }
        MediaMetadata m = current.getMetadata();
        String t = m == null ? null : m.getString(MediaMetadata.METADATA_KEY_TITLE);
        String a = m == null ? null : firstNonEmpty(m.getString(MediaMetadata.METADATA_KEY_ARTIST),
                m.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST), m.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE));
        String label = appLabel();
        miniTitle.setText(a != null && t != null ? t + " · " + a : t != null ? t : label);
        title.setText(t != null ? t : label);
        artist.setText(a != null ? a : "");
        artist.setVisibility(a != null ? View.VISIBLE : View.GONE);
        app.setText(label);
        // On a track change Spotify sends the new title first and the artwork a moment later: keep the last
        // cover of this app until then instead of flashing its green logo and resetting the tint.
        Bitmap b = m == null ? null : firstBitmap(m);
        if (b == null && current.getPackageName().equals(lastArtPackage)) b = lastArt;
        if (b != null && b == lastArt && current.getPackageName().equals(lastArtPackage)) {
            // unchanged: nothing to redraw
        } else if (b != null) {
            lastArt = b;
            lastArtPackage = current.getPackageName();
            tint(b);
            art.setImageBitmap(b);
            miniArt.setImageBitmap(b);
        } else {
            tint(null);
            try {
                art.setImageDrawable(context.getPackageManager().getApplicationIcon(current.getPackageName()));
            } catch (PackageManager.NameNotFoundException e) {
                art.setImageDrawable(null);
            }
            miniArt.setImageDrawable(art.getDrawable());
        }
        int icon = isPlaying(current) ? R.drawable.ic_pause : R.drawable.ic_play;
        playPause.setImageResource(icon);
        miniPlay.setImageResource(icon);
        if (!scrolledAway) {
            card.animate().cancel();
            card.setAlpha(1f);
            card.setVisibility(View.VISIBLE);
        }
        showMini(scrolledAway);
        tick.run();
    }

    /** Once a second while shown: the progress bar and times; hidden when the app gives no duration. */
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (current == null) return;
            MediaMetadata m = current.getMetadata();
            PlaybackState s = current.getPlaybackState();
            long total = m == null ? 0 : m.getLong(MediaMetadata.METADATA_KEY_DURATION);
            boolean known = total > 0 && s != null && s.getPosition() >= 0;
            int vis = known ? View.VISIBLE : View.GONE;
            progress.setVisibility(vis);
            ((View) elapsed.getParent()).setVisibility(vis);
            if (!known) return;
            long pos = s.getPosition();
            if (s.getState() == PlaybackState.STATE_PLAYING)
                pos += (long) ((SystemClock.elapsedRealtime() - s.getLastPositionUpdateTime()) * s.getPlaybackSpeed());
            pos = Math.max(0, Math.min(total, pos));
            progress.setProgress((int) (pos * 1000 / total));
            elapsed.setText(time(pos));
            duration.setText(time(total));
            if (s.getState() == PlaybackState.STATE_PLAYING) handler.postDelayed(this, 1000);
        }
    };

    static String time(long ms) {
        long sec = ms / 1000;
        return sec >= 3600 ? String.format(Locale.ROOT, "%d:%02d:%02d", sec / 3600, sec / 60 % 60, sec % 60)
                : String.format(Locale.ROOT, "%d:%02d", sec / 60, sec % 60);
    }

    /** The card's glass takes the artwork's average colour, darkened so white text stays readable. */
    private void tint(Bitmap b) {
        if (b == null) {
            card.setBackgroundResource(R.drawable.media_card_bg);
            mini.setBackgroundResource(R.drawable.mini_media_bg);
            return;
        }
        int c = Bitmap.createScaledBitmap(b, 1, 1, true).getPixel(0, 0);
        float[] hsv = new float[3];
        Color.colorToHSV(c, hsv);
        hsv[1] = Math.min(1f, hsv[1] * 1.2f);
        hsv[2] = Math.min(hsv[2], 0.55f);
        int deep = Color.HSVToColor(hsv);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {
                (0xE6 << 24) | (deep & 0xFFFFFF), (0xB3 << 24) | (darker(deep) & 0xFFFFFF)});
        g.setCornerRadius(28 * density);
        g.setStroke(Math.round(density), 0x59FFFFFF);
        card.setBackground(g);
        GradientDrawable m = (GradientDrawable) g.getConstantState().newDrawable().mutate();
        m.setCornerRadius(22 * density);
        mini.setBackground(m);
    }

    private static int darker(int c) {
        float[] hsv = new float[3];
        Color.colorToHSV(c, hsv);
        hsv[2] *= 0.6f;
        return Color.HSVToColor(hsv);
    }

    void setScrolledAway(boolean away) {
        if (away == scrolledAway) return;
        scrolledAway = away;
        if (current == null) return;
        card.animate().cancel();
        if (away) {
            card.animate().alpha(0f).setDuration(150).withEndAction(() -> card.setVisibility(View.INVISIBLE));
        } else {
            card.setVisibility(View.VISIBLE);
            card.animate().alpha(1f).setDuration(200);
        }
        showMini(away);
    }

    /** The mini player under the top buttons, which lift by TOP_LIFT_DP while it shows. */
    private void showMini(boolean on) {
        boolean shown = mini.getVisibility() == View.VISIBLE && mini.getAlpha() > 0f;
        if (on == shown) return;
        mini.animate().cancel();
        topButtons.animate().translationY(on ? -TOP_LIFT_DP * density : 0f).setDuration(200);
        if (on) {
            mini.setAlpha(0f);
            mini.setVisibility(View.VISIBLE);
            mini.animate().alpha(1f).setDuration(200);
        } else {
            mini.animate().alpha(0f).setDuration(150).withEndAction(() -> mini.setVisibility(View.GONE));
        }
    }

    private String appLabel() {
        try {
            PackageManager pm = context.getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(current.getPackageName(), 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    private static boolean isPlaying(MediaController c) {
        PlaybackState s = c.getPlaybackState();
        return s != null && (s.getState() == PlaybackState.STATE_PLAYING || s.getState() == PlaybackState.STATE_BUFFERING);
    }

    private static Bitmap firstBitmap(MediaMetadata m) {
        // Not METADATA_KEY_DISPLAY_ICON: Spotify puts its logo there while the cover is still loading.
        for (String k : new String[] {MediaMetadata.METADATA_KEY_ALBUM_ART, MediaMetadata.METADATA_KEY_ART}) {
            Bitmap b = m.getBitmap(k);
            if (b != null) return b;
        }
        return null;
    }

    private static String firstNonEmpty(String... s) {
        for (String x : s) if (x != null && !x.isEmpty()) return x;
        return null;
    }
}
