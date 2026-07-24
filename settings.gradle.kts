plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "abstract-cache"

include(
    "abstract-cache-system",
    "abstract-cache-semantic",
    "abstract-cache-storage-memory",
    "abstract-cache-storage-disk",
    "abstract-cache-storage-redis",
    "abstract-cache-storage-mongo",
    "abstract-cache-storage-postgres",
    "abstract-cache-storage-qdrant",
    "abstract-cache-suite",
)
