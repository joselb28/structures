package com.transit.settings;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Tous les réglages d'une simulation, lus dans un fichier de scénario.
 *
 * <p>Les valeurs par défaut sont à un seul endroit : {@code src/main/resources/simulation.properties},
 * qui documente aussi chaque clé et sa plage permise. Un fichier de scénario en remplace certaines.
 *
 * @param seed graine de tous les choix au hasard (ville, clients, errance des Uber)
 */
public record Settings(long seed, City city, Fleet fleet, Customers customers, Fares fares, int ticksPerSecond) {

    /** Changer l'une de ces valeurs régénère la carte. */
    public record City(int width, int height, double intersectionSpacing, int busLines) {
    }

    public record Fleet(int busesPerLine, int busCapacity, int ubers) {
    }

    /**
     * @param perMinute       nouveaux clients par minute simulée, sur les ticks où des clients apparaissent
     * @param spawnEveryTicks les clients apparaissent un tick sur cette valeur ; le débit réel moyen est
     *                        {@code perMinute / spawnEveryTicks}
     */
    public record Customers(double perMinute, int spawnEveryTicks, double rushHourFactor, double terminusShare,
                            double offNetworkShare, int transferMinutes) {
    }

    /**
     * Les prix, en dollars.
     *
     * @param bus           prix fixe d'un trajet en bus, correspondances incluses
     * @param uberBase      partie fixe d'une course Uber
     * @param uberPerMinute ajouté pour chaque minute simulée dans l'Uber
     */
    public record Fares(double bus, double uberBase, double uberPerMinute) {
    }

    private static final String DEFAULTS = "/simulation.properties";

    /** Le scénario par défaut. */
    public static Settings defaults() {
        return from(readDefaults());
    }

    /**
     * Les valeurs par défaut, remplacées par les clés d'un fichier de scénario sur le disque.
     *
     * @throws IllegalArgumentException si une valeur est absente, mal formée ou hors plage
     */
    public static Settings load(Path scenario) throws IOException {
        try (Reader reader = Files.newBufferedReader(scenario, StandardCharsets.UTF_8)) {
            return withOverrides(reader);
        }
    }

    /** Les valeurs par défaut, remplacées par les clés lues dans {@code reader}. */
    static Settings withOverrides(Reader reader) throws IOException {
        Properties properties = readDefaults();
        properties.load(reader);
        return from(properties);
    }

    private static Properties readDefaults() {
        Properties properties = new Properties();
        try (Reader reader = Resources.open(DEFAULTS)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Lecture impossible : " + DEFAULTS, e);
        }
        return properties;
    }

    private static Settings from(Properties p) {
        Keys keys = new Keys(p);
        return new Settings(
                keys.integer("seed", Long.MIN_VALUE, Long.MAX_VALUE),
                new City(
                        (int) keys.integer("city.width", 800, 4800),
                        (int) keys.integer("city.height", 600, 3200),
                        keys.decimal("city.intersectionSpacing", 80, 400),
                        (int) keys.integer("city.busLines", 1, 20)),
                new Fleet(
                        (int) keys.integer("fleet.busesPerLine", 1, 10),
                        (int) keys.integer("fleet.busCapacity", 1, 100),
                        (int) keys.integer("fleet.ubers", 0, 100)),
                new Customers(
                        keys.decimal("customers.perMinute", 0, 100),
                        (int) keys.integer("customers.spawnEveryTicks", 1, 600),
                        keys.decimal("customers.rushHourFactor", 1, 50),
                        keys.decimal("customers.terminusShare", 0, 1),
                        keys.decimal("customers.offNetworkShare", 0, 1),
                        (int) keys.integer("customers.transferMinutes", 0, 60)),
                new Fares(
                        keys.decimal("fares.bus", 0, 50),
                        keys.decimal("fares.uberBase", 0, 50),
                        keys.decimal("fares.uberPerMinute", 0, 10)),
                (int) keys.integer("engine.ticksPerSecond", 1, 200));
    }
}
