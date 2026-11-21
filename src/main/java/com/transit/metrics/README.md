# `metrics` — les compteurs du tableau de bord

`Metrics` compte les clients apparus, montés, arrivés, les correspondances, et cumule les
temps d'attente et de trajet. Les moyennes affichées en sont déduites.

## Pourquoi un paquetage à part ?
Ces compteurs sont modifiés **par plusieurs threads en même temps** (les clients sont évalués en
parallèle). Avec un simple `long`, `compteur++` perd des mises à jour : c'est la **race n° 3** du projet.
La correction utilise `LongAdder`, conçu pour beaucoup d'écritures concurrentes et peu de lectures.

`MetricsRaceTest` le prouve : il échoue avec des `long`, passe avec des `LongAdder`.

## Dépend de
`model` (pour convertir des ticks en minutes).
