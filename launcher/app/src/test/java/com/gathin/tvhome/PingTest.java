package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PingTest {
    @Test
    public void colors() {
        assertEquals(Ping.GOOD, Ping.color(25));
        assertEquals(Ping.SLOW, Ping.color(80));
        assertEquals(Ping.BAD, Ping.color(200));
        assertEquals(Ping.BAD, Ping.color(-1));
    }
}
