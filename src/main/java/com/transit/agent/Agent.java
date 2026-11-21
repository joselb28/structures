package com.transit.agent;

/**
 * Tout ce que le moteur fait avancer une fois par tick. Appelé uniquement par le thread du moteur.
 */
public interface Agent {

    void update(long currentTick);
}
