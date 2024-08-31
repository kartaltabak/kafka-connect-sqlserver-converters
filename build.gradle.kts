import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "2.0.0"
    `maven-publish`
    id("jacoco")
}

group = "name.tabak.kafka.connect"
version = "1.0-beta1"

repositories {
    mavenCentral()
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
}

val kafkaConnectVersion = "7.5.3-ccs"
val junitVersion = "5.10.2"

dependencies {
    implementation("org.apache.kafka:connect-transforms:$kafkaConnectVersion")
    implementation("org.slf4j:slf4j-api:2.0.12")
    implementation("io.debezium:debezium-api:2.7.0.Final")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")

    testImplementation("io.mockk:mockk:1.13.12")
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("src/main/java", "src/main/kotlin"))
        }
    }
    test {
        java {
            setSrcDirs(listOf("src/test/java", "src/test/kotlin"))
        }
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "11"
    targetCompatibility = "11"
}

tasks.test {
    useJUnitPlatform()
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}