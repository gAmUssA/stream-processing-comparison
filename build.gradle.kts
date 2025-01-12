plugins {
    id("java")
    id("org.asciidoctor.jvm.convert") version "4.0.4" apply false
}

allprojects {
    group = "com.example"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven {
            url = uri("https://repository.apache.org/content/repositories/snapshots")
            mavenContent {
                snapshotsOnly()
            }
        }
        maven {
            url = uri("https://repository.apache.org/content/repositories/releases")
        }
        maven {
            url = uri("https://packages.confluent.io/maven/")
        }
    }
}

subprojects {
    apply(plugin = "java")

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    }

    tasks.test {
        useJUnitPlatform()
    }
}

// Common versions for all modules
extra.apply {
extra["kafkaVersion"] = "3.9.0"
extra["flinkVersion"] = "1.17.1"  // Use a stable version that is available
extra["lombokVersion"] = "1.18.30"
extra["jacksonVersion"] = "2.15.3"
extra["slf4jVersion"] = "2.0.16"
    set("logbackVersion", "1.4.14")
    set("junitVersion", "5.10.1")
    set("mockitoVersion", "5.8.0")
    set("testcontainersVersion", "1.19.3")
}
