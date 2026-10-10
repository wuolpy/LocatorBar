package com.wuolp.updater;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UpdateManagerTest {

    @Test
    public void testIsNewerVersion() {
        assertTrue(UpdateManager.isNewerVersion("1.0.0", "1.0.1"));
        assertTrue(UpdateManager.isNewerVersion("1.0.0", "1.1.0"));
        assertTrue(UpdateManager.isNewerVersion("1.0.0", "2.0.0"));
        assertTrue(UpdateManager.isNewerVersion("v1.0.0", "v1.0.1"));
        assertTrue(UpdateManager.isNewerVersion("1.0.0", "v1.0.1"));
        assertTrue(UpdateManager.isNewerVersion("1.0.0-SNAPSHOT", "1.0.1"));
        assertTrue(UpdateManager.isNewerVersion("1.0", "1.0.1"));

        assertFalse(UpdateManager.isNewerVersion("1.0.0", "1.0.0"));
        assertFalse(UpdateManager.isNewerVersion("v1.0.0", "v1.0.0"));
        assertFalse(UpdateManager.isNewerVersion("1.0.1", "1.0.0"));
        assertFalse(UpdateManager.isNewerVersion("2.0.0", "1.9.9"));
        assertFalse(UpdateManager.isNewerVersion(null, "1.0.0"));
        assertFalse(UpdateManager.isNewerVersion("1.0.0", null));
    }
}
