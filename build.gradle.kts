import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.30"
    application
}

group = "me.juritomak"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(group = "net.kieker-monitoring", name = "kieker", version = "1.14", classifier = "emf")
    implementation(group = "ch.qos.logback", name = "logback-classic", version = "1.1.7")
    implementation(group = "org.slf4j", name = "slf4j-api", version = "1.7.30")
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClassName = "MainKt"
}