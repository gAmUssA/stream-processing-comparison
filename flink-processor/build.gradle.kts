plugins {
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

val flinkVersion: String by rootProject.extra
val lombokVersion: String by rootProject.extra
val slf4jVersion: String by rootProject.extra
val logbackVersion: String by rootProject.extra

// Configure the application plugin
application {
    mainClass.set("com.example.flink.FlinkProcessor")
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
    implementation("org.apache.flink:flink-connector-kafka:${flinkVersion}")
    implementation("org.apache.flink:flink-runtime-web:${flinkVersion}")
    implementation("org.apache.flink:flink-java:${flinkVersion}")
    
    // Add logging framework
    implementation("org.slf4j:slf4j-api:${slf4jVersion}")
    implementation("ch.qos.logback:logback-classic:${logbackVersion}")
    
    // Lombok
    compileOnly("org.projectlombok:lombok:${lombokVersion}")
    annotationProcessor("org.projectlombok:lombok:${lombokVersion}")

    // Mark Flink dependencies as 'provided' for the shadow JAR
    flinkShadowJar("org.apache.flink:flink-streaming-java:${flinkVersion}")
    flinkShadowJar("org.apache.flink:flink-clients:${flinkVersion}")
    flinkShadowJar("org.apache.flink:flink-connector-base:${flinkVersion}")
    flinkShadowJar("org.apache.flink:flink-connector-kafka:${flinkVersion}")
    flinkShadowJar("org.apache.flink:flink-runtime-web:${flinkVersion}")
    flinkShadowJar("org.apache.flink:flink-java:${flinkVersion}")
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
