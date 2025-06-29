plugins {
    id("org.springframework.boot") version "3.2.5" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    kotlin("jvm") version "2.0.0" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}
