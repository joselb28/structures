# Constantes internes du code

Ces constantes définissent **le modèle lui-même**. Contrairement aux réglages d'une simulation
(voir [`SIMULATION.md`](SIMULATION.md)), elles sont dans le code (`static final`) : les changer
demande de recompiler, et souvent de comprendre l'algorithme qui les utilise.

Chacune a un nom et un commentaire à sa déclaration : il n'y a pas de « nombre magique » dans le code.

## Le temps

| Constante | Valeur | Fichier | Rôle |
|-----------|--------|---------|------|
| `TICKS_PER_SIM_MINUTE` | 10 | `model/Time` | 1 minute simulée = 10 ticks. Un véhicule traverse un tronçon en 1 minute. |
| `STOP_DWELL_TICKS` | 10 | `model/Time` | Un bus reste 1 minute à chaque arrêt. Utilisé aussi pour estimer la durée des trajets. |
| `SEGMENT_LENGTH` | 40 px | `map/MapGenerator` | Longueur d'un tronçon : la distance parcourue en 1 minute. Définit l'échelle de vitesse de toute la simulation. |

*Pourquoi pas des réglages ?* Ils fixent l'unité de temps et de distance : tout le reste (vitesse,
durée des trajets, statistiques en minutes) en découle.

## La génération de la ville

| Constante | Valeur | Fichier | Rôle |
|-----------|--------|---------|------|
| `MARGIN` | 150 px | `map/RoadLayout` | Aucune intersection plus près du bord. |
| `SAMPLING_ATTEMPTS` | 30 000 | `map/RoadLayout` | Points tirés au hasard pour placer les intersections. Assez pour remplir la ville la plus dense permise. |
| `MIN_SPAN_OF_DIAGONAL` | 0.4 | `map/BusLinePlanner` | Les deux terminus d'une ligne sont au moins à 40 % de la diagonale de la ville l'un de l'autre. |
| `REUSE_PENALTY` | 3.0 | `map/BusLinePlanner` | Chaque ligne qui emprunte déjà une route la rend 3 fois plus « chère » pour la suivante : les lignes s'étalent. |
| `MAX_SHARED_ROADS` | 0.4 | `map/BusLinePlanner` | Une ligne candidate qui partage plus de 40 % de ses routes est rejetée. |
| `ATTEMPTS` | 200 | `map/BusLinePlanner` | Lignes candidates tirées avant d'abandonner (d'où « moins de lignes que demandé » dans une petite ville). |
| `MAX_LINE_NUMBER` | 99 | `map/BusLinePlanner` | Les numéros de ligne sont tirés entre 1 et 99. |
| `SCHEMA_VERSION` | 3 | `map/MapStore` | À augmenter quand on change le schéma SQLite ou l'algorithme de génération, pour forcer la régénération. |

## Les agents et le moteur

| Constante | Valeur | Fichier | Rôle |
|-----------|--------|---------|------|
| `Uber.CAPACITY` | 1 | `agent/Uber` | Un Uber transporte un client (ou un groupe) à la fois. |
| `ATTEMPTS` | 10 | `engine/CustomerSpawner` | Tirages (départ, arrivée) essayés avant de renoncer à faire apparaître un client. |
| `MAX_LAG_NANOS` | 0,25 s | `engine/Simulation` | Si le moteur prend plus de retard, il abandonne le rattrapage au lieu de s'emballer. |

## L'interface

| Constante | Valeur | Fichier | Rôle |
|-----------|--------|---------|------|
| `FRAME_MILLIS` | 16 ms | `ui/MainWindow` | ~60 images par seconde, quel que soit le rythme du moteur. |
| `START_HOUR` | 6 | `ui/DashboardPanel` | La journée simulée commence à 6 h. |
| `SPEEDS` | ×1, ×4, ×16 | `ui/DashboardPanel` | Les boutons de vitesse. |
| `UberQueuePanel.SHOWN` | 5 | `ui/UberQueuePanel` | Clients détaillés dans la file d'attente Uber. |
| `SCROLL_STEP` | 16 px | `ui/MainWindow` | Défilement du tableau de bord par cran de molette. |
| `BADGE_RADIUS`, `BADGE_OFFSET` | 13, 15 px | `ui/SimulationPanel` | Taille et position des pastilles d'attente. |

Les tailles de dessin (largeur d'un bus, épaisseur d'une route…) sont des détails de rendu, gardés
près du code qui dessine.
