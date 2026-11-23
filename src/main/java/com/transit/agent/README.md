# `agent` — les acteurs de la simulation

Chaque acteur implémente `Agent.update(tick)` : le moteur l'appelle une fois par tick.

| Classe | Rôle métier |
|--------|-------------|
| `Movement` | La partie « véhicule » commune : avancer de nœud en nœud, 1 tronçon = 1 minute. `Bus` et `Uber` **possèdent** un `Movement` (composition, pas d'héritage). |
| `Bus` | Fait l'aller-retour sur sa ligne, s'arrête 1 minute à chaque arrêt. Gère ses places : `tryBoard()` / `alight()`. |
| `Uber` | Libre : réserve le client le plus proche ou erre au hasard. Occupé : va chercher le client puis le dépose ; s'arrête une minute à la prise en charge et au dépôt. |
| `Customer` | Suit son trajet course par course : attend, monte, descend, change de ligne. États : `WAITING`, `ASSIGNED` (un Uber arrive), `IN_TRANSIT`, `ARRIVED`. |
| `BusStops` | Quels bus sont arrêtés à quel arrêt pendant ce tick (`Map<Integer, List<Bus>>`). |
| `UberRequests` | Les clients qui attendent un Uber et que personne n'a encore réservés. |
| `VehicleView`, `CustomerView` | Des « photos » immuables d'un véhicule ou d'un client, pour l'interface. |

## Les trois races du projet vivent ici
1. `Bus.tryBoard()` : deux clients pour la dernière place → `AtomicInteger.compareAndSet`.
2. `Customer.tryClaim()` : deux Uber pour le même client → `AtomicReference.compareAndSet`.
3. (dans `metrics`) les compteurs globaux → `LongAdder`.

## Dépend de
`model`, `trip`, `metrics`.
