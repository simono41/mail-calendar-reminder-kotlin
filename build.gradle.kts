plugins {
    kotlin("jvm") version "1.8.21"
    application
}

group = "eu.brothertec"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    // https://mvnrepository.com/artifact/org.mnode.ical4j/ical4j
    implementation("org.mnode.ical4j:ical4j:3.2.11")

    // https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-core
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")

    // https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-api
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")

    // https://mvnrepository.com/artifact/jakarta.mail/jakarta.mail-api
    implementation("jakarta.mail:jakarta.mail-api:2.1.2")

    // https://mvnrepository.com/artifact/org.eclipse.angus/jakarta.mail
    implementation("org.eclipse.angus:jakarta.mail:2.0.2")

}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(19)
}

application {
    mainClass.set("MainKt")
}

tasks.register<Jar>("uberJar") {
    archiveClassifier.set("uber")

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)

    manifest {
        attributes("Main-Class" to "MainKt")
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}