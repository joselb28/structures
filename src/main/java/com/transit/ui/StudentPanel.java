package com.transit.ui;

import com.transit.engine.Snapshot;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/**
 * La boîte « Vos fonctionnalités » du tableau de bord : c'est ici que les questions de
 * développement affichent leurs résultats (voir {@code docs/QUESTIONS.md}).
 *
 * <p>Pour afficher quelque chose : ajoutez une ligne à la liste dans {@link #refresh(Snapshot)}.
 * Si une donnée n'est pas encore dans le {@link Snapshot}, ajoutez-la d'abord dans
 * {@code Snapshot.capture} (thread du moteur), puis lisez-la ici (thread de l'interface).
 */
class StudentPanel extends JPanel {

    private static final Font FONT = new Font("SansSerif", Font.PLAIN, 12);
    /** Largeur du texte, en pixels, pour qu'il passe à la ligne dans la boîte. */
    private static final int TEXT_WIDTH = 210;
    private static final String EMPTY = "Vos fonctionnalités s'afficheront ici. "
            + "Voir docs/QUESTIONS.md et les TODO Q1 à Q15 dans le code.";

    private final List<JLabel> labels = new ArrayList<>();

    StudentPanel() {
        setBackground(Palette.BACKGROUND);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Palette.WAITING_BUS),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
    }

    void refresh(Snapshot snapshot) {
        List<String> lines = new ArrayList<>();
        // TODO Q1 : plus longue attente avant de monter
        // TODO Q2 : clients en attente, ligne par ligne
        // TODO Q3 : taux d'occupation moyen des bus et bus le plus chargé
        // TODO Q8 : nombre de clients qui ont abandonné
        // TODO Q9 : statistiques des lignes calculées en SQL (ou dans la console)
        // TODO Q10 : nombre d'intersections dans la zone de K minutes autour de l'arrêt le plus desservi
        // TODO Q11 : les 10 derniers événements, le plus récent en haut
        show(lines.isEmpty() ? List.of(EMPTY) : lines);
    }

    /** Du HTML de largeur fixe : Swing passe alors à la ligne au lieu de couper le texte. */
    private static String wrapped(String text) {
        return "<html><div style='width:" + TEXT_WIDTH + "px'>" + text + "</div></html>";
    }

    /** Remplace le contenu de la boîte par ces lignes (réutilise les étiquettes existantes). */
    private void show(List<String> lines) {
        while (labels.size() < lines.size()) {
            JLabel label = new JLabel();
            label.setFont(FONT);
            label.setForeground(Palette.TEXT);
            labels.add(label);
            add(label);
        }
        for (int i = 0; i < labels.size(); i++) {
            labels.get(i).setText(i < lines.size() ? wrapped(lines.get(i)) : "");
            labels.get(i).setVisible(i < lines.size());
        }
    }
}
