# Régler une simulation

Tout ce qui définit **une simulation** (la ville, la flotte, les clients, les tarifs, la vitesse) se
règle dans un **scénario**, sans toucher au code.

> Les constantes *internes* du code (celles qu'on ne change que pour modifier le modèle lui-même)
> sont décrites à part, dans [`CONSTANTES.md`](CONSTANTES.md).

## Lancer un scénario

**Avec le bouton Play** : lancez `Main` (▶ à côté de `public static void main`). Une fenêtre propose
les scénarios fournis ; choisissez-en un et cliquez sur **Lancer**. **Autre fichier…** ouvre un
fichier de scénario de votre choix.

**Avec Maven**, en ligne de commande :

```bash
mvn compile exec:java
```

```bash
mvn compile exec:java -Dexec.args="chemin/vers/mon-scenario.properties"
```

## Créer son scénario

1. Copiez un fichier de `src/main/resources/scenarios/` sous un nouveau nom, par exemple
   `heure-de-pointe-geante.properties`.
2. Gardez seulement les clés à changer : une clé absente garde sa valeur par défaut.
3. Première ligne : `# Scénario : une phrase qui le décrit` (elle s'affiche dans la fenêtre de choix).
4. Ajoutez son nom à la liste `Scenarios.BUNDLED` (paquetage `settings`). Un test vérifie que la
   liste et les fichiers correspondent.

Une valeur hors plage empêche le démarrage, avec un message qui nomme la clé fautive.

## Les clés

Les valeurs par défaut viennent de `src/main/resources/simulation.properties` (seule source de vérité,
où chaque clé est aussi commentée).

| Clé | Défaut | Plage | Effet |
|-----|--------|-------|-------|
| `seed` | 42 | tout entier | Graine du hasard. Même graine = même ville et mêmes arrivées de clients. |
| `city.width` | 2274 | 800 – 4800 | Largeur de la ville (px). Plus grand = plus de nœuds. Ratio 3:2 conseillé. |
| `city.height` | 1516 | 600 – 3200 | Hauteur de la ville (px). |
| `city.intersectionSpacing` | 150 | 80 – 400 | Distance minimale entre intersections. Plus petit = ville plus dense. |
| `city.busLines` | 7 | 1 – 20 | Lignes de bus voulues (une petite ville peut en recevoir moins ; c'est affiché au démarrage). |
| `fleet.busesPerLine` | 2 | 1 – 10 | Bus par ligne, répartis régulièrement sur l'aller-retour. |
| `fleet.busCapacity` | 5 | 1 – 100 | Places par bus. Plein = FULL, il refuse les clients. |
| `fleet.ubers` | 5 | 0 – 100 | Nombre d'Uber (un client à la fois chacun). |
| `customers.perMinute` | 2 | 0 – 100 | Débit de nouveaux clients (par minute simulée) sur les ticks où des clients apparaissent. |
| `customers.spawnEveryTicks` | 3 | 1 – 600 | Les clients n'apparaissent qu'un tick sur N. **Débit réel = `perMinute` ÷ `spawnEveryTicks`** (défaut : ≈ 0,67/min). |
| `customers.rushHourFactor` | 10 | 1 – 50 | Multiplicateur du bouton « Heure de pointe ». |
| `customers.terminusShare` | 0.4 | 0 – 1 | Part des clients en bus qui vont à une **fin de ligne**. Personne n'apparaît à un terminus. |
| `customers.offNetworkShare` | 0.15 | 0 – 1 | Part des clients qui partent ou vont **hors réseau** : ils prennent un Uber pour tout le trajet. |
| `customers.transferMinutes` | 5 | 0 – 60 | Attente estimée à une correspondance, utilisée pour planifier les trajets. |
| `fares.bus` | 3.50 | 0 – 50 | Prix d'un trajet en bus (en $), correspondances incluses. |
| `fares.uberBase` | 3.00 | 0 – 50 | Course Uber : montant de base (en $). |
| `fares.uberPerMinute` | 0.60 | 0 – 10 | Course Uber : prix par minute simulée dans la voiture (en $). |
| `engine.ticksPerSecond` | 20 | 1 – 200 | Ticks par seconde en ×1 (1 tick = 6 secondes simulées). |

Changer une clé `city.*` ou `seed` régénère la carte au lancement suivant (quelques dixièmes de seconde).

## Les scénarios fournis et leurs questions de discussion

Laissez tourner chaque scénario au moins jusqu'à 16 h simulées (×16 aide), puis discutez en équipe.
Appuyez vos réponses sur des **chiffres** du tableau de bord.

| Scénario | Ce qui change | Questions de discussion |
|----------|---------------|-------------------------|
| `defaut` | La ville resserrée : bus de 5 places, 7 lignes, 5 Uber | Les files se stabilisent-elles ? Où se forment-elles : aux correspondances, aux arrêts isolés ? Pourquoi là ? |
| `gros-bus` | Bus de 40 places | Quelle est la capacité **minimale** qui stabilise la file jaune (essayez 5, 8, 12, 20) ? Que deviennent les recettes ? Un bus de 40 places à moitié vide est-il un bon investissement ? |
| `plus-de-bus` | 4 bus par ligne (5 places) | Même nombre total de places que des bus de 10 : qu'est-ce qui réduit le plus l'**attente** ? Pourquoi la fréquence compte-t-elle autant que la capacité ? |
| `sans-uber` | Aucun Uber, aucun client hors réseau | Qui perd le plus quand l'Uber disparaît ? Le prix moyen d'un trajet baisse : est-ce une bonne nouvelle pour tout le monde ? |
| `flotte-uber` | 5 Uber, 60 % de clients hors réseau | À partir de combien d'Uber la file verte arrête-t-elle de grossir ? Le seuil est-il proportionnel à la demande ? Pourquoi un Uber passe-t-il autant de temps « en route vers le client » ? |
| `correspondances-cheres` | Correspondance estimée à 30 min | Les clients évitent les correspondances : leurs trajets s'allongent-ils ? Les files se déplacent-elles vers d'autres arrêts ? Que dit ce scénario sur le *coût perçu* d'un trajet ? |
| `terminus-populaires` | 90 % des clients vont en bout de ligne | Les bus du retour vers le centre sont-ils vides ? Comment une vraie société de transport gérerait-elle ce déséquilibre ? |
| `ville-dense` | Intersections à 100 px | Combien de nœuds de plus ? Temps de chargement, rythme du moteur en ×16, durée des trajets : qu'est-ce qui change, et pourquoi ? |
| `grande-ville` | La grande ville d'origine : 2400 × 1600, 8 lignes, bus de 40 places | Plus de lignes et plus de places : la ville est-elle « bien desservie » ? Comment le mesureriez-vous ? |
| `stress-test` | Très grande ville, 14 lignes, 8 bus/ligne, 40 Uber, 40 clients/min | Le rythme du moteur tient-il en ×16 ? Quelle partie du code ralentit le plus (indice : combien de demandes chaque Uber trie-t-il) ? La file Uber grossit : la mémoire aussi ? |

## Les limites de ce modèle

Un modèle simplifie toujours. Ces simplifications sont **voulues**, mais elles limitent ce qu'on peut
conclure. Pour chacune : qu'est-ce qu'elle cache ? Comment la lever ?

| Simplification | Ce qu'elle cache | Amélioration possible |
|----------------|------------------|-----------------------|
| Tous les véhicules roulent à la même vitesse (1 tronçon = 1 minute), sans circulation. | Les bouchons, les heures de pointe sur la route, l'avantage des voies réservées aux bus. | Une vitesse par tronçon, qui baisse avec le nombre de véhicules. |
| Pas de marche à pied. | Un client à 2 minutes d'un arrêt prend un Uber pour tout son trajet. | Une marche jusqu'à l'arrêt le plus proche, avec une vitesse de marche. |
| Le client choisit son trajet une fois, à l'apparition, et ne change jamais d'avis. | Un client bloqué derrière des bus pleins devrait pouvoir changer de ligne ou prendre un Uber. | Recalculer le trajet sur des événements (bus FULL passé, attente trop longue). |
| Le mode (bus ou Uber) est imposé par l'endroit où le client part et arrive. | Personne ne choisit l'Uber parce qu'il est pressé, ni le bus parce qu'il est moins cher. | Un coût généralisé : prix + valeur du temps × durée, avec des profils de clients. |
| Les bus suivent un horaire implicite (aller-retour sans fin), sans régulation. | Les bus d'une ligne finissent par se suivre en paquet (*bus bunching*). | Des départs à intervalles fixes, ou un bus qui attend s'il est trop près du précédent. |
| Un Uber prend le client le plus proche, chacun pour soi. | Une attribution globale peut faire beaucoup mieux (et le client le plus ancien peut attendre longtemps). | Un dispatcher central (question Q12) ou une file « premier arrivé, premier servi » (Q6). |
| La demande est uniforme dans la journée et dans la ville. | Les quartiers résidentiels, les centres d'emploi, les heures de pointe réelles. | Des zones de demande et un débit qui varie selon l'heure simulée. |
| Les tarifs sont fixes. | La tarification dynamique, les abonnements, les correspondances payantes. | Un tarif Uber qui monte quand la file est longue. |

## Fiche d'expérience (à recopier)

| Scénario | Heure simulée | En attente (bus / Uber) | Bus FULL | Attente moyenne | Trajet porte-à-porte | Prix moyen d'un trajet | Rythme du moteur en ×16 |
|----------|---------------|-------------------------|----------|-----------------|----------------------|------------------------|-------------------------|
| defaut   | 16:00 | | | | | | |
| ...      | | | | | | | |
