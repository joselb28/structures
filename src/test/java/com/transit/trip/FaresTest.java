package com.transit.trip;

import com.transit.model.Time;
import com.transit.settings.Settings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FaresTest {

    @Test
    void dollarsFromTheScenarioBecomeCents() {
        Fares fares = Fares.of(new Settings.Fares(3.50, 3.00, 0.60));
        assertEquals(new Fares(350, 300, 60), fares);
    }

    @Test
    void anUberRideCostsTheBasePlusEachMinuteInTheCar() {
        Fares fares = new Fares(350, 300, 60);
        assertEquals(300, fares.uberRideCents(0));
        assertEquals(300 + 10 * 60, fares.uberRideCents(10L * Time.TICKS_PER_SIM_MINUTE));
    }

    @Test
    void partialMinutesAreRoundedToTheCent() {
        Fares fares = new Fares(350, 300, 60);
        assertEquals(300 + 30, fares.uberRideCents(Time.TICKS_PER_SIM_MINUTE / 2));
    }
}
