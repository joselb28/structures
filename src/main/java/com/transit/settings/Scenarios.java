package com.transit.settings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Stream;

/**
 * Les scénarios fournis avec le projet, dans {@code src/main/resources/scenarios/}.
 *
 * <p>Pour ajouter le vôtre : créez {@code src/main/resources/scenarios/mon-scenario.properties}
 * (première ligne : {@code # Scénario : une phrase qui le décrit}) et ajoutez {@code "mon-scenario"}
 * à {@link #BUNDLED}. Il apparaîtra dans la fenêtre de choix au lancement.
 */
public final class Scenarios {

    /** Le scénario par défaut : seulement les valeurs de {@code simulation.properties}. */
    public static final String DEFAULT = "defaut";

    /** Dans l'ordre où la fenêtre de choix les propose. */
    public static final List<String> BUNDLED = List.of(
            "gros-bus", "plus-de-bus", "sans-uber", "flotte-uber", "correspondances-cheres",
            "terminus-populaires", "ville-dense", "grande-ville", "stress-test");

    private static final String DESCRIPTION_PREFIX = "# Scénario :";

    private Scenarios() {
    }

    /** @return {@link #DEFAULT} puis tous les scénarios fournis */
    public static List<String> names() {
        return Stream.concat(Stream.of(DEFAULT), BUNDLED.stream()).toList();
    }

    /** Charge un scénario fourni par son nom (ex. {@code "gros-bus"}). */
    public static Settings load(String name) {
        if (DEFAULT.equals(name)) {
            return Settings.defaults();
        }
        try (Reader reader = Resources.open(path(name))) {
            return Settings.withOverrides(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** La phrase de la première ligne {@code # Scénario : ...} du fichier. */
    public static String description(String name) {
        if (DEFAULT.equals(name)) {
            return "La ville resserrée par défaut (simulation.properties).";
        }
        try (BufferedReader reader = new BufferedReader(Resources.open(path(name)))) {
            return reader.lines().filter(l -> l.startsWith(DESCRIPTION_PREFIX))
                    .map(l -> l.substring(DESCRIPTION_PREFIX.length()).trim())
                    .findFirst().orElse(name);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String path(String name) {
        return "/scenarios/" + name + ".properties";
    }
}
