# maven-publish-check
Gradle task for Kotlin DSL to check if published artifact exists in repo already.
It allows to avoid publishing the artifact if artifact with same version is already stored in maven repo. It does not apply for SNAPSHOT.

This code is based on mosamman/maven-publish-check.gradle with few enhancements.

Don't forget to configure publishing plugin.
