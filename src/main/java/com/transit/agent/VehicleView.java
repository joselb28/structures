package com.transit.agent;

/**
 * « Photo » immuable d'un véhicule à la fin d'un tick, qu'on peut donner sans risque au thread de l'interface.
 *
 * @param line      numéro de la ligne de bus, 0 pour un Uber
 * @param previousX position à la fin du tick précédent (pour l'interpolation)
 * @param available peut prendre un nouveau client : un bus avec une place libre, un Uber sans client
 */
public record VehicleView(String id, Kind kind, int line,
                          double previousX, double previousY, double x, double y,
                          int passengers, int capacity, boolean available) {

    public enum Kind { BUS, UBER }

    public boolean isFull() {
        return passengers >= capacity;
    }
}
