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
val junitVersion = "5.11.1"

dependencies {
    implementation("org.apache.kafka:connect-transforms:$kafkaConnectVersion")
    implementation("org.slf4j:slf4j-api:2.0.12")
    implementation("io.debezium:debezium-api:2.7.0.Final")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")

    testImplementation("io.mockk:mockk:1.13.12")

    testImplementation("org.testcontainers:testcontainers:1.20.1")
    // required to overwrite testcontainers problematic dependency in 1.20.1
    testImplementation("org.apache.commons:commons-compress:1.27.1")

    testImplementation("org.testcontainers:kafka:1.20.1")

    testImplementation("org.testcontainers:mssqlserver:1.20.1")

    testImplementation("org.testcontainers:postgresql:1.20.1")

    testImplementation("com.microsoft.sqlserver:mssql-jdbc:12.8.1.jre11")
    testImplementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("org.postgresql:postgresql:42.7.4")
    testImplementation("org.apache.kafka:connect-transforms:$kafkaConnectVersion")
    testImplementation("io.debezium:debezium-api:2.7.0.Final")
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

tasks.withType<Test> {
    dependsOn(tasks.named("jar"))
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