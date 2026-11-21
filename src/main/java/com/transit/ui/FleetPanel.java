package com.transit.ui;

import com.transit.agent.VehicleView;
import com.transit.engine.Snapshot;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Une case numérotée par Uber, avec les trois mêmes rendus que sur la carte : vert (libre), contour
 * orange (en route vers un client), orange plein (client à bord). Une légende est affichée dessous ;
 * survoler une case affiche son état complet.
 */
class FleetPanel extends JComponent {

    private static final int SQUARE = 24;
    private static final int GAP = 6;
    private static final int WIDTH = 270;
    private static final int LEGEND_LINE = 18;
    private static final Font NUMBER_FONT = new Font("SansSerif", Font.BOLD, 11);
    private static final Font LEGEND_FONT = new Font("SansSerif", Font.PLAIN, 12);

    private List<VehicleView> ubers = List.of();

    FleetPanel() {
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setToolTipText(""); // active les infobulles ; le texte vient de getToolTipText(MouseEvent)
    }

    void refresh(Snapshot snapshot) {
        List<VehicleView> latest = snapshot.vehicles().stream().filter(v -> v.kind() == VehicleView.Kind.UBER).toList();
        if (latest.size() != ubers.size()) {
            ubers = latest;
            revalidate();
        }
        ubers = latest;
        repaint();
    }

    private int columns() {
        return Math.max(1, (WIDTH + GAP) / (SQUARE + GAP));
    }

    private int gridHeight() {
        int rows = (ubers.size() + columns() - 1) / columns();
        return rows * (SQUARE + GAP);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(WIDTH, gridHeight() + UberStatus.values().length * LEGEND_LINE);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int col = e.getX() / (SQUARE + GAP), row = e.getY() / (SQUARE + GAP);
        int index = row * columns() + col;
        if (col >= columns() || index >= ubers.size()) {
            return null;
        }
        VehicleView uber = ubers.get(index);
        return uber.id() + " : " + UberStatus.of(uber).label;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i = 0; i < ubers.size(); i++) {
            int x = (i % columns()) * (SQUARE + GAP), y = (i / columns()) * (SQUARE + GAP);
            drawSquare(g, x, y, SQUARE, UberStatus.of(ubers.get(i)));
            g.setFont(NUMBER_FONT);
            g.setColor(UberStatus.of(ubers.get(i)) == UberStatus.ON_ITS_WAY ? Palette.TEXT : Palette.BACKGROUND);
            String number = String.valueOf(i + 1);
            g.drawString(number, x + (SQUARE - g.getFontMetrics().stringWidth(number)) / 2f, y + SQUARE / 2f + 4);
        }
        drawLegend(g, gridHeight());
        g.dispose();
    }

    private void drawLegend(Graphics2D g, int top) {
        g.setFont(LEGEND_FONT);
        int swatch = LEGEND_LINE - 6;
        for (UberStatus status : UberStatus.values()) {
            drawSquare(g, 0, top, swatch, status);
            g.setColor(Palette.TEXT);
            g.drawString(status.label, swatch + 8, top + swatch - 1);
            top += LEGEND_LINE;
        }
    }

    private static void drawSquare(Graphics2D g, int x, int y, int size, UberStatus status) {
        switch (status) {
            case FREE -> fill(g, x, y, size, Palette.UBER_FREE);
            case CUSTOMER_ON_BOARD -> fill(g, x, y, size, Palette.UBER_BUSY);
            case ON_ITS_WAY -> {
                g.setColor(Palette.UBER_BUSY);
                g.setStroke(new BasicStroke(3f));
                g.drawRoundRect(x + 1, y + 1, size - 3, size - 3, 5, 5);
            }
        }
    }

    private static void fill(Graphics2D g, int x, int y, int size, Color color) {
        g.setColor(color);
        g.fillRoundRect(x, y, size, size, 5, 5);
    }
}
