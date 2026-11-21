package com.transit.ui;

import com.transit.engine.Simulation;
import com.transit.engine.Snapshot;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.text.NumberFormat;
import java.util.Locale;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Panneau de droite : heure simulée, rythme du moteur, commandes de vitesse et statistiques.
 * Rafraîchi par le minuteur d'images de la fenêtre, sur l'EDT.
 */
class DashboardPanel extends JPanel {

    private static final int START_HOUR = 6;
    /** Taille du tableau de bord ; plus haut que l'écran, il défile. */
    private static final int WIDTH = 300, HEIGHT = 900;
    private static final int[] SPEEDS = {1, 4, 16};

    private final Simulation simulation;
    private final JLabel clock = label("", 28);
    private final JLabel tick = label("", 13);
    private final JLabel engineRate = label("", 13);
    private final JLabel speed = label("", 13);
    private final JLabel waiting = label("", 13);
    private final JLabel inTransit = label("", 13);
    private final JLabel delivered = label("", 13);
    private final JLabel averageWait = label("", 13);
    private final JLabel averageTrip = label("", 13);
    private final JLabel fullBuses = label("", 13);
    private final JButton pause = new JButton("Pause");
    private final JButton rushHour = new JButton();
    private final FleetPanel fleet = new FleetPanel();
    private final FaresSection fares;
    private final UberQueuePanel uberQueue = new UberQueuePanel();
    private final StudentPanel student = new StudentPanel();

    private long lastTick;
    private long lastMeasureNanos = System.nanoTime();
    private double measuredTicksPerSecond;

    DashboardPanel(Simulation simulation) {
        this.simulation = simulation;
        this.fares = new FaresSection(simulation.fares());
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Palette.PANEL);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(label("Heure simulée", 12));
        add(clock);
        add(Box.createVerticalStrut(15));
        add(tick);
        engineRate.setToolTipText("<html>Nombre de ticks que le moteur a réellement calculés pendant la dernière"
                + " seconde réelle,<br>comparé au rythme visé : engine.ticksPerSecond × la vitesse choisie."
                + "<br>S'il reste sous la cible, le moteur n'arrive plus à suivre.</html>");
        add(engineRate);
        add(speed);
        add(Box.createVerticalStrut(15));
        add(controls());
        add(Box.createVerticalStrut(8));
        rushHour.setAlignmentX(Component.LEFT_ALIGNMENT);
        rushHour.addActionListener(e -> simulation.spawner().setRushHour(!simulation.spawner().isRushHour()));
        add(rushHour);
        // TODO Q4 (★) : bouton « Enregistrer la séance » qui écrit dans la table run_stats (JDBC) — voir docs/QUESTIONS.md
        add(Box.createVerticalStrut(25));
        add(label("Clients", 12));
        add(waiting);
        add(inTransit);
        add(delivered);
        add(averageWait);
        add(averageTrip);
        add(Box.createVerticalStrut(15));
        add(label("Bus", 12));
        add(fullBuses);
        add(Box.createVerticalStrut(15));
        add(label("Tarifs et recettes", 12));
        add(fares);
        add(Box.createVerticalStrut(15));
        add(label("Flotte Uber", 12));
        add(fleet);
        add(Box.createVerticalStrut(8));
        add(uberQueue);
        add(Box.createVerticalStrut(15));
        add(label("Vos fonctionnalités", 12));
        add(Box.createVerticalStrut(4));
        add(student);
        add(Box.createVerticalGlue());
        refresh();
    }

    private JPanel controls() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        pause.addActionListener(e -> simulation.setPaused(!simulation.isPaused()));
        row.add(pause);
        for (int factor : SPEEDS) {
            JButton button = new JButton("×" + factor);
            button.addActionListener(e -> simulation.setSpeed(factor));
            row.add(button);
        }
        row.setMaximumSize(row.getPreferredSize()); // sinon BoxLayout étirerait la rangée
        return row;
    }

    /**
     * Lit le dernier snapshot et met à jour chaque étiquette. Mesure aussi, une fois par seconde, le
     * nombre de ticks réellement calculés pendant la dernière seconde.
     */
    void refresh() {
        Snapshot snapshot = simulation.snapshot();
        long minutes = START_HOUR * 60 + snapshot.simulatedMinutes();
        clock.setText(String.format("J%d  %02d:%02d", minutes / 1440 + 1, minutes / 60 % 24, minutes % 60));
        tick.setText("Tick : " + snapshot.tick());

        long now = System.nanoTime();
        if (now - lastMeasureNanos >= 1_000_000_000L) {
            measuredTicksPerSecond = (snapshot.tick() - lastTick) * 1e9 / (now - lastMeasureNanos);
            lastTick = snapshot.tick();
            lastMeasureNanos = now;
        }
        engineRate.setText(String.format(Locale.CANADA_FRENCH, "Rythme du moteur : %.1f ticks/s (visé : %d)",
                measuredTicksPerSecond, simulation.ticksPerSecond() * simulation.speed()));
        speed.setText("Vitesse : ×" + simulation.speed() + (simulation.isPaused() ? "  (pause)" : ""));
        pause.setText(simulation.isPaused() ? "Reprendre" : "Pause");

        Snapshot.Stats stats = snapshot.stats();
        waiting.setText("En attente : " + stats.waiting());
        inTransit.setText("En transit : " + stats.inTransit());
        delivered.setText("Arrivés : " + stats.delivered() + " / " + stats.spawned());
        averageWait.setText(String.format("Attente moyenne à l'arrêt : %.1f min", stats.averageWaitMinutes()));
        averageTrip.setText(String.format("Trajet porte-à-porte : %.1f min", stats.averageTripMinutes()));
        fullBuses.setText("Bus pleins (FULL) : " + stats.fullBuses());
        fares.refresh(stats);
        fleet.refresh(snapshot);
        uberQueue.refresh(snapshot);
        student.refresh(snapshot);
        String factor = "×" + NumberFormat.getInstance(Locale.CANADA_FRENCH).format(simulation.spawner().rushHourFactor());
        rushHour.setText(simulation.spawner().isRushHour() ? "Fin de l'heure de pointe" : "Heure de pointe (" + factor + ")");
    }

    private static JLabel label(String text, int size) {
        JLabel label = new JLabel(text);
        label.setForeground(Palette.TEXT);
        label.setFont(new Font("SansSerif", Font.BOLD, size));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }
}
