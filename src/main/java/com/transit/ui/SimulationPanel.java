package com.transit.ui;

import com.transit.agent.CustomerView;
import com.transit.agent.VehicleView;
import com.transit.engine.Snapshot;
import com.transit.model.CityMap;
import com.transit.model.Node;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import javax.swing.JPanel;

/**
 * Le canevas de la carte, mis à l'échelle de la fenêtre : l'image de la ville gardée en cache par
 * {@link MapRenderer}, puis les pastilles d'attente (bus / Uber) et les véhicules du dernier
 * {@link Snapshot} du moteur.
 *
 * <p>Les véhicules sont interpolés entre leur position du tick précédent et celle du tick courant :
 * le mouvement reste fluide quel que soit le nombre d'images par seconde.
 */
public class SimulationPanel extends JPanel {

    private static final Font LABEL_FONT = new Font("SansSerif", Font.BOLD, 10);
    private static final Stroke OUTLINE = new BasicStroke(1.5f);
    private static final Font BADGE_FONT = new Font("SansSerif", Font.BOLD, 16);
    private static final double BADGE_RADIUS = 13;
    /** Le centre des pastilles est à cette distance sous le nœud, à droite (bus) ou à gauche (Uber). */
    private static final double BADGE_OFFSET = 15;

    private final CityMap map;
    private final Supplier<Snapshot> snapshots;
    private final Map<Integer, Color> lineColors;
    private final MapRenderer mapRenderer;
    private final VehicleRenderer vehicles;
    private BufferedImage mapImage;

    public SimulationPanel(CityMap map, Supplier<Snapshot> snapshots) {
        this.map = map;
        this.snapshots = snapshots;
        this.lineColors = Palette.lineColors(map);
        this.mapRenderer = new MapRenderer(map, lineColors);
        this.vehicles = new VehicleRenderer(lineColors);
        setPreferredSize(new Dimension(map.width() / 2, map.height() / 2));
        setBackground(Palette.BACKGROUND);
    }

    /**
     * Échelle + décalage qui transforment les coordonnées virtuelles en pixels du panneau, en gardant les proportions.
     */
    private AffineTransform viewTransform() {
        double scale = Math.min((double) getWidth() / map.width(), (double) getHeight() / map.height());
        AffineTransform t = AffineTransform.getTranslateInstance(
                (getWidth() - map.width() * scale) / 2, (getHeight() - map.height() * scale) / 2);
        t.scale(scale, scale);
        return t;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        if (mapImage == null || mapImage.getWidth() != getWidth() || mapImage.getHeight() != getHeight()) {
            mapImage = mapRenderer.render(getWidth(), getHeight(), viewTransform());
        }
        g.drawImage(mapImage, 0, 0, null);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.transform(viewTransform());
        g2.setFont(LABEL_FONT);
        Snapshot snapshot = snapshots.get();
        drawWaitingCustomers(g2, snapshot);
        drawVehicles(g2, snapshot);
        g2.dispose();
    }

    private void drawVehicles(Graphics2D g, Snapshot snapshot) {
        double alpha = Math.clamp(
                (double) (System.nanoTime() - snapshot.publishedAtNanos()) / snapshot.tickPeriodNanos(), 0.0, 1.0);
        for (VehicleView v : snapshot.vehicles()) {
            double x = v.previousX() + (v.x() - v.previousX()) * alpha;
            double y = v.previousY() + (v.y() - v.previousY()) * alpha;
            vehicles.draw(g, v, x, y);
        }
    }

    /**
     * Deux pastilles numérotées sous chaque nœud où des clients attendent, à des places fixes pour ne
     * jamais sauter : jaune à droite pour le bus, verte à gauche pour l'Uber.
     */
    private void drawWaitingCustomers(Graphics2D g, Snapshot snapshot) {
        Map<Integer, int[]> countsByNode = new TreeMap<>(); // nœud -> {bus, uber}
        for (CustomerView c : snapshot.waiting()) {
            countsByNode.computeIfAbsent(c.node(), k -> new int[2])[c.mode() == CustomerView.Mode.BUS ? 0 : 1]++;
        }
        g.setFont(BADGE_FONT);
        g.setStroke(OUTLINE);
        countsByNode.forEach((nodeId, counts) -> {
            Node node = map.node(nodeId);
            drawBadge(g, node.x() + BADGE_OFFSET, node.y() + BADGE_OFFSET, counts[0], Palette.WAITING_BUS);
            drawBadge(g, node.x() - BADGE_OFFSET, node.y() + BADGE_OFFSET, counts[1], Palette.WAITING_UBER);
        });
        g.setFont(LABEL_FONT);
    }

    /** Un disque avec le nombre, qui grossit lentement avec la foule. Rien pour zéro. */
    private static void drawBadge(Graphics2D g, double cx, double cy, int count, Color color) {
        if (count == 0) {
            return;
        }
        double radius = BADGE_RADIUS + Math.min(8, Math.sqrt(count));
        Ellipse2D badge = new Ellipse2D.Double(cx - radius, cy - radius, 2 * radius, 2 * radius);
        g.setColor(color);
        g.fill(badge);
        g.setColor(Palette.BACKGROUND);
        g.draw(badge);
        String text = String.valueOf(count);
        var metrics = g.getFontMetrics();
        g.drawString(text, (float) (cx - metrics.stringWidth(text) / 2.0), (float) (cy + metrics.getAscent() / 2.0 - 2));
    }
}
