# `model` — les données de la ville

Ce que la simulation sait de la ville, **immuable** : une fois chargée, la carte ne change plus.
C'est pour ça que le moteur et l'interface peuvent la lire en même temps sans aucune synchronisation.

| Classe | Rôle métier |
|--------|-------------|
| `Node` | Un point du réseau routier : une **intersection** ou un **point de route** (entre deux intersections). Son `id` est aussi son indice dans la carte. |
| `Edge` | Un tronçon de route entre deux nœuds voisins (non orienté, toujours `a < b`). |
| `BusLine` | Une ligne de bus numérotée : la suite des nœuds qu'elle parcourt, d'un **terminus** à l'autre. |
| `CityMap` | La ville entière : nœuds, voisins de chaque nœud, lignes, arrêts, terminus. |
| `Time` | Les règles du temps simulé : 1 nœud traversé = 1 minute = 10 ticks. |

## Vocabulaire
- **Arrêt** : une intersection où au moins une ligne s'arrête (`isStop`).
- **Correspondance** : un arrêt desservi par 2 lignes ou plus (`isTransferStop`).
- **Terminus** : premier ou dernier arrêt d'une ligne (`terminals`).

## Structures de données à observer
- `int[][] neighbors` : un tableau de voisins **par nœud** (liste d'adjacence). Regardez
  `buildAdjacency` : deux passes, une pour compter, une pour remplir.
- `Map<Integer, Set<Integer>>` : arrêt → lignes qui s'y arrêtent.

## Dépend de
Rien. Tous les autres paquetages dépendent de lui.
