# Introduction

This IntelliJ IDEA plugin runs the native *Convert Java File To Kotlin File* action and preserves files VCS 
history by committing the rename step in VCS.

Indeed, the native action renames the files to convert which may result in loosing the entire file VCS history 
(e.g. git history).


# How does it work?

For each selected Java file, the following steps are applied:
- Rename Java file with Kotlin extension
- Commit previous renaming step to VCS with standard commit message 
- Rename file back to Java extension
- Run native *Convert Java File To Kotlin File* action


# Plugin manual tests

Run the following gradle command:
```sh
./gradlew runIde
```


# Company Proxy

When running gradle command `runIde`, IntelliJ IDEA downloads (and launches) an IDEA version from `dl.bintray.com`.

If you are behind a company proxy, it may be necessary to configure proxy in the following files:
```sh
<idea_intsallation_path>/bin/idea.vmoptions
<idea_intsallation_path>/bin/idea64.vmoptions
```

By adding the following lines:
```
-Dhttps.proxyHost=localhost
-Dhttps.proxyPort=3128
```