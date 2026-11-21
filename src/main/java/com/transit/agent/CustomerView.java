package com.transit.agent;

/**
 * « Photo » immuable d'un client en attente, pour l'interface.
 *
 * @param node l'endroit où le client attend
 * @param mode ce que le client attend
 * @param line la ligne de bus attendue, 0 s'il attend un Uber
 * @param waitingSinceTick tick depuis lequel il attend (pour afficher son temps d'attente)
 */
public record CustomerView(int id, int node, Mode mode, int line, long waitingSinceTick) {

    public enum Mode { BUS, UBER }
}
