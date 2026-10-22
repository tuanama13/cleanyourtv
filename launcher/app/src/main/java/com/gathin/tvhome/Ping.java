package com.gathin.tvhome;

import java.net.InetSocketAddress;
import java.net.Socket;

/** Internet round trip for the Wi-Fi pill's dot: a TCP connect to a public DNS server, no DNS lookup of its own. */
final class Ping {
    static final int GOOD = 0xFF34C759, SLOW = 0xFFFFCC00, BAD = 0xFFFF3B30;

    private Ping() {}

    /** Blocking; call off the UI thread. Best of three connects in ms, or -1 when none got through. */
    static int measure() {
        int best = -1;
        for (int i = 0; i < 3; i++) {
            long start = System.nanoTime();
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress("8.8.8.8", 53), 2000);
                int ms = (int) ((System.nanoTime() - start) / 1_000_000);
                if (best < 0 || ms < best) best = ms;
            } catch (Exception ignored) {
            }
        }
        return best;
    }

    static int color(int ms) {
        if (ms < 0) return BAD;
        if (ms < 80) return GOOD;
        return ms < 200 ? SLOW : BAD;
    }
}
