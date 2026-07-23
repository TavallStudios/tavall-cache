import java.util.zip.ZipFile

plugins {
    base
}

group = "org.tavall"
version = "1.0.0"

val jacksonDatabind = libs.jackson.databind
val jedis = libs.jedis
val junitJupiter = libs.junit.jupiter
val junitPlatformLauncher = libs.junit.platform.launcher
val mongodbDriver = libs.mongodb.driver
val postgresql = libs.postgresql
val tavallDi = libs.tavall.di
val tavallLogging = libs.tavall.logging

subprojects {
    group = rootProject.group
    version = rootProject.version

    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion = JavaLanguageVersion.of(21)
        withSourcesJar()
        withJavadocJar()
    }

    repositories {
        mavenLocal()
        mavenCentral()
    }

    dependencyLocking {
        lockAllConfigurations()
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    tasks.withType<Jar>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    val verifyJarContents = tasks.register("verifyJarContents") {
        dependsOn(tasks.named("jar"))
        val archive = tasks.named<Jar>("jar").flatMap { it.archiveFile }
        inputs.file(archive)
        doLast {
            val forbidden = listOf("com/fasterxml/", "com/mongodb/", "org/postgresql/", "redis/clients/")
            ZipFile(archive.get().asFile).use { jar ->
                val embedded = jar.entries().asSequence().map { it.name }
                    .firstOrNull { entry -> forbidden.any(entry::startsWith) }
                check(embedded == null) { "Third-party class embedded in first-party JAR: $embedded" }
            }
        }
    }

    tasks.named("check") {
        dependsOn(verifyJarContents)
    }

    extensions.configure<PublishingExtension> {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                artifactId = project.name
            }
        }
        repositories {
            val token = providers.environmentVariable("GITHUB_TOKEN")
            if (token.isPresent) {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/TavallStudios/tavall-cache")
                    credentials {
                        username = providers.environmentVariable("GITHUB_ACTOR").orNull
                        password = token.get()
                    }
                }
            }
        }
    }
}

project(":abstract-cache-system") {
    dependencies {
        "api"(tavallDi)
        "api"(tavallLogging)
        "testImplementation"(junitJupiter)
        "testRuntimeOnly"(junitPlatformLauncher)
    }
}

project(":abstract-cache-semantic") {
    dependencies {
        "api"(project(":abstract-cache-system"))
        "api"(jacksonDatabind)
        "testImplementation"(junitJupiter)
        "testRuntimeOnly"(junitPlatformLauncher)
    }
}

project(":abstract-cache-storage-memory") {
    dependencies {
        "api"(project(":abstract-cache-semantic"))
        "testImplementation"(junitJupiter)
        "testRuntimeOnly"(junitPlatformLauncher)
    }
}

project(":abstract-cache-storage-disk") {
    dependencies {
        "api"(project(":abstract-cache-semantic"))
        "testImplementation"(junitJupiter)
        "testRuntimeOnly"(junitPlatformLauncher)
    }
}

project(":abstract-cache-storage-redis") {
    dependencies {
        "api"(project(":abstract-cache-semantic"))
        "api"(jedis)
        "testImplementation"(junitJupiter)
        "testRuntimeOnly"(junitPlatformLauncher)
    }
}

project(":abstract-cache-storage-mongo") {
    dependencies {
        "api"(project(":abstract-cache-semantic"))
        "api"(mongodbDriver)
        "testImplementation"(junitJupiter)
        "testRuntimeOnly"(junitPlatformLauncher)
    }
}

project(":abstract-cache-storage-postgres") {
    dependencies {
        "api"(project(":abstract-cache-semantic"))
        "api"(postgresql)
        "testImplementation"(junitJupiter)
        "testRuntimeOnly"(junitPlatformLauncher)
    }
}

project(":abstract-cache-storage-qdrant") {
    dependencies {
        "api"(project(":abstract-cache-semantic"))
        "testImplementation"(junitJupiter)
        "testRuntimeOnly"(junitPlatformLauncher)
    }
}

project(":abstract-cache-suite") {
    dependencies {
        "api"(project(":abstract-cache-system"))
        "api"(project(":abstract-cache-semantic"))
        "api"(project(":abstract-cache-storage-memory"))
        "api"(project(":abstract-cache-storage-disk"))
        "api"(project(":abstract-cache-storage-redis"))
        "api"(project(":abstract-cache-storage-mongo"))
        "api"(project(":abstract-cache-storage-postgres"))
        "api"(project(":abstract-cache-storage-qdrant"))
        "testImplementation"(junitJupiter)
        "testRuntimeOnly"(junitPlatformLauncher)
    }

    val sourceSets = extensions.getByType<SourceSetContainer>()
    val integrationTestSourceSet = sourceSets.create("integrationTest") {
        compileClasspath += sourceSets.named("main").get().output
        runtimeClasspath += output + compileClasspath
    }
    configurations.named(integrationTestSourceSet.implementationConfigurationName) {
        extendsFrom(configurations.named("testImplementation").get())
    }
    configurations.named(integrationTestSourceSet.runtimeOnlyConfigurationName) {
        extendsFrom(configurations.named("testRuntimeOnly").get())
    }

    tasks.register<Test>("integrationTest") {
        description = "Runs integration tests that require local external services."
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        testClassesDirs = integrationTestSourceSet.output.classesDirs
        classpath = integrationTestSourceSet.runtimeClasspath
        useJUnitPlatform()
        shouldRunAfter(tasks.named("test"))
    }
}
