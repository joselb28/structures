# Guide étudiant — CarNBus

CarNBus simule une ville où des **bus** et des **Uber** transportent des **clients**. Le code est
plus gros que ce que vous avez l'habitude de lire. Vous travaillez **en équipe de 3**, en cinq temps :

| # | Étape | Document | Points |
|---|-------|----------|--------|
| 1 | **Expérimenter** : jouer avec les scénarios et trouver les limites du modèle | [`SIMULATION.md`](SIMULATION.md) | 15 |
| 2 | **Explorer** : lire le code paquetage par paquetage | [`EXPLORATION.md`](EXPLORATION.md) | 20 |
| 3 | **Développer** : au moins 3 questions par niveau (4 ou 5 pour viser plus haut) | [`QUESTIONS.md`](QUESTIONS.md) | 36 |
| 4 | **Tester et déboguer** : 3 tests unitaires et un bug documenté | [`TESTS.md`](TESTS.md) | 14 |
| 5 | **Contribuer** : un historique Git régulier, pour chaque membre | — | 15 |

Le détail de la correction est dans le [barème](BAREME.md).

**Lancer** : ouvrez le projet dans IntelliJ, puis cliquez sur ▶ à côté de `main` dans
`src/main/java/com/transit/Main.java`. Une fenêtre vous fait choisir un scénario.

---

## 1. Expérimenter

Lancez la simulation et observez :

- les **pastilles jaunes** (clients qui attendent un bus) et **vertes** (clients qui attendent un Uber) ;
- les bus qui deviennent rouges (**FULL**) et leur jauge d'occupation ;
- les **cases des Uber** (vert libre, contour orange en route, orange plein client à bord) et la
  **file d'attente Uber** juste en dessous ;
- le **rythme du moteur** : les ticks réellement calculés pendant la dernière seconde, comparés au
  rythme visé. S'il reste sous la cible, le moteur n'arrive plus à suivre.

Puis faites tourner les scénarios et répondez aux questions de discussion de
[`SIMULATION.md`](SIMULATION.md), y compris la section sur les **limites du modèle**. Créez au moins
un scénario à vous.

---

## 2. Explorer

Chaque paquetage a son `README.md` (dans `src/main/java/com/transit/<paquetage>/`). Lisez-les dans
l'ordre de [`EXPLORATION.md`](EXPLORATION.md), du plus simple au plus intégré, et répondez aux questions.

Astuce : chaque test (`src/test/java/...`) est un **exemple d'utilisation** de sa classe.
Lire `BusTest` vous en apprend autant que lire `Bus`.

---

## 3. Développer

[`QUESTIONS.md`](QUESTIONS.md) propose **15 questions**, 5 par niveau (★, ★★, ★★★). L'équipe en fait
**au moins 3 par niveau** (chaque membre au moins une par niveau) ; en faire 4 ou 5 par niveau élève
le plafond de la note (80 %, 90 %, 100 %). Chaque question :

- commence à un `// TODO Qn` dans le code (fenêtre **TODO** d'IntelliJ) ;
- se code dans **une nouvelle classe ou une nouvelle méthode** ;
- s'affiche, si elle a un résultat à montrer, dans la boîte **« Vos fonctionnalités »** du tableau de bord.

| Mécanisme | Questions |
|-----------|-----------|
| Structures de données (`HashMap`, `ArrayDeque`, `PriorityQueue`, tableau 2D…) | Q2, Q3, Q6, Q7, Q8, Q12, Q15 |
| Concurrence (`AtomicLong`, `ConcurrentLinkedQueue`, `ExecutorService`) | Q1, Q11, Q13 |
| Base de données (JDBC : écrire une table, interroger en SQL) | Q4, Q9 |
| Graphes (listes d'adjacence, parcours en largeur, génération) | Q5, Q10, Q14, Q15 |

---

## 4. Tester et déboguer

Après le développement : un test unitaire par membre, dans trois paquetages différents, puis un bug
à chercher, documenter et corriger. Tout est expliqué dans [`TESTS.md`](TESTS.md).

---

## 5. Contribuer

L'historique Git de chaque membre est évalué individuellement : des commits **réguliers**, **petits**
et **bien nommés** (« Ajoute le cache des itinéraires (Q7) » plutôt que « modifs »).

---

## Ce que le code essaie de vous montrer

### Le code parle le langage du métier
On écrit le code avec les mots du domaine, pour qu'un expert du transport puisse presque le lire.

| Métier (FR) | Code (EN) | Où |
|-------------|-----------|----|
| trajet | `trip` (liste de `Ride`) | `trip` |
| course en bus / en Uber | `BusRide` / `UberRide` | `trip` |
| arrêt, correspondance, terminus | `isStop`, `isTransferStop`, `terminals` | `model/CityMap` |
| monter, descendre | `tryBoard()`, `alight()` | `agent/Bus` |
| prendre en charge, déposer | `tryClaim()`, `pickedUp()`, `droppedOff()` | `agent/Customer` |
| heure de pointe | `rushHour` | `engine/CustomerSpawner` |

### Quelques règles de « clean code » appliquées ici
- **Une classe = une idée.** `BusNetwork` *construit* le graphe des bus, `TripPlanner` *y cherche*
  un chemin. Les classes font moins de ~150 lignes.
- **Composition plutôt qu'héritage.** `Bus` et `Uber` *possèdent* un `Movement` ; il n'y a pas de
  classe `Vehicle` dont ils hériteraient.
- **Données immuables.** `CityMap`, les `record` (`BusRide`, `Snapshot`, `VehicleView`) ne changent
  jamais après leur création : on peut les partager entre threads sans risque.
- **Pas de nombre magique.** Chaque valeur a un nom : un réglage (`simulation.properties`) ou une
  constante commentée (`CONSTANTES.md`).
- **Dépendances à sens unique.** `model` ne connaît personne ; `ui` connaît le moteur, jamais l'inverse.
  Le moteur se teste donc sans fenêtre.
- **Les tests servent de spécification.** Un test porte un nom qui est une phrase :
  `eachCustomerIsClaimedByExactlyOneUber`, `nobodyStartsAtALineTerminal`.
