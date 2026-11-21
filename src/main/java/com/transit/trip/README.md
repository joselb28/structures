# `trip` — les trajets des clients

Un client fait un **trajet** (*trip*), composé de **courses** (*rides*) :

```
Ride  =  BusRide(ligne, arrêt de montée, arrêt de descente)
      |  UberRide(prise en charge, dépôt)
```

| Classe | Rôle |
|--------|------|
| `Ride`, `BusRide`, `UberRide` | Une course. `Ride` est une interface *scellée* : il n'existe que ces deux sortes. |
| `BusNetwork` | Le réseau de bus vu comme un graphe. Un nœud est un `Boarding` : « à cet arrêt, sur cette ligne ». On peut **rouler** jusqu'à l'arrêt suivant ou **changer de ligne** (coûte `customers.transferMinutes`). |
| `TripPlanner` | Trouve le trajet en bus le plus rapide (Dijkstra sur le `BusNetwork`), puis regroupe les arrêts consécutifs d'une même ligne en une seule `BusRide`. |
| `Roads` | La route la plus rapide sur le réseau routier, pour les Uber. |

## Pourquoi un nœud (arrêt, ligne) et pas juste un arrêt ?
Parce qu'à un même arrêt, être dans la ligne 14 ou dans la ligne 62 n'est pas la même situation :
passer de l'une à l'autre coûte une correspondance. C'est l'idée clé de la recherche d'itinéraire.

## Dépend de
`model`, `graph`.
