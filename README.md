# Origines IUT SD — V1

Mod Fabric (Minecraft **26.3**) inspiré d'Origins : 9 personnages, 9 façons de jouer en survie.

> **État honnête de cette V1** : le code a été écrit sans pouvoir être compilé (l'environnement de travail n'avait ni Minecraft ni accès aux dépôts Fabric). La structure est simple et les appels Minecraft fragiles sont isolés dans `Compat.java`, mais **la première compilation demandera probablement quelques corrections**. Envoie-moi le log d'erreurs et je corrige.

## Ce qui est fait

- 9 origines jouables, choisies avec `/origine`.
- Pouvoirs activés avec un **objet signature** (clic droit = pouvoir 1, accroupi + clic droit = pouvoir 2).
- Recharges visibles dans la barre d'action quand l'objet est en main.
- Sauvegarde du personnage choisi (fichier `origines_iut_data.json` dans le dossier du monde).
- Fonctionne en multijoueur, **côté serveur uniquement** : les joueurs n'ont pas besoin d'installer le mod.
- Configuration d'équilibrage : `config/origines_iut.json` (créée au premier lancement, redémarrer pour appliquer).
- Punchlines de Baptou modifiables : `config/origines_iut_punchlines.txt`.

## Ce qui n'est PAS dans la V1 (volontairement)

- Écran de sélection graphique, touches clavier dédiées, HUD de recharges personnalisé : tout ça demande un module client, plus fragile à écrire sans compiler. Prévu pour la V2 une fois la V1 compilée.
- La V1 remplace ça par : commande `/origine`, objet signature, recharges dans la barre d'action.

## Compiler

Prérequis : **Java 25** (JDK).

```
./gradlew build        # Linux / Mac
gradlew.bat build      # Windows
```

Le mod compilé est dans `build/libs/origines-iut-0.1.0.jar`. À mettre dans le dossier `mods/` du serveur (avec Fabric Loader 0.19.5+ et Fabric API pour 26.3).

## Commandes

| Commande | Effet |
|---|---|
| `/origine` | liste des personnages |
| `/origine info <nom>` | fiche complète |
| `/origine choisir <nom>` | choisir son personnage (une seule fois, sauf si `allow_player_change` = 1 dans la config) |
| `/origine talisman` | récupérer son objet signature |
| `/origine admin set <joueur> <nom>` | (op) changer l'origine d'un joueur |
| `/origine admin reset <joueur>` | (op) réinitialiser l'origine d'un joueur |

## Les 9 personnages (V1 simplifiée)

| Perso | Objet | Clic droit | Accroupi + clic droit | Passifs | Malus |
|---|---|---|---|---|---|
| Thomas_H | Raquette (bâton) | Smash devant lui | Renvoi des projectiles 1,5 s | Vitesse avec la raquette | Plastron lourd = tout perdu ; smash coûte de la faim |
| Thomas_D | Disquette (papier) | Sauvegarde de position | Chargement (5 s immobile, 3 niveaux d'XP) | XP en plus sur les minerais | Mise à jour forcée (figé 5 s) toutes les ~20 min au calme |
| Timothe | Ballon (crème de magma) | Dunk | — | Collection d'objets (XP, Hype, paliers), animaux apprivoisés régénérés | Dunk dans le vide = lenteur ; mort = affaibli 3 min |
| Anouchka | Carnet (livre) | Fumée noire (1 charbon) | Ombres : monstres qui brillent | Vision et vitesse dans le noir ; XP par nouveau biome | Soleil sans casque = faiblesse ; rester 5 min au même endroit = faim |
| Yago | Maracas (pépite d'or) | Batucada (vitesse/hâte, animaux en amour) | Confettis (monstres figés) | Prix du Héros du village près d'un villageois | Monstres attirés après la fête ; gueule de bois |
| Rémi | Sifflet (lingot de cuivre) | Ordre aux loups et golems | Dé d6 | Armure complète même matériau = Résistance | Sprint limité à 8 s d'affilée |
| Elie | Ressort (boule de slime) | Air Jordan (saut géant) | Grimace (monstres figés) | Chutes absorbées jusqu'à ~8 blocs | Monstres le repèrent ; 3 bonds/min = fatigue |
| Lucas | Plan parfait (horloge) | Contre (selon le dernier danger) | Blague (faux creeper) | Adaptation aux dégâts répétés | Contre inutile = faiblesse ; dégât inconnu = sonné |
| Baptou | Cristal (éclat de prismarine) | Flash (7 blocs) | Lecture de la cible | Annonceur de kills ; outil en main qui se répare | Chaque sort le fait briller ; Flash bloqué après une mort |

Les valeurs exactes (durées, dégâts, rayons, coûts) sont toutes dans `config/origines_iut.json`.

## Si la compilation échoue

1. Les erreurs viennent presque toujours de **`Compat.java`** (noms de méthodes Minecraft qui changent d'une version à l'autre : dégâts, recul, téléportation, barre d'action, permissions).
2. Sinon des **imports** de classes déplacées dans les paquets de 26.x (ex. `TamableAnimal`, `Animal`, `Monster`).
3. Copie le message d'erreur complet et envoie-le : la correction est rapide.

## Structure

```
src/main/java/fr/iutsd/origines/
  OriginsMod.java      point d'entrée, événements, tick, recharges
  Origin.java          les 9 personnages (textes, objets)
  OriginPower.java     classe de base d'un pouvoir
  power/*.java         un fichier par personnage (la logique)
  Compat.java          appels Minecraft fragiles, isolés
  Config.java          config/origines_iut.json
  Store.java           sauvegarde par monde
  Talisman.java        objets signature
  OrigineCommand.java  commandes /origine
```
