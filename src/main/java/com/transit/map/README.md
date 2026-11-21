# `map` — génération de la ville et base SQLite

Fabrique la ville au démarrage, puis la **sauvegarde dans SQLite** pour les lancements suivants.
Rien ici n'est appelé pendant la simulation : « SQLite pour le froid, la RAM pour le chaud ».

| Classe | Rôle |
|--------|------|
| `RoadLayout` | Place les intersections (échantillonnage de Poisson) et les relie (graphe de Gabriel : routes qui ne se croisent jamais hors d'une intersection). |
| `BusLinePlanner` | Trace les lignes de bus : plus courts chemins entre intersections éloignées, en évitant de réutiliser les mêmes routes. |
| `MapGenerator` | Orchestre les deux étapes, puis découpe chaque route en tronçons de 40 px (1 tronçon = 1 minute). |
| `MapStore` | Lit la ville dans `transit.db`, ou la génère et l'y écrit si les réglages ont changé (empreinte stockée dans la table `meta`). |

## À retenir
- **Même graine + mêmes réglages = même ville.** Changer une clé `city.*` du scénario régénère la carte.
- Le schéma SQLite : `meta`, `nodes`, `edges`, `bus_lines`, `bus_line_nodes` (une requête par table).

## Dépend de
`model`, `graph`, `settings`.
