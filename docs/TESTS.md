# Tests et débogage

Cette partie se fait **après le développement**. Elle compte pour 14 % (voir le [barème](BAREME.md)) :

1. **Trois tests unitaires**, un par membre de l'équipe, dans trois paquetages différents (9 points).
2. **Un bug** : le trouver, le documenter, le corriger (5 points).

---

## 1. Écrire un test unitaire

### Pourquoi tester ?
Un test est un petit programme qui vérifie **automatiquement** qu'un morceau de code fait ce qu'il
doit faire. Une fois écrit, il tourne à chaque changement : si quelqu'un casse ce comportement plus
tard, le test devient rouge tout de suite.

### Anatomie d'un test JUnit 5

```java
class FaresTest {                                         // 1 classe de test par classe testée

    @Test                                                 // une méthode = un comportement
    void anUberRideCostsTheBasePlusEachMinuteInTheCar() { // le nom est une phrase
        // Préparer (Arrange) : les objets nécessaires
        Fares fares = new Fares(350, 300, 60);

        // Agir (Act) : l'appel qu'on veut vérifier
        long price = fares.uberRideCents(10L * Time.TICKS_PER_SIM_MINUTE);

        // Vérifier (Assert) : le résultat attendu
        assertEquals(300 + 10 * 60, price);
    }
}
```

- La classe de test va dans `src/test/java`, **dans le même paquetage** que la classe testée
  (ex. `src/test/java/com/transit/trip/` pour `trip.Fares`) : elle peut alors utiliser ce qui est
  visible dans le paquetage.
- Les vérifications les plus utiles : `assertEquals(attendu, obtenu)`, `assertTrue(condition, message)`,
  `assertThrows(Exception.class, () -> appel())`.
- Pour un nombre à virgule : `assertEquals(0.4, part, 0.01)` (la tolérance est le 3ᵉ argument).

### Des outils déjà prêts
- `TestMaps.street()` et `TestMaps.cross()` : de toutes petites cartes faites à la main, dessinées
  dans leur Javadoc. Idéal pour savoir exactement ce qui doit arriver.
- `TestMaps.defaultCity()` : la ville du scénario par défaut.
- `@TempDir Path dossier` en paramètre d'un test : un dossier temporaire, pratique pour une base
  SQLite de test (voir `MapStoreTest`).

### Lancer les tests
Flèche verte ▶ à côté de la classe ou de la méthode de test, ou clic droit sur `src/test/java` →
**Run 'All Tests'**. En ligne de commande :

```bash
mvn test
```

### Un bon test…
- vérifie **un seul comportement**, dit dans son nom ;
- est **déterministe** : il donne le même résultat à chaque exécution (graine fixe pour le hasard) ;
- teste aussi un **cas limite** (liste vide, zéro, valeur maximale, cul-de-sac…) ;
- échoue si on casse le code : essayez ! Modifiez la classe testée, le test doit devenir rouge.

---

## 2. Vos trois tests

Un test par membre, chacun dans un paquetage différent parmi les trois suivants. Choisissez un
comportement qui **n'est pas déjà testé** (regardez les tests existants du paquetage).

| Paquetage | Idées de comportements non testés |
|-----------|-----------------------------------|
| `trip` | Un trajet de deux arrêts voisins sur la même ligne ; le coût d'une correspondance change-t-il le trajet choisi sur `TestMaps.cross()` quand `transferMinutes` est très grand ? `Fares` avec des tarifs à zéro. |
| `agent` | `Movement` : `previousX()` à l'arrêt ; un `Bus` à un point de route n'est pas « à un arrêt » ; un `Uber` sans demande erre toujours vers un voisin ; un client dont aucun bus de la ligne ne passe à son arrêt reste `WAITING`. |
| `settings` ou `map` | `Keys` refuse un nombre mal écrit (`"abc"`) avec un message qui nomme la clé ; `CityMap.terminals()` sur `TestMaps.street()` ; `Edge` normalise `(5, 2)` en `(2, 5)` et refuse `(3, 3)`. |

Chaque test est livré avec **deux lignes** dans votre rapport : le comportement vérifié, et le cas
limite couvert.

---

## 3. Le bug : trouver, documenter, corriger

Prenez un bug **rencontré pendant le projet**, idéalement un que votre équipe a introduit en
répondant aux questions. Si vous n'en avez aucun, demandez-en un à l'enseignant.

### 3.1 Chercher
1. **Reproduire** : trouvez les étapes exactes (scénario, vitesse, heure simulée) qui font apparaître le bug.
2. **Réduire** : trouvez le plus petit cas qui le montre (souvent avec `TestMaps.street()`).
3. **Observer** : posez des points d'arrêt, regardez les valeurs. Dans un code concurrent, le
   panneau *Threads* du débogueur montre qui exécute quoi.
4. **Remonter l'historique** : *Git → Show History* sur le fichier, ou `git log -p` ; si le bug est
   apparu entre deux versions, `git bisect` trouve le commit fautif.

### 3.2 Écrire d'abord un test qui échoue
Avant de corriger, écrivez un test qui **reproduit** le bug : il doit être rouge. C'est votre
**test de non-régression** : une fois le bug corrigé, il devient vert et empêche le bug de revenir.

### 3.3 Corriger
Corrigez la cause, pas le symptôme. Le nouveau test et **tous** les anciens doivent être verts.

### 3.4 Documenter
Ajoutez un fichier `docs/bugs/<nom-du-bug>.md` en suivant ce modèle :

```markdown
# <Titre court du bug>

**Trouvé par** : … — **Corrigé par** : … — **Commit de la correction** : …

## Symptôme
Ce qu'on voit (capture, message d'erreur, chiffre faux).

## Étapes pour reproduire
1. Scénario … , vitesse … , attendre …
2. …

## Résultat attendu / obtenu
…

## Recherche
Comment vous avez trouvé la cause (points d'arrêt, historique Git, hypothèses écartées).

## Cause
L'explication technique, avec la classe et la méthode.

## Correction
Ce qui a changé et pourquoi ça règle la cause.

## Test de non-régression
Le nom du test, et ce qu'il vérifie.
```
