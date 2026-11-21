package com.transit.ui;

import com.transit.agent.VehicleView;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Map;

/** Dessine un bus ou un Uber à une position donnée, en coordonnées virtuelles. */
final class VehicleRenderer {

    /** La jauge d'occupation devient orange à partir de cette proportion de places prises. */
    private static final double GAUGE_WARNING_SHARE = 0.75;
    private static final double BUS_WIDTH = 28, BUS_HEIGHT = 16, GAUGE_HEIGHT = 5;
    private static final double UBER_RADIUS = 9;
    private static final Stroke OUTLINE = new BasicStroke(1.5f);
    private static final Stroke RING = new BasicStroke(3f);

    private final Map<Integer, Color> lineColors;

    VehicleRenderer(Map<Integer, Color> lineColors) {
        this.lineColors = lineColors;
    }

    void draw(Graphics2D g, VehicleView vehicle, double x, double y) {
        if (vehicle.kind() == VehicleView.Kind.BUS) {
            drawBus(g, vehicle, x, y);
        } else {
            drawUber(g, vehicle, x, y);
        }
    }

    /** Carrosserie à la couleur de la ligne (rouge si FULL), jauge d'occupation dessous, « id  n/places » au-dessus. */
    private void drawBus(Graphics2D g, VehicleView bus, double x, double y) {
        double left = x - BUS_WIDTH / 2, top = y - BUS_HEIGHT / 2;
        RoundRectangle2D body = new RoundRectangle2D.Double(left, top, BUS_WIDTH, BUS_HEIGHT, 6, 6);
        g.setColor(bus.isFull() ? Palette.FULL : lineColors.getOrDefault(bus.line(), Color.GRAY));
        g.fill(body);
        g.setColor(Palette.BACKGROUND);
        g.setStroke(OUTLINE);
        g.draw(body);

        double share = (double) bus.passengers() / bus.capacity();
        double gaugeTop = top + BUS_HEIGHT + 2;
        g.setColor(Palette.GAUGE_EMPTY);
        g.fill(new Rectangle2D.Double(left, gaugeTop, BUS_WIDTH, GAUGE_HEIGHT));
        g.setColor(share >= 1 ? Palette.FULL : share >= GAUGE_WARNING_SHARE ? Palette.UBER_BUSY : Palette.UBER_FREE);
        g.fill(new Rectangle2D.Double(left, gaugeTop, BUS_WIDTH * share, GAUGE_HEIGHT));

        g.setColor(Color.WHITE);
        String occupancy = bus.isFull() ? "FULL" : bus.passengers() + "/" + bus.capacity();
        g.drawString(bus.id() + "  " + occupancy, (float) left - 1, (float) top - 4);
    }

    /** Libre : vert. En route vers un client : anneau orange. Client à bord : orange plein. */
    private void drawUber(Graphics2D g, VehicleView uber, double x, double y) {
        Ellipse2D body = new Ellipse2D.Double(x - UBER_RADIUS, y - UBER_RADIUS, 2 * UBER_RADIUS, 2 * UBER_RADIUS);
        switch (UberStatus.of(uber)) {
            case FREE -> fillWithOutline(g, body, Palette.UBER_FREE);
            case ON_ITS_WAY -> {
                g.setColor(Palette.BACKGROUND);
                g.fill(body);
                g.setColor(Palette.UBER_BUSY);
                g.setStroke(RING);
                g.draw(body);
            }
            case CUSTOMER_ON_BOARD -> fillWithOutline(g, body, Palette.UBER_BUSY);
        }
        g.setColor(Color.WHITE);
        g.drawString(uber.id(), (float) (x - UBER_RADIUS - 6), (float) (y - UBER_RADIUS - 4));
    }

    private static void fillWithOutline(Graphics2D g, Ellipse2D shape, Color color) {
        g.setColor(color);
        g.fill(shape);
        g.setColor(Color.WHITE);
        g.setStroke(OUTLINE);
        g.draw(shape);
    }
}
