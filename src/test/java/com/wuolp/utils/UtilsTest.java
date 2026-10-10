package com.wuolp.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UtilsTest {

    @Test
    public void testFormatTimeSecondsOnly() {
        assertEquals("0s", Utils.formatTime(0));
        assertEquals("45s", Utils.formatTime(45));
        assertEquals("59s", Utils.formatTime(59));
    }

    @Test
    public void testFormatTimeMinutesAndSeconds() {
        assertEquals("1m 0s", Utils.formatTime(60));
        assertEquals("1m 15s", Utils.formatTime(75));
        assertEquals("5m 30s", Utils.formatTime(330));
        assertEquals("15m 0s", Utils.formatTime(900));
    }

    @Test
    public void testColorizeLegacy() {
        String input = "&c&lLOCATOR BAR IS ON!";
        String output = Utils.colorize(input);
        assertNotNull(output);
        assertTrue(output.contains("LOCATOR BAR IS ON!"));
    }

    @Test
    public void testColorizeNull() {
        assertEquals("", Utils.colorize((String) null));
    }
}
