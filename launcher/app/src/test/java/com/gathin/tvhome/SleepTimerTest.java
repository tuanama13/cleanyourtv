package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SleepTimerTest {
    @Test
    public void steps() {
        assertEquals(30, SleepTimer.nextStep(0));
        assertEquals(60, SleepTimer.nextStep(30));
        assertEquals(60, SleepTimer.nextStep(25));
        assertEquals(90, SleepTimer.nextStep(45));
        assertEquals(0, SleepTimer.nextStep(90));
        assertEquals(0, SleepTimer.nextStep(61));
    }
}
