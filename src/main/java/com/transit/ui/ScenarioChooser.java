package com.transit.ui;

import com.transit.settings.Scenarios;
import com.transit.settings.Settings;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * La fenêtre affichée au lancement (bouton Play) : choisir un scénario fourni, ou un fichier de
 * scénario sur le disque. À appeler sur l'EDT.
 */
public final class ScenarioChooser {

    /** Le scénario choisi et son nom, affiché dans le titre de la fenêtre. */
    public record Choice(String label, Settings settings) {
    }

    private static final String LAUNCH = "Lancer";
    private static final String OTHER_FILE = "Autre fichier…";
    private static final String QUIT = "Quitter";

    private ScenarioChooser() {
    }

    /** @return le scénario choisi, ou vide si l'utilisateur quitte */
    public static Optional<Choice> choose() {
        JComboBox<String> list = new JComboBox<>(Scenarios.names().toArray(String[]::new));
        JLabel description = new JLabel();
        list.addActionListener(e -> description.setText(Scenarios.description((String) list.getSelectedItem())));
        description.setText(Scenarios.description(Scenarios.DEFAULT));

        while (true) {
            int answer = JOptionPane.showOptionDialog(null, form(list, description), "CarNBus — choisir un scénario",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                    new String[]{LAUNCH, OTHER_FILE, QUIT}, LAUNCH);
            try {
                switch (answer) {
                    case 0 -> {
                        String name = (String) list.getSelectedItem();
                        return Optional.of(new Choice(name, Scenarios.load(name)));
                    }
                    case 1 -> {
                        Optional<Choice> fromFile = chooseFile();
                        if (fromFile.isPresent()) {
                            return fromFile;
                        }
                    }
                    default -> {
                        return Optional.empty();
                    }
                }
            } catch (IllegalArgumentException | IOException e) {
                JOptionPane.showMessageDialog(null, e.getMessage(), "Scénario invalide", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static JPanel form(JComboBox<String> list, JLabel description) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        for (Component c : new Component[]{new JLabel("Scénario :"), list, description}) {
            ((JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(c);
        }
        description.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        return panel;
    }

    private static Optional<Choice> chooseFile() throws IOException {
        JFileChooser chooser = new JFileChooser(new File("."));
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
            return Optional.empty();
        }
        File file = chooser.getSelectedFile();
        return Optional.of(new Choice(file.getName(), Settings.load(file.toPath())));
    }
}
