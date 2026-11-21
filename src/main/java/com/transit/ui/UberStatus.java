package com.transit.ui;

import com.transit.agent.VehicleView;

/** Les deux drapeaux d'un Uber, tels qu'affichés : réservé pour un client, et client à bord. */
enum UberStatus {
    FREE("Libre"),
    ON_ITS_WAY("En route vers le client"),
    CUSTOMER_ON_BOARD("Client à bord");

    final String label;

    UberStatus(String label) {
        this.label = label;
    }

    static UberStatus of(VehicleView uber) {
        if (uber.available()) {
            return FREE;
        }
        return uber.passengers() > 0 ? CUSTOMER_ON_BOARD : ON_ITS_WAY;
    }
}
