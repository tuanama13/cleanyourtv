package com.gathin.tvhome;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Current temperature, sky and the next change between rain and dry, from Open-Meteo (no key, no account). */
final class Weather {
    private static final Pattern TEMP = Pattern.compile("\"temperature_2m\"\\s*:\\s*(-?[0-9.]+)");
    private static final Pattern CODE = Pattern.compile("\"weather_code\"\\s*:\\s*([0-9]+)");
    private static final Pattern NOW = Pattern.compile("\"current\"\\s*:\\s*\\{\\s*\"time\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern HOURLY = Pattern.compile(
            "\"hourly\"\\s*:\\s*\\{\\s*\"time\"\\s*:\\s*\\[([^\\]]*)\\]\\s*,\\s*\"weather_code\"\\s*:\\s*\\[([^\\]]*)\\]");

    private Weather() {}

    /** Blocking; call off the UI thread. "29° Cerah", or null when the network or the reply fails. */
    static String fetch(String latLon) {
        String[] p = latLon.split(",");
        if (p.length != 2) return null;
        HttpURLConnection c = null;
        try {
            URL url = new URL("https://api.open-meteo.com/v1/forecast?latitude=" + p[0].trim()
                    + "&longitude=" + p[1].trim() + "&current=temperature_2m,weather_code"
                    + "&hourly=weather_code&forecast_hours=12&timezone=auto");
            c = (HttpURLConnection) url.openConnection();
            c.setConnectTimeout(10_000);
            c.setReadTimeout(10_000);
            try (InputStream in = c.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
                return parse(out.toString("UTF-8"));
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    static String parse(String json) {
        // The "current" block comes after "current_units", whose values are strings, so the last match is the number.
        String temp = last(TEMP, json), code = last(CODE, json);
        if (temp == null || code == null) return null;
        int now = Integer.parseInt(code);
        String text = String.format(Locale.ROOT, "%d° %s", Math.round(Double.parseDouble(temp)), describe(now));
        String change = nextChange(json, rainy(now));
        return change == null ? text : text + " · " + change;
    }

    /** "Hujan ~15.00" when dry now, "Reda ~17.00" when raining now: the first later hour that differs, or null. */
    private static String nextChange(String json, boolean rainingNow) {
        Matcher now = NOW.matcher(json), hourly = HOURLY.matcher(json);
        if (!now.find() || !hourly.find()) return null;
        String[] times = hourly.group(1).replace("\"", "").split(","), codes = hourly.group(2).split(",");
        for (int i = 0; i < Math.min(times.length, codes.length); i++) {
            String t = times[i].trim();
            if (t.compareTo(now.group(1)) <= 0 || t.length() < 13) continue;
            if (rainy(Integer.parseInt(codes[i].trim())) != rainingNow)
                return (rainingNow ? "Reda ~" : "Hujan ~") + t.substring(11, 13) + ".00";
        }
        return null;
    }

    static boolean rainy(int code) {
        return (code >= 51 && code <= 67) || (code >= 80 && code <= 82) || code >= 95;
    }

    private static String last(Pattern p, String s) {
        Matcher m = p.matcher(s);
        String v = null;
        while (m.find()) v = m.group(1);
        return v;
    }

    /** WMO weather interpretation codes, as Open-Meteo documents them. */
    static String describe(int code) {
        if (code == 0) return "Cerah";
        if (code <= 2) return "Cerah berawan";
        if (code == 3) return "Berawan";
        if (code <= 48) return "Berkabut";
        if (code <= 57) return "Gerimis";
        if (code <= 67 || (code >= 80 && code <= 82)) return "Hujan";
        if (code <= 77 || code == 85 || code == 86) return "Salju";
        if (code >= 95) return "Badai petir";
        return "";
    }
}
