plugins {
    `java-library`
}

val jacksonVersion: String by rootProject.extra
val lombokVersion: String by rootProject.extra

dependencies {
    // Jackson for JSON serialization
    api("com.fasterxml.jackson.core:jackson-databind:${jacksonVersion}")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${jacksonVersion}")
    
    // Lombok
    compileOnly("org.projectlombok:lombok:${lombokVersion}")
    annotationProcessor("org.projectlombok:lombok:${lombokVersion}")
    
    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.15.2")
    
    testCompileOnly("org.projectlombok:lombok:${lombokVersion}")
    testAnnotationProcessor("org.projectlombok:lombok:${lombokVersion}")
}
