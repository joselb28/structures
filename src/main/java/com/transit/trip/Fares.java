package com.transit.trip;

import com.transit.model.Time;
import com.transit.settings.Settings;

/**
 * Ce que paient les clients. Les montants sont gardés en <b>cents</b> ({@code long}) : additionner de
 * l'argent en {@code double} accumule des erreurs d'arrondi.
 *
 * @param busTripCents       prix fixe d'un trajet en bus, quel que soit le nombre de correspondances
 * @param uberBaseCents      partie fixe d'une course Uber
 * @param uberPerMinuteCents ajouté pour chaque minute simulée dans l'Uber
 */
public record Fares(long busTripCents, long uberBaseCents, long uberPerMinuteCents) {

    private static final int CENTS_PER_DOLLAR = 100;

    public static Fares of(Settings.Fares dollars) {
        return new Fares(cents(dollars.bus()), cents(dollars.uberBase()), cents(dollars.uberPerMinute()));
    }

    /** Prix d'une course Uber qui a duré {@code rideTicks}, arrondi au cent. */
    public long uberRideCents(long rideTicks) {
        return uberBaseCents + Math.round((double) uberPerMinuteCents * rideTicks / Time.TICKS_PER_SIM_MINUTE);
    }

    private static long cents(double dollars) {
        return Math.round(dollars * CENTS_PER_DOLLAR);
    }
}
