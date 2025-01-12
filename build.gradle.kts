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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    dependencies {
        // If using JUnit Jupiter
        testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    tasks.test {
        useJUnitPlatform()
    }
}

// Common versions for all modules
extra.apply {
    extra["kafkaVersion"] = "3.9.0"
    extra["flinkVersion"] = "1.20.0"  // Use a stable version that is available
    extra["lombokVersion"] = "1.18.36"
    extra["jacksonVersion"] = "2.18.2"
    extra["slf4jVersion"] = "2.0.16"
    set("logbackVersion", "1.5.16")
    set("junitVersion", "5.11.4")
    set("mockitoVersion", "5.15.2")
    set("testcontainersVersion", "1.19.3")
}
