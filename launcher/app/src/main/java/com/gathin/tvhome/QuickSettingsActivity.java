package com.gathin.tvhome;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.text.format.Formatter;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The control center the Settings pill or the remote's Settings button opens (settings_key_panel): liquid glass
 * modules of mixed sizes, as on iOS, over Home or whatever app is playing. Translucent and in a
 * task of its own, so the app behind keeps running and Back returns to it. Pressing Settings again
 * closes the panel.
 */
public final class QuickSettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_quick_settings);
        findViewById(R.id.scrim).setOnClickListener(v -> finish());
        GridLayout grid = findViewById(R.id.rows);
        // iOS-style modules on a 5 x 3 grid: connectivity 2x2, Sleep tall, timer and storage wide, the rest square.
        LinearLayout radios = (LinearLayout) getLayoutInflater().inflate(R.layout.item_cc_block, grid, false);
        GridLayout radioGrid = radios.findViewById(R.id.radios);
        View wifi = addRound(radioGrid, R.drawable.ic_wifi, getString(R.string.quick_wifi), wifiName() != null, SystemSettings.intent(this, Settings.ACTION_WIFI_SETTINGS));
        addRound(radioGrid, R.drawable.ic_bluetooth, getString(R.string.quick_bluetooth), bluetoothOn(),
                SystemSettings.intent(this, Settings.ACTION_BLUETOOTH_SETTINGS));
        addRound(radioGrid, R.drawable.ic_sound, getString(R.string.quick_sound), false,
                SystemSettings.intent(this, Settings.ACTION_SOUND_SETTINGS));
        addRound(radioGrid, R.drawable.ic_picture, getString(R.string.quick_picture), false,
                SystemSettings.intent(this, Settings.ACTION_DISPLAY_SETTINGS));
        place(grid, radios, 0, 0, 2, 2);

        place(grid, tile(R.layout.item_cc_tile, R.drawable.ic_sleep, getString(R.string.quick_sleep), null, null), 0, 2, 2, 1)
                .setOnClickListener(v -> {
                    finish();
                    SleepService.sleepNow(this);
                });
        // Stays open while pressed again: Off, 30, 60, 90 minutes, Off.
        View timer = place(grid, tile(R.layout.item_cc_wide, R.drawable.ic_timer, getString(R.string.quick_sleep_timer),
                timerStatus(), null), 0, 3, 1, 2);
        timer.setOnClickListener(v -> {
            SleepTimer.next(this);
            ((TextView) timer.findViewById(R.id.status)).setText(timerStatus());
        });
        StatFs data = new StatFs(Environment.getDataDirectory().getPath());
        place(grid, tile(R.layout.item_cc_wide, R.drawable.ic_storage, getString(R.string.quick_storage),
                getString(R.string.storage_free, Formatter.formatShortFileSize(this, data.getAvailableBytes())),
                SystemSettings.intent(this, Settings.ACTION_INTERNAL_STORAGE_SETTINGS)), 1, 3, 1, 2);

        place(grid, tile(R.layout.item_cc_tile, R.drawable.ic_apps, getString(R.string.quick_apps), null,
                SystemSettings.intent(this, Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)), 2, 0, 1, 1);
        place(grid, tile(R.layout.item_cc_tile, R.drawable.ic_settings, getString(R.string.quick_system), null,
                SystemSettings.intent(this, Settings.ACTION_SETTINGS)), 2, 1, 1, 1);
        // Through Home, so it rebuilds with the new choices when Launcher settings closes.
        place(grid, tile(R.layout.item_cc_tile, R.drawable.ic_home_settings, getString(R.string.quick_launcher), null,
                new Intent(this, MainActivity.class).setAction(MainActivity.ACTION_LAUNCHER_SETTINGS)), 2, 2, 1, 1);
        place(grid, powerTile(R.drawable.ic_restart, R.string.quick_restart, "svc power reboot"), 2, 3, 1, 1);
        place(grid, powerTile(R.drawable.ic_power, R.string.quick_power_off, "svc power shutdown"), 2, 4, 1, 1);
        wifi.requestFocus();
    }

    /** Settings pressed again while the panel is up. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        finish();
    }

    /** Anything else taking the screen (Home, an app, a screensaver) closes the panel. */
    @Override
    protected void onPause() {
        super.onPause();
        finish();
        overridePendingTransition(0, 0);
    }

    /**
     * Restart and power off need root (an app cannot hold REBOOT), so they go through su; Magisk asks once on the
     * TV. The first press only arms the tile, a second press within a few seconds runs it.
     */
    private View powerTile(int icon, int title, String command) {
        View row = tile(R.layout.item_cc_tile, icon, getString(title), null, null);
        TextView hint = row.findViewById(R.id.status);
        row.setOnClickListener(v -> {
            Long armed = (Long) row.getTag();
            if (armed == null || System.currentTimeMillis() - armed > 4000) {
                row.setTag(System.currentTimeMillis());
                hint.setText(R.string.press_again);
                hint.setVisibility(View.VISIBLE);
                return;
            }
            new Thread(() -> {
                boolean ok;
                try {
                    ok = Runtime.getRuntime().exec(new String[] {"su", "-c", command}).waitFor() == 0;
                } catch (Exception e) {
                    ok = false;
                }
                if (!ok) runOnUiThread(() -> Toast.makeText(this, R.string.needs_root, Toast.LENGTH_LONG).show());
            }, "power").start();
        });
        return row;
    }

    private String timerStatus() {
        int left = SleepTimer.minutesLeft(this);
        return left == 0 ? getString(R.string.sleep_timer_off) : getString(R.string.sleep_timer_left, left);
    }

    /** The connected network's name, or null (no Wi-Fi, or no location grant to read it). */
    private String wifiName() {
        WifiManager wm = getApplicationContext().getSystemService(WifiManager.class);
        WifiInfo info = wm == null ? null : wm.getConnectionInfo();
        String ssid = info == null ? null : info.getSSID();
        if (ssid == null || ssid.equals(WifiManager.UNKNOWN_SSID) || ssid.equals("<unknown ssid>")) return null;
        return ssid.replaceAll("^\"|\"$", "");
    }

    /** One module cell is UNIT_DP square; spans add the GAP_DP between cells they cover. */
    private static final int UNIT_DP = 92, GAP_DP = 8;

    private View place(GridLayout grid, View v, int row, int col, int rowSpan, int colSpan) {
        float d = getResources().getDisplayMetrics().density;
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams(GridLayout.spec(row, rowSpan), GridLayout.spec(col, colSpan));
        lp.width = Math.round((UNIT_DP * colSpan + GAP_DP * (colSpan - 1)) * d);
        lp.height = Math.round((UNIT_DP * rowSpan + GAP_DP * (rowSpan - 1)) * d);
        int m = Math.round(GAP_DP / 2f * d);
        lp.setMargins(m, m, m, m);
        grid.addView(v, lp);
        return v;
    }

    /** A module that opens target and closes the panel; with a null target the caller sets the click. */
    private View tile(int layout, int icon, String title, String status, Intent target) {
        View row = getLayoutInflater().inflate(layout, null, false);
        ((ImageView) row.findViewById(R.id.icon)).setImageResource(icon);
        ((TextView) row.findViewById(R.id.title)).setText(title);
        if (status != null) {
            TextView hint = row.findViewById(R.id.status);
            hint.setText(status);
            hint.setVisibility(View.VISIBLE);
        }
        if (target != null) row.setOnClickListener(v -> open(target));
        return row;
    }

    /** A round button in the connectivity module; on is drawn iOS blue (a connected radio). */
    private View addRound(GridLayout radios, int icon, String title, boolean on, Intent target) {
        View b = getLayoutInflater().inflate(R.layout.item_cc_round, radios, false);
        ImageView i = b.findViewById(R.id.icon);
        i.setImageResource(icon);
        if (on) i.setBackgroundResource(R.drawable.cc_round_on);
        ((TextView) b.findViewById(R.id.title)).setText(title);
        b.setOnClickListener(v -> open(target));
        // Equal cells, each button centred in its own: labels of different widths no longer pull the columns apart.
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, GridLayout.CENTER, 1f),
                GridLayout.spec(GridLayout.UNDEFINED, GridLayout.CENTER, 1f));
        radios.addView(b, lp);
        return b;
    }

    private void open(Intent target) {
        try {
            startActivity(target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private boolean bluetoothOn() {
        try {
            BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
            return bt != null && bt.isEnabled();
        } catch (SecurityException e) {
            return false;
        }
    }
}
