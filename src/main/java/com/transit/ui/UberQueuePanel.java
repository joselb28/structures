package com.transit.ui;

import com.transit.agent.CustomerView;
import com.transit.engine.Snapshot;
import com.transit.model.Time;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * La file des Uber : combien de clients attendent un Uber (réservé ou non) et, pour les
 * {@value #SHOWN} qui attendent depuis le plus longtemps, où et depuis combien de minutes.
 */
class UberQueuePanel extends JPanel {

    /** Nombre de clients détaillés sous le total. */
    static final int SHOWN = 5;
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 13);
    private static final Font ROW_FONT = new Font("SansSerif", Font.PLAIN, 12);

    private final JLabel total = label(TITLE_FONT);
    private final List<JLabel> rows = new ArrayList<>();

    UberQueuePanel() {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        add(total);
        for (int i = 0; i < SHOWN; i++) {
            JLabel row = label(ROW_FONT);
            rows.add(row);
            add(row);
        }
    }

    void refresh(Snapshot snapshot) {
        List<CustomerView> queue = snapshot.waiting().stream()
                .filter(c -> c.mode() == CustomerView.Mode.UBER)
                .sorted(Comparator.comparingLong(CustomerView::waitingSinceTick))
                .toList();
        total.setText("File d'attente : " + queue.size() + (queue.size() <= 1 ? " client" : " clients"));
        for (int i = 0; i < SHOWN; i++) {
            if (i < queue.size()) {
                CustomerView c = queue.get(i);
                long minutes = (long) Time.ticksToMinutes(snapshot.tick() - c.waitingSinceTick());
                rows.get(i).setText("  " + (i + 1) + ". client " + c.id() + " au nœud " + c.node() + " — " + minutes + " min");
            }
            rows.get(i).setVisible(i < queue.size());
        }
    }

    private static JLabel label(Font font) {
        JLabel label = new JLabel(" ");
        label.setFont(font);
        label.setForeground(Palette.TEXT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }
}
