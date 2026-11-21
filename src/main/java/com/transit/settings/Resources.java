package com.transit.settings;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/** Ouvre les fichiers de {@code src/main/resources} : ils voyagent avec le code, quel que soit le dossier de travail. */
final class Resources {

    private Resources() {
    }

    /** @throws IllegalStateException si la ressource n'existe pas */
    static Reader open(String path) {
        InputStream in = Resources.class.getResourceAsStream(path);
        if (in == null) {
            throw new IllegalStateException("Ressource introuvable : " + path);
        }
        return new InputStreamReader(in, StandardCharsets.UTF_8);
    }
}
