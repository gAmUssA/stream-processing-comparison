plugins {
    application
}

dependencies {
    val kafkaVersion: String by rootProject.extra
    val lombokVersion: String by rootProject.extra
    val jacksonVersion: String by rootProject.extra
    val slf4jVersion: String by rootProject.extra
    val logbackVersion: String by rootProject.extra
    val junitVersion: String by rootProject.extra
    val mockitoVersion: String by rootProject.extra

    implementation(project(":common"))
    
    // Kafka
    implementation("org.apache.kafka:kafka-streams:$kafkaVersion")
    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    
    // Jackson for JSON
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    
    // Logging
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    
    // Lombok
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    
    // Testing
    testImplementation("org.apache.kafka:kafka-streams-test-utils:$kafkaVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("org.mockito:mockito-junit-jupiter:$mockitoVersion")
}

application {
    mainClass.set("com.example.streaming.kafka.FlightDelayProcessor")
}

tasks.test {
    useJUnitPlatform()
}
