# Questions d'exploration

Ces questions se répondent **en lisant le code et en le faisant tourner**, sans rien modifier.
Elles comptent pour 20 % (voir le [barème](BAREME.md)). Répondez en équipe, avec vos mots, en citant
les classes et les méthodes (ex. `Bus.tryBoard()`). Quand une question demande une expérience, notez
ce que vous avez fait et ce que vous avez observé.

Outils utiles dans IntelliJ : *Ctrl+B* (aller à la déclaration), *Alt+F7* (trouver les utilisations),
*Ctrl+F12* (structure du fichier), le débogueur et ses points d'arrêt.

---

## 1. `model` — les données de la ville
1. Comment trouve-t-on les voisins d'un nœud ? Décrivez la structure de `CityMap.neighbors` et
   expliquez les deux passes de `buildAdjacency`. Pourquoi ne pas utiliser une `List<List<Integer>>` ?
2. `CityMap` n'a aucun *setter* et ses listes sont copiées avec `List.copyOf`. Qu'est-ce que ça
   garantit ? Quel autre paquetage en profite, et sur quel thread ?
3. Quelle est la différence entre un **arrêt**, un **arrêt de correspondance** et un **terminus** ?
   Pour chacun, quelle méthode de `CityMap` le reconnaît ?
4. *Expérience* : avec le débogueur, arrêtez-vous à la fin du constructeur de `CityMap` dans le
   scénario par défaut. Combien de nœuds ? Quel est le degré du nœud 0 ?

## 2. `settings` — les réglages
1. Où sont les valeurs par défaut ? Que se passe-t-il, pas à pas, quand on choisit `gros-bus` dans la
   fenêtre de lancement (de `ScenarioChooser` jusqu'à `Settings.from`) ?
2. Pourquoi les scénarios sont-ils lus comme des **ressources** (`getResourceAsStream`) plutôt que
   comme des fichiers ? Qu'est-ce que ça change quand on lance avec le bouton Play ?
3. *Expérience* : créez un scénario avec `fleet.busCapacity = 0`. Que se passe-t-il ? Quelle classe
   l'a refusé, et pourquoi est-ce mieux qu'un plantage plus tard dans la simulation ?

## 3. `graph` — Dijkstra générique
1. Expliquez le rôle de la `PriorityQueue` dans `ShortestPath.find`. Que contient chaque entrée ?
2. Que sont les entrées « périmées » (*stale*) ? Pourquoi les ignorer plutôt que de les retirer de la file ?
3. `ShortestPath` ne parle jamais de bus ni d'Uber. Trouvez ses **trois** utilisations dans le projet
   et, pour chacune, dites ce qu'est un nœud et ce que coûte un pas.
4. Quelle est la complexité de `find` en fonction du nombre de nœuds et de pas ?

## 4. `trip` — les trajets
1. Qu'est-ce qu'un `Boarding` ? Pourquoi le nœud du graphe de recherche est-il une paire
   (arrêt, ligne) et pas simplement un arrêt ? Donnez un exemple où ça change le résultat.
2. Comment `TripPlanner.toRides` transforme-t-il une suite de `Boarding` en une liste de `BusRide` ?
3. Pourquoi `Ride` est-elle une interface `sealed` ? Où le projet profite-t-il du `switch` sur ses
   deux implémentations ?
4. Pourquoi les tarifs (`Fares`) sont-ils en **cents** dans un `long` ? Donnez un exemple d'erreur
   que causerait un `double`.

## 5. `agent` — les acteurs
1. Que fait un `Bus` quand il arrive à un terminus ? Suivez `update` et `departToNextNode`.
2. Dessinez la machine à états d'un `Customer` (`WAITING`, `ASSIGNED`, `IN_TRANSIT`, `ARRIVED`) :
   quelles méthodes font passer d'un état à l'autre, et qui les appelle (le client ou l'Uber) ?
3. `Bus` et `Uber` possèdent un `Movement` au lieu d'hériter d'une classe `Vehicle`. Qu'est-ce que la
   composition apporte ici ? Qu'est-ce qu'on perdrait avec l'héritage ?
4. Expliquez la boucle de `Bus.tryBoard()`. Que se passerait-il avec `if (passengers < capacity) passengers++;`
   si deux clients montent en même temps ? Faites le scénario pas à pas.
5. Même question pour `Customer.tryClaim()` et deux Uber.
6. À quoi servent `VehicleView` et `CustomerView` ? Pourquoi l'interface ne lit-elle pas directement les `Bus` ?

## 6. `metrics` — les compteurs
1. Pourquoi `compteur++` sur un `long` perd-il des mises à jour quand plusieurs threads l'exécutent ?
   Décomposez l'instruction en étapes.
2. Pourquoi `LongAdder` plutôt que `AtomicLong` ici ? Dans quel cas `AtomicLong` serait-il meilleur ?
3. *Expérience* : lancez `MetricsRaceTest` et `BoardingRaceTest`. Remplacez temporairement le
   `LongAdder` de `delivered` par un `long` et relancez : que voyez-vous ? (Remettez le code ensuite.)

## 7. `engine` — le moteur
1. Décrivez les 4 phases d'un tick dans `Simulation.tick()`. Lesquelles sont parallèles ? Pourquoi
   seulement celles-là ?
2. Comment la boucle de `Simulation.run()` garde-t-elle un rythme fixe ? Que fait-elle quand elle
   prend du retard (`MAX_LAG_NANOS`) ?
3. Comment le « rythme du moteur » du tableau de bord est-il mesuré ? Que signifie un rythme sous la cible ?
4. *Expérience* : posez un point d'arrêt dans `Customer.update` et lancez en mode Debug. Dans le
   panneau *Threads*, quels threads voyez-vous ? Lequel exécute ce code ?
5. Faites un tableau des structures partagées (`Bus.passengers`, `Customer.state`, les compteurs de
   `Metrics`, `Simulation.snapshot`, la vitesse et la pause) : pour chacune, quels threads **écrivent**,
   quels threads **lisent**, et quel mécanisme rend l'accès sûr (`AtomicInteger`, `AtomicReference`,
   `LongAdder`, `volatile`, immuabilité) ?
6. Le champ `snapshot` de `Simulation` est `volatile`, mais les objets `Snapshot` ne sont jamais
   verrouillés. Pourquoi est-ce suffisant ? Que pourrait voir l'interface si le champ n'était pas
   `volatile`, ou si `Snapshot` était modifiable ?

## 8. `ui` — l'interface
1. Pourquoi la ville est-elle dessinée **une seule fois** dans une image (`MapRenderer`) ? Que se
   passerait-il si on redessinait les 1 000 tronçons 60 fois par seconde ?
2. Comment `SimulationPanel` rend-il le mouvement fluide alors que le moteur ne tourne qu'à 20 ticks
   par seconde ? Trouvez le calcul d'`alpha`.
3. Qui appelle `DashboardPanel.refresh()`, sur quel thread, et à quelle fréquence ?
4. Où afficheriez-vous une nouvelle statistique ? Suivez le chemin complet : `Metrics` →
   `Snapshot.capture` → `Snapshot.Stats` → `StudentPanel`.

## 9. `map` — la génération et SQLite
1. Expliquez l'échantillonnage de Poisson de `RoadLayout.sampleIntersections`. Pourquoi les
   intersections ne forment-elles pas d'amas ?
2. En une phrase, quelle règle décide si deux intersections sont reliées par une route
   (`RoadLayout.gabrielGraph`) ? Lisez ensuite les tests `graphIsConnected` et
   `roadsNeverCrossOutsideIntersections` de `MapGeneratorTest` : que vérifient-ils, et comment ?
3. Quand `transit.db` est-il régénéré ? Qu'est-ce que l'« empreinte » stockée dans la table `meta` ?
4. Pourquoi la simulation ne fait-elle **aucun** accès à SQLite pendant qu'elle tourne ?

## 10. Questions transversales
1. Dessinez le graphe des dépendances entre les 9 paquetages. Pourquoi aucune flèche ne remonte-t-elle
   vers `ui` ?
2. Choisissez une classe que vous trouvez bien écrite et une que vous trouvez difficile à lire.
   Justifiez avec les règles de clean code de [`ETUDIANTS.md`](ETUDIANTS.md).
3. Si vous deviez ajouter un troisième mode de transport (vélo en libre-service), quelles classes
   faudrait-il créer ou modifier ?
