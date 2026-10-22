package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class WeatherTest {
    @Test
    public void parsesCurrentBlockNotUnits() {
        String json = "{\"current_units\":{\"temperature_2m\":\"°C\",\"weather_code\":\"wmo code\"},"
                + "\"current\":{\"time\":\"2026-10-22T17:00\",\"temperature_2m\":26.7,\"weather_code\":63}}";
        assertEquals("27° Hujan", Weather.parse(json));
    }

    @Test
    public void rainLaterAndStopsLater() {
        String hourly = ",\"hourly\":{\"time\":[\"2026-10-23T00:00\",\"2026-10-23T01:00\",\"2026-10-23T02:00\",\"2026-10-23T03:00\"],"
                + "\"weather_code\":[%s]}}";
        String dry = "{\"current\":{\"time\":\"2026-10-23T00:30\",\"temperature_2m\":26.0,\"weather_code\":3}";
        // 00:00 is before now and ignored even though it rains.
        assertEquals("26° Berawan · Hujan ~02.00", Weather.parse(dry + String.format(hourly, "63,3,61,61")));
        assertEquals("26° Berawan", Weather.parse(dry + String.format(hourly, "3,3,3,3")));
        String wet = "{\"current\":{\"time\":\"2026-10-23T00:30\",\"temperature_2m\":24.0,\"weather_code\":81}";
        assertEquals("24° Hujan · Reda ~03.00", Weather.parse(wet + String.format(hourly, "81,81,95,2")));
    }

    @Test
    public void badReplyIsNull() {
        assertNull(Weather.parse("{\"error\":true}"));
    }

    @Test
    public void codes() {
        assertEquals("Cerah", Weather.describe(0));
        assertEquals("Berawan", Weather.describe(3));
        assertEquals("Hujan", Weather.describe(81));
        assertEquals("Badai petir", Weather.describe(95));
    }
}
