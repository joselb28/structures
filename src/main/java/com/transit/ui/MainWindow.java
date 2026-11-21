package com.transit.ui;

import com.transit.engine.Simulation;
import com.transit.model.CityMap;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;

/**
 * Fenêtre principale : la carte (au centre) et le tableau de bord (à droite).
 *
 * <p>Le rendu est découplé du moteur : un minuteur Swing redessine environ 60 fois par seconde,
 * quel que soit le rythme du moteur.
 */
public class MainWindow extends JFrame {

    private static final int FRAME_MILLIS = 16;
    /** Pixels parcourus par cran de molette dans le tableau de bord. */
    private static final int SCROLL_STEP = 16;

    /** @param scenario nom du scénario, affiché dans le titre */
    public MainWindow(CityMap map, Simulation simulation, String scenario) {
        setTitle("CarNBus — " + scenario);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        SimulationPanel canvas = new SimulationPanel(map, simulation::snapshot);
        DashboardPanel dashboard = new DashboardPanel(simulation);
        add(canvas, BorderLayout.CENTER);
        JScrollPane scroll = new JScrollPane(dashboard, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(SCROLL_STEP);
        add(scroll, BorderLayout.EAST);

        new Timer(FRAME_MILLIS, e -> {
            canvas.repaint();
            dashboard.refresh();
        }).start();

        pack();
        setLocationRelativeTo(null);
    }
}
