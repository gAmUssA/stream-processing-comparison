plugins {
    application
    java
}

dependencies {
    val kafkaVersion: String by rootProject.extra
    val lombokVersion: String by rootProject.extra
    val slf4jVersion = "2.0.17"
    val logbackVersion = "1.5.19"
    val testcontainersVersion = "1.21.3"

    implementation(project(":common"))

    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    implementation("net.datafaker:datafaker:2.5.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.20.0")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")

    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:5.14.0")
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