# `settings` — les réglages d'une simulation

`Settings` rassemble tous les boutons qu'on peut tourner **sans toucher au code** : taille de la
ville, nombre de lignes, flotte, débit de clients... Ils sont lus dans un fichier de scénario.

- Valeurs par défaut (et documentation de chaque clé) : `src/main/resources/simulation.properties`.
- Scénarios prêts à l'emploi : `src/main/resources/scenarios/`, listés dans `Scenarios.BUNDLED` et
  proposés par la fenêtre de lancement (`ui/ScenarioChooser`).
- Guide complet : [`docs/SIMULATION.md`](../../../../../../docs/SIMULATION.md).

`Resources` lit ces fichiers comme des ressources du programme : ils sont trouvés quel que soit le
dossier de travail, donc le bouton Play suffit.

`Keys` vérifie chaque valeur : une valeur hors plage arrête le démarrage avec un message qui
nomme la clé à corriger.

## Dépend de
Rien.
