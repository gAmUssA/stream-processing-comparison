plugins {
    `java-library`
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
}

val jacksonVersion: String by rootProject.extra
val lombokVersion: String by rootProject.extra
val avroVersion = "1.12.1"
val confluentVersion = "8.1.0"

dependencies {
    // Jackson for JSON serialization
    api("com.fasterxml.jackson.core:jackson-databind:${jacksonVersion}")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${jacksonVersion}")
    
    // Avro and Schema Registry dependencies
    api("org.apache.avro:avro:${avroVersion}")
    api("io.confluent:kafka-avro-serializer:${confluentVersion}")
    api("io.confluent:kafka-schema-registry-client:${confluentVersion}")
    
    // Lombok
    compileOnly("org.projectlombok:lombok:${lombokVersion}")
    annotationProcessor("org.projectlombok:lombok:${lombokVersion}")
    
    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.20.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.20.0")
    
    testCompileOnly("org.projectlombok:lombok:${lombokVersion}")
    testAnnotationProcessor("org.projectlombok:lombok:${lombokVersion}")
}

repositories {
    mavenCentral()
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
