# `engine` — le moteur de simulation

| Classe | Rôle |
|--------|------|
| `Simulation` | Possède tous les agents. Tourne sur son propre thread à `engine.ticksPerSecond` (× la vitesse choisie). |
| `CustomerSpawner` | Fait apparaître les clients : combien, où (jamais à un terminus), vers où (`terminusShare` vers une fin de ligne, `offNetworkShare` hors réseau en Uber), et planifie leur trajet. |
| `Snapshot` | Une photo immuable du monde à la fin d'un tick. C'est **la seule chose** que l'interface lit. |

## Un tick, en 4 phases
1. **Bus** : avancent, un par un.
2. **Uber** : en parallèle — les libres se disputent les clients (race n° 2).
3. **Clients** : en parallèle, en deux temps — **d'abord toutes les descentes, puis toutes les montées**
   (on laisse descendre avant de monter : une place libérée sert tout de suite). Races n° 1 et 3.
4. **Bilan** : on retire les arrivés, on fait apparaître les nouveaux, on publie un `Snapshot`.

Seules les phases 2 et 3 sont concurrentes : c'est voulu, pour que les races soient localisées.

## Dépend de
`agent`, `trip`, `metrics`, `model`, `settings`.
