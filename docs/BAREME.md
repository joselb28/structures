# Barème — CarNBus

Projet en **équipe de 3**. Total : **100 points**.

| Partie | Contenu | Points |
|--------|---------|--------|
| A. Expérimentation | Fiche d'expérience et questions de discussion de [`SIMULATION.md`](SIMULATION.md) : au moins 5 scénarios, dont un créé par l'équipe, et une analyse des limites du modèle. | **15** |
| B. Exploration | Réponses aux questions de [`EXPLORATION.md`](EXPLORATION.md). | **20** |
| C. Développement | Questions de [`QUESTIONS.md`](QUESTIONS.md) : au moins 3 par niveau ; 4 ou 5 par niveau élèvent le plafond. | **36** |
| D. Tests et débogage | 3 tests unitaires et un bug documenté ([`TESTS.md`](TESTS.md)). | **14** |
| E. Contribution | Appréciation, par l'enseignant, des dépôts Git de **chaque membre** pendant le projet. | **15** |

---

## A. Expérimentation — 15 points

| Critère | Points |
|---------|--------|
| Fiche d'expérience complète et chiffrée pour au moins 5 scénarios, dont un scénario créé par l'équipe | 5 |
| Réponses aux questions de discussion, appuyées sur des chiffres observés | 6 |
| Analyse des limites du modèle : au moins 3 limites, avec leur effet sur vos conclusions et une amélioration proposée | 4 |

## B. Exploration — 20 points

Les 10 sections de [`EXPLORATION.md`](EXPLORATION.md) valent 2 points chacune :
réponse juste et précise (2), partielle ou vague (1), absente ou fausse (0). Les références aux
classes et méthodes du code sont attendues.

## C. Développement — 36 points

Les 15 questions sont proposées. L'équipe en fait **au moins 3 par niveau** (chaque membre au moins
une par niveau). Le nombre de questions remises **par niveau** fixe le **plafond** de la note :

| Questions remises par niveau | Plafond de la note de développement |
|------------------------------|-------------------------------------|
| 3 (9 questions) | 80 % (28,8 / 36) |
| 4 (12 questions) | 90 % (32,4 / 36) |
| 5 (15 questions) | 100 % (36 / 36) |

Si les niveaux ne sont pas égaux (ex. 4, 3 et 5), c'est le **plus petit** nombre qui compte.

Chaque question vaut **3 points** (★), **4 points** (★★) ou **5 points** (★★★). La note est :

> note = 36 × plafond × (points obtenus ÷ points possibles des questions remises)

*Exemple* : 3 questions par niveau (possible : 3×3 + 3×4 + 3×5 = 36 points), 30 points obtenus →
36 × 0,80 × 30/36 = 24 / 36.

Chaque question est une **nouvelle classe ou méthode**, placée à l'endroit indiqué par son
`// TODO Qn` dans le code. Le rapport indique qui a fait chaque question.

Grille appliquée à chaque question, en pourcentage de ses points :

| Critère | % | Excellent | Satisfaisant | Insuffisant |
|---------|---|-----------|--------------|-------------|
| **Fonctionne et s'intègre** | 45 % | Fait ce qui est demandé dans la vraie simulation ; visible (boîte « Vos fonctionnalités », carte ou console) ; aucun test existant cassé. | Fonctionne, avec un cas limite oublié ou une intégration partielle. | Ne fonctionne pas, ou casse la simulation ou des tests existants. |
| **Structure de données / mécanisme** | 35 % | Le bon choix, justifié par écrit avec sa complexité ; comparaison avec une alternative. | Bon choix, justification sommaire ou complexité absente. | Choix inadapté ou non justifié. |
| **Clean code** | 20 % | Noms du domaine, méthodes courtes, Javadoc en français sur ce qui n'est pas évident, pas de nombre magique, dépendances à sens unique respectées. | Lisible, avec quelques écarts. | Difficile à lire ou à relier au reste du code. |

Une question dont le code **ne compile pas** vaut 0. Une question non listée peut être acceptée si
l'enseignant l'approuve avant.

## D. Tests et débogage — 14 points

| Critère | Points |
|---------|--------|
| 3 tests unitaires (3 points chacun) : comportement non déjà testé, nom clair, un cas limite, déterministe | 9 |
| Bug : test de non-régression qui échouait avant la correction | 2 |
| Bug : rapport complet (symptôme, reproduction, recherche, cause, correction) | 2 |
| Bug : correction de la cause, tous les tests verts | 1 |

## E. Contribution — 15 points (note individuelle)

Appréciation, par l'enseignant, de l'historique Git de **chaque membre** :

| Critère | Points |
|---------|--------|
| Régularité : des commits tout au long du projet, pas tout la dernière semaine | 5 |
| Qualité des commits : petits, cohérents, messages qui disent ce qui a été fait et pourquoi | 5 |
| Part du travail : chaque membre a contribué à ses questions, à ses tests et aux documents | 5 |

Les points A à D sont communs à l'équipe ; la partie E est individuelle.
