package com.gathin.tvhome;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/** The control center's sleep timer: each press steps Off, 30, 60, 90 minutes, Off. The alarm lands here. */
public final class SleepTimer extends BroadcastReceiver {
    static final int[] STEPS = {0, 30, 60, 90};
    private static final String PREFS = "sleep_timer", END = "end";

    @Override
    public void onReceive(Context context, Intent intent) {
        prefs(context).edit().remove(END).apply();
        SleepService.sleepNow(context);
    }

    /** Minutes left, rounded up; 0 when off. */
    static int minutesLeft(Context context) {
        long left = prefs(context).getLong(END, 0) - System.currentTimeMillis();
        return left <= 0 ? 0 : (int) ((left + 59_999) / 60_000);
    }

    /** The step after the one running now; returns the new minutes (0 is off). */
    static int next(Context context) {
        int minutes = nextStep(minutesLeft(context));
        AlarmManager am = context.getSystemService(AlarmManager.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, new Intent(context, SleepTimer.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(pi);
        if (minutes == 0) {
            prefs(context).edit().remove(END).apply();
        } else {
            long end = System.currentTimeMillis() + minutes * 60_000L;
            prefs(context).edit().putLong(END, end).apply();
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, pi);
        }
        return minutes;
    }

    /** A running timer counts as the step at or below what is left, so 25 minutes left steps to 60. */
    static int nextStep(int left) {
        for (int i = STEPS.length - 1; i >= 0; i--) {
            if (left > 0 && left <= STEPS[i] && (i == 0 || left > STEPS[i - 1])) return STEPS[(i + 1) % STEPS.length];
        }
        return STEPS[1];
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
