package com.transit.settings;

import java.util.Properties;

/** Lit dans les propriétés des valeurs typées et vérifiées (plage), avec des messages d'erreur qui nomment la clé. */
final class Keys {

    private final Properties properties;

    Keys(Properties properties) {
        this.properties = properties;
    }

    long integer(String key, long min, long max) {
        String raw = raw(key);
        try {
            return check(key, Long.parseLong(raw), min, max);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " = " + raw + " : un nombre entier est attendu");
        }
    }

    double decimal(String key, double min, double max) {
        String raw = raw(key);
        try {
            return check(key, Double.parseDouble(raw), min, max);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " = " + raw + " : un nombre est attendu (ex. 0.4)");
        }
    }

    private String raw(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Clé manquante : " + key);
        }
        return value.trim();
    }

    private static <T extends Comparable<T>> T check(String key, T value, T min, T max) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new IllegalArgumentException(key + " = " + value + " : hors plage [" + min + " .. " + max + "]");
        }
        return value;
    }
}
