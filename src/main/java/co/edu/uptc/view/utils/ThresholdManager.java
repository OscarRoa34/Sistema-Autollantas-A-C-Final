package co.edu.uptc.view.utils;

import java.util.prefs.Preferences;

public class ThresholdManager {
    private static final String KEY_WARNING = "warning_threshold";
    private static final String KEY_CRITICAL = "critical_threshold";
    private static final double DEFAULT_WARNING = 10;
    private static final double DEFAULT_CRITICAL = 3;

    private final Preferences prefs;

    public ThresholdManager() {
        prefs = Preferences.userNodeForPackage(getClass());
    }

    public int getWarningThreshold() {
        return (int) prefs.getDouble(KEY_WARNING, DEFAULT_WARNING);
    }

    public int getCriticalThreshold() {
        return (int) prefs.getDouble(KEY_CRITICAL, DEFAULT_CRITICAL);
    }

    public void saveThresholds(int warning, int critical) {
        prefs.putDouble(KEY_WARNING, warning);
        prefs.putDouble(KEY_CRITICAL, critical);
    }
}
