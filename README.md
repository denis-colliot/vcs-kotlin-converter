> # :warning: Deprecated plugin no longer maintained :warning:
> 
> Latest versions of **IntelliJ IDEA** & **Android Studio** now provide this feature natively through the commit dialog of their builtin git support.
> 
> See related post blog: https://medvector.github.io/kotlin/converting-to-Kotlin/
> 
> For this reason, this plugin comes to its end and is no longer maintained.
> 
> Long live Kotlin!


# Introduction

The __VCS Kotlin Converter__ IntelliJ IDEA plugin runs the native *Convert Java File To Kotlin File* action and preserves files VCS 
history by committing the rename step in VCS.

Indeed, the native action renames and transforms content of the files to convert which may result in loosing the 
entire file VCS history (e.g. git history).  
This plugin helps solving this annoying issue.


# How to install?

The plugin is published under the *JetBrains Plugins Repository* (see [here](https://plugins.jetbrains.com/plugin/10862-vcs-kotlin-converter)) 
and can be installed following these simple steps:

 1. Open __Settings__ menu (`Ctrl Alt S`).
 2. Access __Plugins__ section.
 3. Click __Brows repositories...__ button.
 4. Search for __VCS Kotlin Converter__ and click __Install__ button.


# How does it work?

This plugin adds a new *Convert Java File To Kotlin File in VCS* menu action right after *Convert Java File To Kotlin File* 
native menu (under *Code* menu).  
The new menu overrides the default native menu keymap `Ctrl Alt Shift K`, but may be configured in IDE settings.

When running this new action menu, the following steps are applied to each selected Java file:

 - Rename Java file with Kotlin extension
 - Commit renaming step to VCS with standard commit message 
 - Rename file back to Java extension

Once all renaming operations are done, the plugin invokes the native *Convert Java File To Kotlin File* action on the 
selected Java files.


# Developers

## Plugin manual tests

Run the following gradle command:
```sh
./gradlew runIde
```

## Company Proxy

When running gradle command `runIde`, IntelliJ IDEA downloads (and launches) an IDEA version from `dl.bintray.com`.

If you are behind a company proxy, it may be necessary to configure proxy in the following files:

```sh
<idea_installation_path>/bin/idea.vmoptions
<idea_installation_path>/bin/idea64.vmoptions
```

By adding and configuring the following lines:

```
-Dhttps.proxyHost=<proxy_host>
-Dhttps.proxyPort=<proxy_port>
-Dhttp.proxyUser=<proxy_username> 
-Dhttp.proxyPassword=<proxy_password>
```

## Publish plugin

The plugin can be automatically published to *JetBrains Plugins Repository* using the following gradle command:

```
./gradlew publishPlugin
```

This command relies on publish plugin configuration declared in `build.gradle` :

```
publishPlugin {
    token project.properties['jetbrains.publish.token']
}
```

The publish token should be defined in **local** `gradle.properties` under key `jetbrains.publish.token` **but 
should never be indexed in git**!
