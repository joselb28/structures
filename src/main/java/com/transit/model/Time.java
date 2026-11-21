package com.transit.model;

/**
 * Les règles du temps simulé, communes aux agents, aux trajets et aux compteurs.
 */
public final class Time {

    /** Un nœud traversé = une minute simulée = ce nombre de ticks. */
    public static final int TICKS_PER_SIM_MINUTE = 10;
    /** Un bus reste une minute simulée à chaque arrêt. */
    public static final int STOP_DWELL_TICKS = TICKS_PER_SIM_MINUTE;

    private Time() {
    }

    public static double ticksToMinutes(long ticks) {
        return (double) ticks / TICKS_PER_SIM_MINUTE;
    }
}
