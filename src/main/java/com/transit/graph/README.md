# `graph` — algorithmes de graphe génériques

Une seule classe, `ShortestPath` : l'**algorithme de Dijkstra**, écrit une fois et réutilisé partout.

Elle ne connaît rien au transport : on lui donne
1. des nœuds de départ,
2. une condition d'arrivée,
3. une fonction qui donne les **pas** possibles depuis un nœud (`Step` : voisin + coût).

| Utilisé par | Nœuds | Coût d'un pas |
|-------------|-------|---------------|
| `map.BusLinePlanner` | intersections | longueur de la route (plus cher si déjà desservie) |
| `trip.TripPlanner` | `Boarding` (arrêt, ligne) | minutes de trajet ou de correspondance |
| `trip.Roads` | tous les nœuds | 1 minute par tronçon (trajets des Uber) |

## Structures de données à observer
- `PriorityQueue` : le prochain nœud à explorer est toujours le moins cher.
- `Map<N, Double>` (meilleur coût connu) et `Map<N, N>` (prédécesseur) pour reconstruire le chemin.
- Les entrées « périmées » de la file sont ignorées plutôt que retirées (`stale entry`).

## Dépend de
Rien.
