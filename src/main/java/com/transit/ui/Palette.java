package com.transit.ui;

import com.transit.model.BusLine;
import com.transit.model.CityMap;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Les couleurs communes à la carte, aux véhicules et au tableau de bord. */
final class Palette {

    static final Color BACKGROUND = new Color(28, 30, 34);
    static final Color PANEL = new Color(40, 44, 52);
    static final Color TEXT = new Color(220, 223, 228);
    static final Color ROAD = new Color(60, 64, 72);
    static final Color INTERSECTION = new Color(90, 95, 110);
    static final Color STOP = new Color(235, 235, 235);
    static final Color UBER_FREE = new Color(46, 204, 113);
    static final Color UBER_BUSY = new Color(230, 126, 34);
    static final Color FULL = new Color(231, 76, 60);
    static final Color WAITING_BUS = new Color(241, 196, 15);
    static final Color WAITING_UBER = UBER_FREE;
    static final Color GAUGE_EMPTY = new Color(70, 74, 84);

    private static final Color[] LINES = {
            new Color(231, 76, 60), new Color(241, 196, 15), new Color(155, 89, 182), new Color(26, 188, 156),
            new Color(230, 126, 34), new Color(52, 152, 219), new Color(236, 112, 160), new Color(149, 165, 166),
            new Color(46, 204, 113), new Color(192, 57, 43),
    };

    private Palette() {
    }

    /** Numéro de ligne -> couleur, attribuée dans l'ordre des lignes de la carte. */
    static Map<Integer, Color> lineColors(CityMap map) {
        Map<Integer, Color> colors = new HashMap<>();
        List<BusLine> lines = map.busLines();
        for (int i = 0; i < lines.size(); i++) {
            colors.put(lines.get(i).number(), LINES[i % LINES.length]);
        }
        return colors;
    }
}
