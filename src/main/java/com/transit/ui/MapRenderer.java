package com.transit.ui;

import com.transit.model.BusLine;
import com.transit.model.CityMap;
import com.transit.model.Edge;
import com.transit.model.Node;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

/**
 * Dessine la ville statique (routes, lignes de bus, arrêts, numéros de ligne) dans une image.
 * La carte ne change jamais : ce dessin n'est refait que si la fenêtre change de taille.
 */
final class MapRenderer {

    private static final Stroke ROAD_STROKE = new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final Stroke LINE_STROKE = new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final Font LINE_FONT = new Font("SansSerif", Font.BOLD, 14);

    private final CityMap map;
    private final Map<Integer, Color> lineColors;

    MapRenderer(CityMap map, Map<Integer, Color> lineColors) {
        this.map = map;
        this.lineColors = lineColors;
    }

    BufferedImage render(int width, int height, AffineTransform view) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Palette.BACKGROUND);
        g.fillRect(0, 0, width, height);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.transform(view);
        drawRoads(g);
        drawLines(g);
        drawStops(g);
        drawLineNumbers(g);
        g.dispose();
        return image;
    }

    private void drawRoads(Graphics2D g) {
        g.setColor(Palette.ROAD);
        g.setStroke(ROAD_STROKE);
        for (Edge e : map.edges()) {
            Node a = map.node(e.a()), b = map.node(e.b());
            g.draw(new Line2D.Double(a.x(), a.y(), b.x(), b.y()));
        }
    }

    private void drawLines(Graphics2D g) {
        g.setStroke(LINE_STROKE);
        for (BusLine line : map.busLines()) {
            Path2D path = new Path2D.Double();
            Node first = map.node(line.nodeIds().getFirst());
            path.moveTo(first.x(), first.y());
            for (int id : line.nodeIds()) {
                path.lineTo(map.node(id).x(), map.node(id).y());
            }
            g.setColor(lineColors.get(line.number()));
            g.draw(path);
        }
    }

    // TODO Q10 (★★, bonus) : colorer la zone à K tronçons ou moins d'un arrêt, trouvée par un BFS — voir docs/QUESTIONS.md
    /** Anneau creux : arrêt de correspondance. Point blanc : arrêt. Point gris : simple intersection. */
    private void drawStops(Graphics2D g) {
        for (int id : map.intersectionIds()) {
            Node n = map.node(id);
            if (map.isTransferStop(id)) {
                fillCircle(g, n, 9, Palette.STOP);
                fillCircle(g, n, 5, Palette.BACKGROUND);
            } else if (map.isStop(id)) {
                fillCircle(g, n, 6, Palette.STOP);
            } else {
                fillCircle(g, n, 5, Palette.INTERSECTION);
            }
        }
    }

    private void drawLineNumbers(Graphics2D g) {
        g.setFont(LINE_FONT);
        for (BusLine line : map.busLines()) {
            g.setColor(lineColors.get(line.number()));
            for (int terminal : List.of(line.nodeIds().getFirst(), line.nodeIds().getLast())) {
                Node n = map.node(terminal);
                g.drawString(String.valueOf(line.number()), (float) n.x() + 10, (float) n.y() - 10);
            }
        }
    }

    private static void fillCircle(Graphics2D g, Node n, double radius, Color color) {
        g.setColor(color);
        g.fill(new Ellipse2D.Double(n.x() - radius, n.y() - radius, 2 * radius, 2 * radius));
    }
}
