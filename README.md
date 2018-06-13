# Tests du plugin

Lancer la commande gradle suivante :
```sh
./gradlew runIde
```

# Proxy

Lors de l'exécution de la commande `runIde`, IntelliJ IDEA va télécharger (et démarrer) une version de l'IDE pour 
valider l'intégration du plugin. Cette version est téléchargée depuis `dl.bintray.com`.

Afin de faire fonctionner ce téléchargement, il est **nécessaire** de configurer le proxy dans les fichiers suivants :
```sh
<intallation_path>/bin/idea.vmoptions
<intallation_path>/bin/idea64.vmoptions
```

Il faut en effet ajouter les lignes suivantes :
```
-Dhttps.proxyHost=localhost
-Dhttps.proxyPort=3128
```