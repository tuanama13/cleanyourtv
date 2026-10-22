package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MediaCardTest {
    @Test
    public void times() {
        assertEquals("0:00", MediaCard.time(0));
        assertEquals("3:05", MediaCard.time(185_000));
        assertEquals("1:02:03", MediaCard.time(3_723_000));
    }
}
