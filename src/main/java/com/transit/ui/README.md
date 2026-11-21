# `ui` — l'interface Swing

| Classe | Rôle |
|--------|------|
| `MainWindow` | La fenêtre : la carte au centre, le tableau de bord à droite. Un `javax.swing.Timer` redessine ~60 fois par seconde, indépendamment du moteur. |
| `SimulationPanel` | Dessine les véhicules et les pastilles d'attente (jaune = bus, vert = Uber) par-dessus l'image de la ville. Interpole la position des véhicules entre deux ticks. |
| `MapRenderer` | Dessine la ville (routes, lignes, arrêts) **une seule fois** dans une image, refaite seulement si la fenêtre change de taille. |
| `FaresSection` | Les tarifs du scénario et les recettes par mode. |
| `VehicleRenderer` | Dessine un bus ou un Uber. |
| `UberStatus` | Les deux drapeaux d'un Uber (réservé, client à bord) tels qu'affichés. |
| `DashboardPanel` | Heure simulée, rythme du moteur, boutons pause / vitesse / heure de pointe, statistiques. |
| `ScenarioChooser` | La fenêtre de lancement : choisir un scénario fourni ou un fichier. |
| `UberQueuePanel` | La file d'attente des Uber : combien de clients, et les 5 qui attendent depuis le plus longtemps. |
| `StudentPanel` | La boîte « Vos fonctionnalités » : là où les questions de développement affichent leurs résultats. |
| `FleetPanel` | L'état de chaque Uber : libre, en route vers le client, client à bord. |
| `Palette` | Toutes les couleurs, au même endroit. |

## La règle d'or
L'interface ne lit **que** le dernier `Snapshot` publié par le moteur. Elle ne touche jamais aux
agents : elle ne peut donc pas voir un monde « à moitié mis à jour ».

## Dépend de
`engine`, `agent`, `model`.
