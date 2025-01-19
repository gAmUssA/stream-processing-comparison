plugins {
    application
    java
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
}

dependencies {
    val kafkaVersion: String by rootProject.extra
    val lombokVersion: String by rootProject.extra
    val slf4jVersion = "2.0.16"
    val logbackVersion = "1.5.16"
    val testcontainersVersion = "1.20.4"
    val avroVersion = "1.12.0"
    val confluentVersion = "7.8.0"

    implementation(project(":common"))

    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    implementation("net.datafaker:datafaker:2.4.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    // Avro and Schema Registry dependencies
    implementation("org.apache.avro:avro:$avroVersion")
    implementation("io.confluent:kafka-avro-serializer:$confluentVersion")
    implementation("io.confluent:kafka-schema-registry-client:$confluentVersion")

    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")

    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testImplementation("org.testcontainers:kafka:$testcontainersVersion")
    testImplementation("com.github.testcontainers-all-things-kafka:cp-testcontainers:0.2.1")
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
}

avro {
    setCreateSetters(true)
    setCreateOptionalGetters(false)
    setGettersReturnOptional(false)
    setOptionalGettersForNullableFieldsOnly(false)
    setFieldVisibility("PRIVATE")
}

sourceSets {
    main {
        java {
            srcDir("build/generated-main-avro-java")
        }
    }
}

application {
    mainClass.set("com.example.streaming.generator.AvroFlightDataGenerator")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.example.streaming.generator.AvroFlightDataGenerator"
    }

    // Make sure to depend on common project's jar task
    dependsOn(":common:jar")

    // Include all dependencies in the JAR
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.test {
    useJUnitPlatform()
}