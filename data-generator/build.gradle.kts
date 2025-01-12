plugins {
    application
}

dependencies {
    val kafkaVersion: String by rootProject.extra
    val lombokVersion: String by rootProject.extra
    val slf4jVersion = "2.0.16"
    val logbackVersion = "1.5.16"
    val testcontainersVersion = "1.20.4"

    implementation(project(":common"))
    
    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    implementation("com.github.javafaker:javafaker:1.0.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testImplementation("org.testcontainers:kafka:$testcontainersVersion")
}

application {
    mainClass.set("com.example.streaming.generator.FlightDataGenerator")
}

tasks.test {
    useJUnitPlatform()
}
