package com.transit;

import com.transit.engine.Simulation;
import com.transit.map.MapStore;
import com.transit.model.CityMap;
import com.transit.settings.Settings;
import com.transit.ui.MainWindow;
import com.transit.ui.ScenarioChooser;
import com.transit.ui.ScenarioChooser.Choice;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Point d'entrée : choix du scénario, démarrage à froid (SQLite -> RAM), thread du moteur, fenêtre Swing.
 *
 * <p>Avec le bouton Play, une fenêtre propose les scénarios fournis. On peut aussi donner le chemin d'un
 * fichier de scénario en argument (ex. {@code mvn compile exec:java -Dexec.args="mon-scenario.properties"}).
 */
public class Main {

    private static final Path DB_FILE = Path.of("transit.db");

    public static void main(String[] args) throws IOException, InterruptedException, InvocationTargetException {
        Optional<Choice> choice = args.length > 0
                ? Optional.of(new Choice(args[0], Settings.load(Path.of(args[0]))))
                : chooseOnScreen();
        if (choice.isEmpty()) {
            return;
        }
        Settings settings = choice.get().settings();
        System.out.println("[Scénario] " + choice.get().label() + " : " + settings);

        long start = System.nanoTime();
        CityMap map = MapStore.loadOrGenerate(DB_FILE, settings.city(), settings.seed());
        System.out.printf("[Carte] %d nœuds, %d arêtes, %d lignes de bus (%d demandées), chargée en %d ms%n",
                map.nodeCount(), map.edges().size(), map.busLines().size(), settings.city().busLines(),
                (System.nanoTime() - start) / 1_000_000);

        // TODO Q9 (★★) : statistiques des lignes calculées en SQL sur transit.db (SELECT, JOIN, GROUP BY) — voir docs/QUESTIONS.md
        Simulation simulation = new Simulation(map, settings);
        simulation.start();

        SwingUtilities.invokeLater(() -> new MainWindow(map, simulation, choice.get().label()).setVisible(true));
    }

    /** La fenêtre de choix doit s'afficher sur le thread de l'interface (EDT). */
    private static Optional<Choice> chooseOnScreen() throws InterruptedException, InvocationTargetException {
        AtomicReference<Optional<Choice>> result = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> result.set(ScenarioChooser.choose()));
        return result.get();
    }
}
