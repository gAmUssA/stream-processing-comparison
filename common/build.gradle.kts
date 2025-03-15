plugins {
    `java-library`
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
}

val jacksonVersion: String by rootProject.extra
val lombokVersion: String by rootProject.extra
val avroVersion = "1.12.0"
val confluentVersion = "7.9.0"

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
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.16.1")
    testImplementation("org.mockito:mockito-junit-jupiter:5.16.1")
    
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
