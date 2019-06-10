repositories {
    jcenter()
}

plugins {
    `kotlin-dsl`
}

// Activate reproducible archives
tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
