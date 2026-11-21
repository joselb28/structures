# Questions de développement

15 questions, 5 par niveau de difficulté. L'équipe en fait **au moins 3 par niveau** : chaque membre
prend au moins une question par niveau. Faire **4** ou **5** questions par niveau élève le plafond de
la note de développement (80 %, 90 %, 100 % : voir le [barème](BAREME.md)). Chaque question
cible un mécanisme précis : une structure de données, la concurrence (threads), la base de données
(JDBC) ou la génération du graphe. La correction suit le [barème](BAREME.md).

## Comment lire une question

Toutes les questions suivent le même plan :

| Rubrique | Ce qu'elle vous dit |
|----------|---------------------|
| **Contexte** | Pourquoi la question existe, dans la simulation. |
| **Où** | Les fichiers à lire et à modifier, et le `// TODO Qn` où la question commence dans le code. |
| **Ce que vous devez faire** | Les étapes, dans l'ordre. |
| **Contraintes** | Ce qui est obligatoire. |
| **Livrables** | Ce que vous remettez. |
| **Pour vérifier** | Comment vous assurer que votre réponse marche. |
| **Indices** et **Pièges** | Pour vous débloquer, et les erreurs fréquentes. |

## Règles communes à toutes les questions

1. **Où coder ?** Chaque question a un commentaire `// TODO Qn` à l'endroit où elle commence (dans
   IntelliJ, la fenêtre **TODO**, en bas, les liste tous). Votre réponse est **une nouvelle classe ou
   une nouvelle méthode**, appelée à cet endroit. Gardez le commentaire `// TODO Qn` (remplacez
   `TODO` par `FAIT`) pour que l'enseignant retrouve votre code.
2. **Où afficher ?** Ce qui doit être vu va dans la boîte **« Vos fonctionnalités »** du tableau de
   bord (`ui/StudentPanel`, qui contient un `TODO` pour chaque question à afficher), sur la carte ou
   dans la console, selon la question.
3. **Le chemin d'une donnée jusqu'à l'écran** : le moteur calcule (thread du moteur) → la donnée est
   copiée dans le `Snapshot` (`Snapshot.capture`) → l'interface la lit (`StudentPanel.refresh`).
   L'interface ne lit **jamais** directement les agents.
4. **Ne rien casser** : la simulation doit toujours se lancer avec le bouton Play, et tous les tests
   existants doivent rester verts (`mvn test` ou *Run 'All Tests'*).
5. **Réponse écrite** (5 à 10 lignes, dans votre rapport) : la structure ou le mécanisme utilisé,
   pourquoi, sa **complexité** (temps et mémoire), et le nom du membre qui a fait la question.
6. **Clean code** : noms du domaine, méthodes courtes, Javadoc en français sur ce qui n'est pas
   évident, pas de nombre magique (une constante nommée, ou une clé de scénario).

Écrire un test JUnit pour votre question est bienvenu, mais les tests sont évalués à part
([`TESTS.md`](TESTS.md)).

---

## Niveau ★

### Q1. La plus longue attente — *concurrence : `AtomicLong`*

**Contexte.** Le tableau de bord affiche l'attente **moyenne**, mais pas le **record** : la plus
longue attente d'un client avant de monter. Ce record est mis à jour pendant la phase des clients,
qui est **parallèle** : plusieurs threads peuvent vouloir le remplacer au même moment. Avec un simple
`long`, le code « si mon attente est plus grande que le record, je remplace le record » peut perdre
la bonne valeur : deux threads lisent l'ancien record en même temps, et le plus petit écrit en dernier.

**Où.** `metrics/Metrics` (`// TODO Q1`), `engine/Snapshot`, `ui/StudentPanel`.

**Ce que vous devez faire.**
1. Dans `Metrics`, ajoutez un champ `AtomicLong longestWaitTicks`.
2. Dans `recordBoarding(long waitTicks)` (appelée à chaque montée, en parallèle), mettez le record à
   jour **de façon atomique** : `longestWaitTicks.accumulateAndGet(waitTicks, Math::max)`.
3. Ajoutez une méthode `longestWaitMinutes()` (conversion avec `Time.ticksToMinutes`).
4. Ajoutez la valeur à `Snapshot.Stats` (dans `Snapshot.capture`) et affichez-la dans `StudentPanel` :
   « Plus longue attente : 42 min ».

**Contraintes.** Aucun `synchronized` ; le record ne doit jamais redescendre ni rater une valeur.

**Livrables.** Le code, et la réponse écrite : expliquez, pas à pas, comment deux threads peuvent
perdre le record avec un simple `long` ; ce que fait `accumulateAndGet` pour l'éviter (une boucle de
`compareAndSet`, comme `Bus.tryBoard`) ; pourquoi un `LongAdder` (utilisé pour les autres compteurs)
ne convient pas pour un maximum.

**Pour vérifier.** Inspirez-vous de `MetricsRaceTest` : 8 threads enregistrent chacun 100 000 attentes
au hasard, dont une seule vaut 999 999 ; à la fin, le record vaut exactement 999 999. À l'écran, le
record ne fait que monter.

**Indices.** `AtomicLong.get()`, `accumulateAndGet(x, Math::max)`.

**Pièges.** Faire `if (wait > record.get()) record.set(wait);` : deux opérations séparées, donc pas
atomiques (c'est exactement le bug à éviter).

---

### Q2. Les clients par ligne — *structure : `HashMap` de comptage*

**Contexte.** Le tableau de bord dit combien de clients attendent un bus, mais pas **quelle ligne**
ils attendent. C'est pourtant ce qu'un répartiteur voudrait savoir pour ajouter des bus.

**Où.** `engine/Snapshot` (`// TODO Q2`, dans `capture`), `ui/StudentPanel`.

**Ce que vous devez faire.**
1. Dans `Snapshot.capture`, en parcourant les clients en attente, comptez-les par ligne attendue
   dans une `Map<Integer, Integer>` (numéro de ligne → nombre de clients). Les clients qui attendent
   un Uber ne comptent pas.
2. Ajoutez cette table au `Snapshot` (copiée avec `Map.copyOf` pour qu'elle soit immuable).
3. Dans `StudentPanel`, affichez une ligne par ligne de bus, **triée par numéro** : « Ligne 14 : 6 clients ».

**Contraintes.** Un seul parcours des clients ; le comptage se fait sur le thread du moteur, pas dans l'interface.

**Livrables.** Le code, et la réponse écrite : complexité du comptage ; quelle structure donne
directement un affichage trié (`TreeMap`) et à quel coût ?

**Pour vérifier.** La somme des valeurs est égale au nombre de clients qui attendent un bus. Dans
le scénario `terminus-populaires`, les lignes qui desservent beaucoup de terminus ressortent.

**Indices.** `map.merge(ligne, 1, Integer::sum)` ; `CustomerView.line()` et `CustomerView.mode()`.

**Pièges.** Oublier qu'une ligne sans client en attente n'apparaît pas dans la table (affichez 0 ou rien, mais choisissez).

---

### Q3. Le taux d'occupation des bus — *structure : parcours de `List`, agrégation*

**Contexte.** Un bus plein refuse des clients, un bus vide coûte cher pour rien. Le taux
d'occupation moyen résume l'efficacité de la flotte.

**Où.** `engine/Snapshot` (`// TODO Q3`, dans `capture`), `ui/StudentPanel`.

**Ce que vous devez faire.**
1. Dans `Snapshot.capture`, calculez à partir des `VehicleView` des bus :
   - le **taux d'occupation moyen** = somme des passagers ÷ somme des places ;
   - le **bus le plus chargé** (son identifiant et son occupation, ex. « 14A : 5/5 »).
2. Ajoutez ces deux informations au `Snapshot`.
3. Affichez-les dans `StudentPanel` : « Occupation moyenne : 63 % — plus chargé : 14A (5/5) ».

**Contraintes.** Un seul parcours de la liste des véhicules ; ne comptez que les bus (pas les Uber).

**Livrables.** Le code, et la réponse écrite : pourquoi ce calcul se fait-il dans `Snapshot.capture`
(thread du moteur) et pas dans l'interface ? Pourquoi « somme des passagers ÷ somme des places » et
pas « moyenne des taux de chaque bus » ? (Ces deux calculs donnent-ils le même résultat ?)

**Pour vérifier.** Le taux est entre 0 et 100 %. Avec `gros-bus` (40 places), il est bien plus bas
qu'avec le scénario par défaut (5 places).

**Indices.** `VehicleView.kind()`, `passengers()`, `capacity()`.

**Pièges.** La division entière (`int / int` donne 0) ; une liste de bus vide (division par zéro).

---

### Q4. Enregistrer la séance — *JDBC : `CREATE TABLE`, `PreparedStatement`*

**Contexte.** Une séance de simulation se perd quand on ferme la fenêtre. Pour comparer les scénarios,
on veut garder un résumé de la séance dans la base `transit.db`, en cliquant sur un bouton. Règle du
projet : SQLite seulement « à froid », jamais dans la boucle de simulation ; un clic de l'utilisateur
est un bon moment.

**Où.** `ui/DashboardPanel` (`// TODO Q4`), une nouvelle classe `metrics/RunLog`.
**Modèle à suivre : `map/MapStore`**, qui fait déjà tout ce qu'il faut (ouvrir la base, créer des
tables, insérer avec un `PreparedStatement`).

**Ce que vous devez faire.**
1. Créez la classe `RunLog` avec une méthode `static void save(Path dbFile, long seed, Snapshot snapshot)`.
2. Elle crée la table si elle n'existe pas, exactement avec ce schéma :
   ```sql
   CREATE TABLE IF NOT EXISTS run_stats (
       id                INTEGER PRIMARY KEY AUTOINCREMENT,
       saved_at          TEXT    NOT NULL,   -- date et heure de l'enregistrement
       seed              INTEGER NOT NULL,
       simulated_minutes INTEGER NOT NULL,
       delivered         INTEGER NOT NULL,
       average_wait      REAL    NOT NULL,   -- en minutes simulées
       revenue_cents     INTEGER NOT NULL    -- recettes bus + Uber
   )
   ```
3. Elle insère **une ligne** avec un `PreparedStatement` : `INSERT INTO run_stats (saved_at, seed, …)
   VALUES (?, ?, …)`, en remplissant chaque `?` avec `setString`, `setLong` ou `setDouble`.
4. Dans `DashboardPanel`, ajoutez un bouton **« Enregistrer la séance »** qui appelle
   `RunLog.save(Path.of("transit.db"), simulation.seed(), simulation.snapshot())` (ajoutez l'accesseur
   `seed()` à `Simulation`, qui la reçoit dans ses `Settings`) et affiche un message de confirmation
   (`JOptionPane.showMessageDialog`).

**Contraintes.** `try`-with-resources pour la connexion et la requête ; aucune valeur concaténée dans le
SQL ; le bouton ne bloque pas le moteur (la simulation continue de tourner).

**Livrables.** Le code, et la réponse écrite : pourquoi enregistrer sur un clic plutôt qu'à chaque
tick ? Pourquoi un `PreparedStatement` plutôt qu'une chaîne SQL construite avec `+` (pensez à une valeur
qui contiendrait une apostrophe) ?

**Pour vérifier.** Cliquez deux fois, puis comptez les lignes : un petit test qui relit la table, ou
l'onglet *Database* d'IntelliJ ouvert sur `transit.db`. Dans un test, utilisez une base temporaire avec
`@TempDir`, comme `MapStoreTest`.

**Indices.** Copiez la structure de `MapStore.save` : `DriverManager.getConnection("jdbc:sqlite:" + dbFile)`,
puis `conn.createStatement().execute(...)` pour la table et `conn.prepareStatement(...)` pour l'insertion.
`LocalDateTime.now().toString()` donne la date.

**Pièges.** `MapStore.save` efface les tables qu'il connaît quand la carte est régénérée : votre table
`run_stats` n'en fait pas partie, ne l'ajoutez pas à sa liste.

---

### Q5. Portrait du graphe — *graphe : listes d'adjacence `int[][]`*

**Contexte.** La ville est un graphe : les nœuds sont les intersections et les points de route, les
arêtes sont les tronçons. Quelques chiffres suffisent à décrire sa forme.

**Où.** `model/CityMap` (`// TODO Q5`), affichage au démarrage dans `Main` (console).

**Ce que vous devez faire.** Ajoutez à `CityMap` une ou plusieurs méthodes qui calculent, pour les
**intersections** seulement (pas les points de route) :
1. le **degré moyen** (nombre moyen de voisins) ;
2. l'**intersection la plus connectée** et son degré ;
3. le nombre de **culs-de-sac** (degré 1).

Affichez le résultat dans la console au démarrage, après la ligne `[Carte] …`.

**Contraintes.** Utilisez `neighbors(id)` (le tableau de voisins) ; un seul parcours des intersections.

**Livrables.** Le code, et la réponse écrite : pourquoi la ville est-elle stockée en **listes
d'adjacence** plutôt qu'en **matrice d'adjacence** ? Comparez la mémoire des deux pour 811 nœuds et
environ 900 tronçons.

**Pour vérifier.** Sur `TestMaps.street()` (dessinée dans sa Javadoc), comparez avec un calcul à la
main. Le degré d'un point de route est toujours 2 : c'est pour ça qu'on ne le compte pas.

**Indices.** `intersectionIds()`, `neighbors(id).length`.

**Pièges.** Compter les points de route fausse complètement le degré moyen.

---

## Niveau ★★

### Q6. Premier arrivé, premier servi — *structure : file `ArrayDeque`*

**Contexte.** Aujourd'hui, un Uber libre prend le client **le plus proche**. C'est efficace, mais un
client loin de tout peut attendre très longtemps : la file d'attente du tableau de bord le montre.

**Où.** `agent/UberRequests` (`// TODO Q6`), `agent/Uber` (`claimNearestRequest`), `settings`.

**Ce que vous devez faire.**
1. Ajoutez une seconde stratégie de dispatch : servir le client qui attend **depuis le plus
   longtemps**, dans l'ordre d'arrivée (une file FIFO).
2. Dans `UberRequests`, gardez les demandes dans une `ArrayDeque<Customer>` : une nouvelle demande va
   à la fin, l'Uber libre prend celle du début.
3. Ajoutez une clé de scénario `fleet.uberDispatch = nearest | fifo` (dans `simulation.properties`,
   avec un commentaire, et dans `Settings`) pour choisir la stratégie.

**Contraintes.** La stratégie « le plus proche » doit rester disponible et rester la valeur par
défaut. Plusieurs Uber peuvent toujours viser le même client : gardez `tryClaim()`.

**Livrables.** Le code, et la réponse écrite : comparez l'**attente moyenne** et la **plus longue
attente** (visible dans la file d'attente) des deux stratégies sur `flotte-uber` après 10 h
simulées ; complexité de chaque stratégie pour un Uber libre.

**Pour vérifier.** Trois clients qui demandent un Uber dans un ordre connu sont servis dans cet ordre
avec `fifo`.

**Indices.** `ArrayDeque.addLast`, `peekFirst`, `pollFirst` ; `Customer.waitingSince()`.

**Pièges.** `UberRequests` est reconstruit à chaque tick : la file doit être reconstruite dans
l'ordre de `waitingSince()`, ou bien conservée d'un tick à l'autre (et alors retirer les clients
réservés). Choisissez et justifiez.

---

### Q7. Mémoriser les itinéraires — *structure : cache `HashMap` (mémoïsation)*

**Contexte.** À chaque nouveau client, `TripPlanner.planByBus` relance Dijkstra, même si le trajet
« arrêt A → arrêt B » a déjà été calculé mille fois. Il n'y a pourtant qu'environ 51 × 51 paires
d'arrêts dans la ville par défaut.

**Où.** `trip/TripPlanner` (`// TODO Q7`).

**Ce que vous devez faire.**
1. Créez une petite clé immuable pour une paire (départ, arrivée), par exemple
   `record StopPair(int origin, int destination)`.
2. Gardez les résultats dans une `Map<StopPair, List<BusRide>>`.
3. Dans `planByBus`, renvoyez le résultat mémorisé s'il existe ; sinon, calculez-le, mémorisez-le, renvoyez-le.

**Contraintes.** `planByBus` doit renvoyer exactement les mêmes trajets qu'avant ; la liste renvoyée
doit rester immuable.

**Livrables.** Le code, et la réponse écrite : combien de paires au maximum ? Combien de mémoire ?
Le cache doit-il être thread-safe ici (qui appelle `planByBus`, et sur quel thread) ? Mesurez le gain :
temps passé dans `planByBus` sur 10 000 appels, avant et après.

**Pour vérifier.** Deux appels identiques renvoient le même résultat, et le second ne relance pas
Dijkstra (comptez les appels à Dijkstra, ou mesurez le temps). `TripPlannerTest` reste vert.

**Indices.** `Map.computeIfAbsent` ; un `record` a déjà un bon `equals`/`hashCode`.

**Pièges.** Utiliser `int[]` comme clé de `Map` (son `equals` compare les adresses, pas le contenu).

---

### Q8. Les clients qui abandonnent — *structure : `PriorityQueue` par échéance*

**Contexte.** Dans la vraie vie, un client qui attend trop longtemps abandonne. Le modèle actuel le
laisse attendre pour toujours.

**Où.** `engine/Simulation` (`// TODO Q8`, dans la phase de bilan), `agent/Customer`, `metrics/Metrics`,
`ui/StudentPanel`, `settings`.

**Ce que vous devez faire.**
1. Ajoutez une clé de scénario `customers.patienceMinutes` (plage raisonnable : 10 à 600).
2. Ajoutez un état `GAVE_UP` au client, et une méthode qui le fait abandonner **seulement s'il attend
   encore** (un client déjà en route ou réservé par un Uber n'abandonne pas).
3. Dans `Simulation`, gardez les clients en attente dans une `PriorityQueue` triée par **échéance**
   (tick d'apparition + patience). À chaque tick, retirez seulement ceux dont l'échéance est passée,
   faites-les abandonner, et retirez-les de la simulation.
4. Comptez les abandons dans `Metrics` et affichez-les dans `StudentPanel`.

**Contraintes.** Ne parcourez pas tous les clients à chaque tick pour trouver ceux qui abandonnent :
c'est le rôle de la file de priorité.

**Livrables.** Le code, et la réponse écrite : complexité par tick avec la file de priorité, comparée
à un parcours de tous les clients ; que se passe-t-il pour un client qui est monté avant son échéance ?

**Pour vérifier.** Avec `fleet.busCapacity = 2` et une patience courte, les abandons montent vite.
Un client qui monte avant son échéance n'abandonne pas.

**Indices.** `new PriorityQueue<>(Comparator.comparingLong(...))`, `peek()`, `poll()`.

**Pièges.** Un client sort de la file quand il monte, mais `PriorityQueue.remove(objet)` coûte O(n) :
il est plus simple de le laisser dans la file et de l'ignorer quand son échéance arrive (s'il n'attend
plus). Attention au changement d'état concurrent avec un Uber qui le réserve (pensez à `tryClaim`).

---

### Q9. Interroger la ville en SQL — *JDBC : `SELECT`, `JOIN`, `GROUP BY`, `ORDER BY`*

**Contexte.** Toute la ville est déjà enregistrée dans `transit.db` par `MapStore` : les tables
`nodes`, `edges`, `bus_lines` et `bus_line_nodes`. Une base de données sert aussi à **répondre à des
questions** : on va calculer des statistiques des lignes de bus directement en SQL.

**Où.** `Main` (`// TODO Q9`), une nouvelle classe, par exemple `map/CityReport`, `ui/StudentPanel`
(ou la console). Le schéma des tables est dans `MapStore.save`.

**Ce que vous devez faire.**
1. Écrivez une requête qui donne, **pour chaque ligne de bus, son nombre d'arrêts** : un arrêt est un
   nœud de type `'INTERSECTION'` de la ligne. Il faut relier `bus_line_nodes` à `nodes` :
   ```sql
   SELECT bln.line, COUNT(*) AS stops
   FROM bus_line_nodes bln JOIN nodes n ON n.id = bln.node
   WHERE n.type = 'INTERSECTION'
   GROUP BY bln.line
   ORDER BY stops DESC
   ```
2. Écrivez une seconde requête : **la longueur de chaque ligne** en nombre de nœuds (`COUNT(*)` sur
   `bus_line_nodes`, groupé par ligne), et gardez la plus longue (`ORDER BY … DESC LIMIT 1`).
3. Exécutez les deux requêtes au démarrage, transformez chaque ligne du `ResultSet` en un petit
   `record` Java, et affichez le résultat dans la console (ou dans `StudentPanel`).

**Contraintes.** Les comptes, les tris et la limite se font **dans le SQL**, pas en Java ; la lecture
se fait une seule fois, au démarrage, après `MapStore.loadOrGenerate`.

**Livrables.** Le code, et la réponse écrite : expliquez la jointure (pourquoi faut-il `nodes` pour
savoir ce qu'est un arrêt ?) et le `GROUP BY` ; pourquoi faire ces calculs en SQL plutôt qu'en Java ?

**Pour vérifier.** Le nombre de lignes affichées est égal au nombre de lignes de bus de la carte ; le
nombre d'arrêts d'une ligne est égal à celui qu'on obtient en Java avec `line.nodeIds()` filtrés par
`map.isStop`. Dans un test, enregistrez `TestMaps.cross()` dans une base temporaire (`@TempDir`) avec
`MapStore.save` (accessible depuis le paquetage `map`), puis vérifiez que les lignes 5 et 7 ont
3 et 4 arrêts.

**Indices.** `conn.prepareStatement(sql).executeQuery()`, puis `while (rs.next()) { rs.getInt("stops") … }`.

**Pièges.** Fermer le `ResultSet`, la requête et la connexion (`try`-with-resources) ; un nom de
colonne mal écrit ne provoque une erreur qu'à l'exécution.

---

### Q10. La zone desservie par un arrêt — *graphe : parcours en largeur (BFS) avec `ArrayDeque`*

**Contexte.** Jusqu'où peut-on aller en quelques minutes depuis un arrêt ? Un parcours en largeur
répond à cette question : il visite d'abord les nœuds à 1 tronçon, puis à 2, puis à 3, etc. Comme
chaque tronçon prend une minute, « K tronçons » veut dire « K minutes ».

**Où.** `ui/MapRenderer` (`// TODO Q10`), une nouvelle classe, par exemple `trip/ServiceArea`,
`ui/StudentPanel`.

**Ce que vous devez faire.**
1. Écrivez une méthode `int[] distancesFrom(CityMap map, int start)` qui fait un **BFS** depuis le
   nœud `start` et renvoie, pour chaque nœud, sa distance en tronçons (-1 s'il n'est pas atteint).
2. Choisissez l'arrêt de départ : l'arrêt desservi par **le plus de lignes** (`map.linesAt(id).size()`).
3. Comptez les intersections à **K tronçons ou moins** de cet arrêt (K est une constante nommée,
   par exemple 10) et affichez le résultat dans `StudentPanel` :
   « Zone de 10 min autour de l'arrêt 23 : 18 intersections ».
4. *Bonus* : dans `MapRenderer`, dessinez ces intersections d'une couleur distincte (ajoutée à `Palette`).

**Contraintes.** Une `ArrayDeque` comme file ; chaque nœud est ajouté à la file une seule fois ;
le calcul se fait une seule fois, au démarrage (la ville ne change pas).

**Livrables.** Le code, et la réponse écrite : pourquoi la file donne-t-elle les nœuds dans l'ordre de
leur distance ? Complexité en fonction du nombre de nœuds et de tronçons.

**Pour vérifier.** Sur `TestMaps.street()` en partant du nœud 0 (dessin dans la Javadoc) : le nœud 1
est à 1, le nœud 2 à 2, les nœuds 3 et 4 à 3. Avec K = 0, la zone ne contient que l'arrêt de départ.

**Indices.** Au départ, mettez `start` dans la file avec la distance 0 ; un tableau `int[]` rempli de -1
sert à la fois de distance et de « déjà visité » ; les voisins sont dans `map.neighbors(nœud)`.

**Pièges.** Marquer un nœud comme visité quand on le **retire** de la file au lieu de quand on
l'**ajoute** : il peut alors être ajouté plusieurs fois.

---

## Niveau ★★★

### Q11. Le journal des événements — *concurrence : `ConcurrentLinkedQueue`, producteurs et consommateur*

**Contexte.** On veut voir passer, dans le tableau de bord, les **derniers événements** de la ville :
« client 57 monte dans le 14A », « client 12 arrive à destination », « client 33 change de ligne à
l'arrêt 23 ». Ces événements naissent pendant la phase des clients, sur **plusieurs threads à la fois**
(les *producteurs*) ; le moteur les récupère ensuite sur son propre thread (le *consommateur*).

**Où.** `engine/Simulation` (`// TODO Q11`, phase de bilan), `agent/Customer`, une nouvelle classe
`engine/EventLog`, `engine/Snapshot`, `ui/StudentPanel`.

**Ce que vous devez faire.**
1. Créez un `record Event(long tick, String text)`.
2. Créez `EventLog` avec :
   - une `ConcurrentLinkedQueue<Event>` où les clients **publient** (`publish(Event)`), depuis n'importe quel thread ;
   - une méthode `drain()`, appelée par le moteur dans la phase de bilan, qui **vide** la file et garde
     les **10 derniers** événements dans une `ArrayDeque` (on retire le plus vieux quand on dépasse 10).
3. Donnez l'`EventLog` aux clients (comme `Metrics`) et publiez un événement à chaque montée, arrivée
   et correspondance (`Customer.tryToBoard`, `finishRide`).
4. Copiez les 10 derniers événements dans le `Snapshot` (une `List` immuable) et affichez-les dans
   `StudentPanel`, le plus récent en haut.

**Contraintes.** Les clients ne touchent **jamais** à l'`ArrayDeque` des 10 derniers (elle n'est pas
thread-safe) : seul le moteur la modifie. Aucun `synchronized`.

**Livrables.** Le code, et la réponse écrite : remplacez temporairement la `ConcurrentLinkedQueue` par
une `ArrayList` et lancez la simulation en ×16 : que se passe-t-il (événements perdus, exception) ?
Expliquez pourquoi. Pourquoi l'`ArrayDeque` peut-elle rester non thread-safe ?

**Pour vérifier.** Dans un test inspiré de `MetricsRaceTest`, 8 threads publient chacun 10 000
événements ; après `drain()`, aucun n'a été perdu (comptez-les avant de ne garder que les 10 derniers)
et l'historique en contient exactement 10. À l'écran, les événements défilent.

**Indices.** `queue.offer(e)`, `queue.poll()` (renvoie `null` quand la file est vide) ;
`deque.addFirst(e)`, `deque.removeLast()`.

**Pièges.** Construire le texte de l'événement coûte du temps : gardez-le court. Ne vous servez pas de
`queue.size()` pour décider combien en retirer : elle peut changer pendant le parcours.

---

### Q12. Un dispatcher central — *structure : `PriorityQueue` de paires (Uber, client)*

**Contexte.** Chaque Uber choisit pour lui seul. Deux Uber peuvent viser le même client en même temps
(c'est pour ça que `Customer.tryClaim()` utilise un `compareAndSet`), et un Uber peut prendre un client
qui était presque collé à un autre Uber. Une attribution
**globale** fait mieux.

**Où.** `engine/Simulation` (`// TODO Q12`, phase des Uber), une nouvelle classe `engine/Dispatcher`,
`agent/Uber` (une méthode pour recevoir un client attribué).

**Ce que vous devez faire.**
1. À chaque tick, construisez toutes les paires (Uber libre, client en attente) avec leur distance,
   dans une `PriorityQueue` triée de la plus courte à la plus longue.
2. Retirez les paires une à une : si l'Uber et le client sont encore libres, attribuez le client à
   l'Uber ; sinon, sautez la paire. C'est un **appariement glouton**.
3. Les Uber ne choisissent plus eux-mêmes : ils reçoivent leur client du dispatcher.

**Contraintes.** Le dispatcher tourne sur le thread du moteur ; les Uber continuent ensuite leur
phase (rouler) comme avant.

**Livrables.** Le code, et la réponse écrite : en quoi c'est meilleur que « chaque Uber prend le plus
proche » (donnez un exemple à deux Uber et deux clients) ? Complexité. Deux Uber peuvent-ils encore
viser le même client ? `tryClaim()` est-il encore nécessaire ? Pourquoi ?

**Pour vérifier.** Sur un cas construit à la main (2 Uber, 2 clients), l'appariement glouton donne
une distance totale plus petite que le choix individuel. `flotte-uber` tourne toujours.

**Indices.** `record Pairing(Uber uber, Customer customer, double distance)` ; deux `Set` pour les Uber
et les clients déjà attribués.

**Pièges.** Le nombre de paires est (Uber libres × clients) : avec beaucoup de clients, limitez-vous
aux k plus proches de chaque Uber.

---

### Q13. Remplacer `parallelStream` — *concurrence : `ExecutorService`, `invokeAll`*

**Contexte.** Les phases parallèles utilisent `parallelStream()`, qui cache tout : combien de
threads, comment le travail est découpé, comment on attend la fin. On veut le contrôler nous-mêmes.

**Où.** `engine/Simulation` (`// TODO Q13`, phases des Uber et des clients).

**Ce que vous devez faire.**
1. Créez un `ExecutorService` à taille fixe (N threads), une seule fois, au démarrage de la simulation.
2. À chaque tick, découpez la liste des clients en N tranches à peu près égales ; faites de chaque
   tranche une tâche (`Callable`) qui met à jour ses clients.
3. Soumettez toutes les tâches avec `invokeAll` (qui attend leur fin), puis passez à la phase suivante.
4. Faites de même pour la phase des Uber. Ajoutez une clé de scénario `engine.workerThreads` pour N.

**Contraintes.** Le moteur ne doit passer à la phase suivante qu'une fois **toutes** les tâches
terminées ; aucune race ne doit réapparaître.

**Livrables.** Le code, et la réponse écrite : pourquoi la fin de `invokeAll` garantit-elle que le
thread du moteur voit les écritures faites par les autres threads (*happens-before*) ? Mesurez le
rythme du moteur sur `stress-test` en ×16 pour N = 1, 2, 4 et 8 : que constatez-vous, et pourquoi ?

**Pour vérifier.** Tous les tests restent verts, en particulier `SimulationTest` (aucun bus ne
dépasse sa capacité) et les tests de concurrence.

**Indices.** `Executors.newFixedThreadPool(n)`, `invokeAll(List<Callable<Void>>)`,
`List.subList(debut, fin)`.

**Pièges.** Créer un nouvel `ExecutorService` à chaque tick (très lent) ; oublier de le fermer à
l'arrêt ; découper une liste qu'un autre thread modifie.

---

### Q14. La route des Uber par parcours en largeur — *graphe : BFS et reconstruction du chemin*

**Contexte.** Les Uber cherchent leur route avec Dijkstra (`trip/Roads.fastestRoute`). Mais sur les
routes, **chaque tronçon coûte exactement une minute** : quand toutes les arêtes ont le même coût, un
simple parcours en largeur (BFS) trouve un plus court chemin, sans file de priorité.

**Où.** `trip/Roads` (`// TODO Q14`), une nouvelle méthode, dans `Roads` ou dans `graph`.

**Ce que vous devez faire.**
1. Écrivez une méthode `List<Integer> bfsRoute(CityMap map, int from, int to)` : un BFS depuis `from`
   qui s'arrête dès qu'il atteint `to`.
2. Pendant le parcours, retenez le **prédécesseur** de chaque nœud (le nœud d'où on l'a atteint),
   dans un tableau `int[]`.
3. Reconstruisez le chemin en remontant les prédécesseurs depuis `to` jusqu'à `from`, puis
   inversez-le pour qu'il aille de `from` à `to` (les deux inclus, comme `fastestRoute`).
4. Faites utiliser `bfsRoute` par `fastestRoute` (les Uber s'en servent alors automatiquement).

**Contraintes.** Une `ArrayDeque` comme file ; la méthode renvoie un chemin de même longueur que
celui de Dijkstra.

**Livrables.** Le code, et la réponse écrite : pourquoi le BFS trouve-t-il un plus court chemin
**seulement** quand toutes les arêtes ont le même coût ? Donnez un petit exemple où il se tromperait
avec des coûts différents. Comparez la complexité du BFS et de Dijkstra, et mesurez le temps de 1 000
routes au hasard avec chacun.

**Pour vérifier.** Sur 100 trajets au hasard dans la ville par défaut, le chemin du BFS a **le même
nombre de nœuds** que celui de Dijkstra (pas forcément les mêmes nœuds). Sur `TestMaps.street()`,
la route de 4 à 0 est 4 → 2 → 1 → 0. `UberTest` reste vert.

**Indices.** Comme pour Q10 : un tableau `int[]` rempli de -1 sert de « déjà visité » ; les voisins sont
dans `map.neighbors(nœud)`. `Collections.reverse` inverse une liste.

**Pièges.** Marquer un nœud comme visité quand on le **retire** de la file au lieu de quand on
l'**ajoute** ; oublier le cas `from == to` (le chemin est alors juste `[from]`).

### Q15. Une ville en damier — *génération : tableau 2D*

**Contexte.** La ville est générée en plaçant des intersections au hasard (`map/RoadLayout`). On veut
un second mode, plus simple et très différent : une ville **en damier**, comme un quartier
nord-américain, avec des rues droites qui se croisent à angle droit.

**Où.** `map/RoadLayout` (`// TODO Q15`), `settings` (nouvelle clé).

**Ce que vous devez faire.**
1. Ajoutez une méthode qui place les intersections sur une **grille** de `colonnes × rangées`, à
   intervalles réguliers (l'intervalle vaut `city.intersectionSpacing()`), dans la zone utile de la
   carte (en respectant `MARGIN`).
2. Gardez l'indice de chaque intersection dans un **tableau 2D** `int[rangée][colonne]`.
3. Reliez chaque intersection à sa voisine de **droite** et à sa voisine du **dessous** (si elles
   existent) : ce sont les routes.
4. Ajoutez un peu de hasard pour que ce ne soit pas trop parfait : décalez chaque intersection de
   quelques pixels au hasard (constante nommée, bien plus petite que l'intervalle).
   *Bonus* : retirez aussi quelques routes au hasard, mais seulement si la ville reste d'un seul
   morceau après le retrait (vérifiez-le avec un parcours, comme le test `graphIsConnected`).
5. Ajoutez une clé de scénario `city.roadLayout = organique | damier` (dans `simulation.properties`,
   avec un commentaire, et dans `Settings.City`) ; le mode par défaut reste `organique`.

**Contraintes.** Les lignes de bus, la subdivision et tout le reste doivent fonctionner sans
changement sur la ville en damier.

**Livrables.** Le code, et la réponse écrite : comment passe-t-on d'une position (rangée, colonne) à
un indice et inversement ? Combien de routes compte une grille de *c* colonnes et *r* rangées avant
le retrait ? Comparez les deux villes : nombre de nœuds, durée moyenne des trajets, attente moyenne.

**Pour vérifier.** Créez un scénario `damier.properties` et lancez-le : la ville a des rues droites.
Les tests de `MapGeneratorTest` (ville connexe, routes qui ne se croisent pas) passent aussi avec ce
mode : adaptez-les pour tester les deux modes.

**Indices.** Les voisins de (r, c) sont (r, c+1) et (r+1, c) ; `new Edge(a, b)` crée une route ;
`RoadLayout` est un `record(List<Point> intersections, List<Edge> roads)` : il suffit de produire ces deux listes.

**Pièges.** Changer `city.roadLayout` doit régénérer la carte : en l'ajoutant au record
`Settings.City`, l'empreinte de `MapStore` en tient compte automatiquement. Un décalage trop grand
peut faire se croiser deux routes.
