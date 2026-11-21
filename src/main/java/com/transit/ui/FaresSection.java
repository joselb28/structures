package com.transit.ui;

import com.transit.engine.Snapshot;
import com.transit.trip.Fares;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Font;
import java.text.NumberFormat;
import java.util.Locale;

/** Section du tableau de bord : les tarifs du scénario et ce que chaque mode a rapporté jusqu'ici. */
class FaresSection extends JPanel {

    private static final int CENTS_PER_DOLLAR = 100;
    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(Locale.CANADA_FRENCH);
    private static final Font FONT = new Font("SansSerif", Font.BOLD, 13);

    private final JLabel busRevenue = line();
    private final JLabel uberRevenue = line();
    private final JLabel averageUberRide = line();
    private final JLabel averagePrice = line();

    FaresSection(Fares fares) {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel busTariff = line();
        busTariff.setText("Bus : " + money(fares.busTripCents()) + " / trajet");
        JLabel uberTariff = line();
        uberTariff.setText("Uber : " + money(fares.uberBaseCents()) + " + " + money(fares.uberPerMinuteCents()) + " / min");
        add(busTariff);
        add(uberTariff);
        add(busRevenue);
        add(uberRevenue);
        add(averageUberRide);
        add(averagePrice);
    }

    void refresh(Snapshot.Stats stats) {
        busRevenue.setText("Recettes bus : " + money(stats.busRevenueCents()) + " (" + stats.busFares() + " trajets)");
        double averageUber = stats.uberFares() == 0 ? 0 : (double) stats.uberRevenueCents() / stats.uberFares();
        uberRevenue.setText("Recettes Uber : " + money(stats.uberRevenueCents()) + " (" + stats.uberFares() + " courses)");
        averageUberRide.setText("Prix moyen d'une course Uber : " + money(averageUber));
        averagePrice.setText("Prix moyen d'un trajet : " + money(stats.averageTripPriceCents()));
    }

    private static String money(double cents) {
        return MONEY.format(cents / CENTS_PER_DOLLAR);
    }

    private static JLabel line() {
        JLabel label = new JLabel();
        label.setFont(FONT);
        label.setForeground(Palette.TEXT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }
}
