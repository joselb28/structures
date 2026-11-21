package com.transit.agent;

import com.transit.engine.Simulation;
import com.transit.model.TestMaps;
import com.transit.model.Time;

import org.junit.jupiter.api.Test;

import static com.transit.model.Time.TICKS_PER_SIM_MINUTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovementTest {

    private final Movement movement = new Movement(TestMaps.street(), 0);

    @Test
    void crossingOneEdgeTakesOneSimulatedMinute() {
        movement.headTo(1);
        for (int t = 1; t < TICKS_PER_SIM_MINUTE; t++) {
            assertFalse(movement.step());
        }
        assertTrue(movement.step());
        assertTrue(movement.isIdle());
        assertEquals(1, movement.currentNode());
    }

    @Test
    void positionIsInterpolatedAndPreviousPositionIsKept() {
        movement.headTo(1);
        for (int t = 0; t < TICKS_PER_SIM_MINUTE / 2; t++) {
            movement.step();
        }
        assertEquals(20, movement.x(), 1e-9);
        assertEquals(20 - 40.0 / TICKS_PER_SIM_MINUTE, movement.previousX(), 1e-9);
    }

    @Test
    void cannotJumpToANonAdjacentNode() {
        assertThrows(IllegalArgumentException.class, () -> movement.headTo(3));
    }

    @Test
    void cannotChangeCourseMidEdge() {
        movement.headTo(1);
        movement.step();
        assertThrows(IllegalStateException.class, () -> movement.headTo(0));
    }
}
