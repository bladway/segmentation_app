plugins {
    id("java")
    id("org.springframework.boot") version "3.4.5"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "ru.vsu.cs.bladway"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter:3.4.5")
    implementation("org.springframework.boot:spring-boot-starter-web:3.4.5")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf:3.4.5")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.4.5")
    developmentOnly("org.springframework.boot:spring-boot-devtools:3.4.5")

    implementation("org.jfree:jfreechart:1.5.5")
    implementation("org.openpnp:opencv:4.9.0-0")
    implementation("commons-io:commons-io:2.19.0")
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
    runtimeOnly("org.postgresql:postgresql:42.7.5")
}

tasks.wrapper {
    gradleVersion = "8.14"
}
