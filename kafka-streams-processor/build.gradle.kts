plugins {
    application
}

dependencies {
    val kafkaVersion: String by rootProject.extra
    val lombokVersion: String by rootProject.extra

    implementation(project(":common"))
    
    implementation("org.apache.kafka:kafka-streams:$kafkaVersion")
    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    
    testImplementation("org.apache.kafka:kafka-streams-test-utils:$kafkaVersion")
}

application {
    mainClass.set("com.example.kafka.StreamProcessor")
}
