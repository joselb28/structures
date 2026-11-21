package com.transit.agent;

import com.transit.trip.UberRide;

import java.util.ArrayList;
import java.util.List;

/**
 * Les clients qui attendent un Uber pendant ce tick et que personne n'a encore réservés.
 *
 * <p>Reconstruit par le thread du moteur avant la phase parallèle des Uber, puis seulement lu. Plusieurs
 * Uber peuvent voir le même client : {@link Customer#tryClaim()} décide qui l'obtient.
 */
public final class UberRequests {

    private List<Customer> open = List.of();

    // TODO Q6 (★★) : stratégie « premier arrivé, premier servi » avec une file (ArrayDeque) — voir docs/QUESTIONS.md
    public void refresh(List<Customer> customers) {
        List<Customer> requests = new ArrayList<>();
        for (Customer c : customers) {
            if (c.state() == Customer.State.WAITING && c.currentRide() instanceof UberRide) {
                requests.add(c);
            }
        }
        open = requests;
    }

    public List<Customer> open() {
        return open;
    }
}
