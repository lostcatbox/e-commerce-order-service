// 플러그인과 공통 설정은 root build.gradle.kts에서 적용됨

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.0")
    }
}

dependencies {
    // Common 모듈 의존성
    implementation(project(":common"))

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Spring Boot Core (Producer 서버)
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.retry:spring-retry")
    implementation("org.springframework:spring-aspects")
    // Spring Data Redis (캐싱)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // DB
    runtimeOnly("com.mysql:mysql-connector-j")

    // Redisson
    implementation("org.redisson:redisson-spring-boot-starter:3.24.3")

    // Spring Data Redis (캐싱용 - 상품 관련)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Jackson Kotlin Module
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Kafka Producer
    implementation("org.springframework.kafka:spring-kafka")

    // Spring Cloud OpenFeign
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mysql")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("io.rest-assured:rest-assured:5.4.0")
    testImplementation("io.rest-assured:kotlin-extensions:5.4.0")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// 테스트 설정은 root build.gradle.kts에서 적용됨

// jar 파일명 설정
tasks.bootJar {
    archiveFileName.set("coupon.jar")
}

tasks.jar {
    enabled = false
}
