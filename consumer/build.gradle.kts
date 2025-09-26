// 플러그인과 공통 설정은 root build.gradle.kts에서 적용됨

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.0")
    }
}

dependencies {
    // Core 모듈 의존성 - 도메인과 서비스 로직 공유
    implementation(project(":core"))

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Spring Boot (Consumer는 웹 서버가 아니므로 최소 구성)
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.retry:spring-retry")
    implementation("org.springframework:spring-aspects")

    // DB
    runtimeOnly("com.mysql:mysql-connector-j")

    // Redisson (분산 락)
    implementation("org.redisson:redisson-spring-boot-starter:3.24.3")

    // Spring Data Redis (캐싱)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Jackson Kotlin Module
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Kafka Consumer
    implementation("org.springframework.kafka:spring-kafka")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mysql")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// 테스트 설정은 root build.gradle.kts에서 적용됨

// jar 파일명 설정
tasks.bootJar {
    archiveFileName.set("coupon-issue-consumer.jar")
}

tasks.jar {
    enabled = false
}
