plugins {
    application
    id("com.gradleup.shadow") version "8.3.6"
}

val flinkVersion = "1.20.0"
val kafkaVersion: String by rootProject.extra
val lombokVersion: String by rootProject.extra
val slf4jVersion: String by rootProject.extra
val logbackVersion: String by rootProject.extra
val junitVersion = "5.12.0"

// Configure the application plugin
application {
    mainClass.set("com.example.streaming.flink.generator.DataGeneratorJob")
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
}

configurations {
    create("flinkShadowJar") {
        isCanBeResolved = true
        isCanBeConsumed = true
    }
}

val flinkShadowJar by configurations

dependencies {
    implementation(project(":common"))

    // Flink core dependencies
    implementation("org.apache.flink:flink-streaming-java:${flinkVersion}")
    implementation("org.apache.flink:flink-clients:${flinkVersion}")
    implementation("org.apache.flink:flink-connector-base:${flinkVersion}")
    implementation("org.apache.flink:flink-connector-kafka:3.4.0-1.20")
    implementation("org.apache.flink:flink-runtime-web:${flinkVersion}")
    implementation("org.apache.flink:flink-java:${flinkVersion}")
    
    // Add flink-avro dependency to fix the AvroTypeInfo class error
    implementation("org.apache.flink:flink-avro:${flinkVersion}")

    // Add logging framework
    implementation("org.slf4j:slf4j-api:${slf4jVersion}")
    implementation("ch.qos.logback:logback-classic:${logbackVersion}")

    // Lombok
    compileOnly("org.projectlombok:lombok:${lombokVersion}")
    annotationProcessor("org.projectlombok:lombok:${lombokVersion}")

    // Data generation
    implementation("net.datafaker:datafaker:2.4.2")

    // Testing dependencies
    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
    testImplementation("org.apache.flink:flink-test-utils:${flinkVersion}")
    testImplementation("org.apache.flink:flink-runtime:${flinkVersion}")
    testImplementation("org.apache.flink:flink-streaming-java:${flinkVersion}:tests")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.mockito:mockito-core:5.15.2")

    // Mark Flink dependencies as 'provided' for the shadow JAR
    flinkShadowJar("org.apache.flink:flink-streaming-java:${flinkVersion}")
    flinkShadowJar("org.apache.flink:flink-clients:${flinkVersion}")
    flinkShadowJar("org.apache.flink:flink-connector-base:${flinkVersion}")
    flinkShadowJar("org.apache.flink:flink-connector-kafka:3.4.0-1.20")
    flinkShadowJar("org.apache.flink:flink-runtime-web:${flinkVersion}")
    flinkShadowJar("org.apache.flink:flink-java:${flinkVersion}")
    flinkShadowJar("org.apache.flink:flink-avro:${flinkVersion}")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    jvmArgs = listOf(
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.time=ALL-UNNAMED"
    )
}

// Configure the shadow JAR
tasks.shadowJar {
    // Exclude Flink dependencies from the shadow JAR
    configurations = listOf(project.configurations.runtimeClasspath.get())
    configurations.remove(flinkShadowJar)

    archiveClassifier.set("all")

    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }

    // Relocate dependencies to avoid conflicts
    relocate("com.google.common", "shade.com.google.common")

    // Exclude signed jars
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

// Make the shadow JAR the default artifact
artifacts {
    add("archives", tasks.shadowJar)
}
