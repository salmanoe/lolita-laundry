plugins {
    java
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "id.co.lolita.laundry"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

// Lombok is declared once (compileOnly + annotationProcessor); test configs inherit it.
configurations {
    testCompileOnly { extendsFrom(configurations.compileOnly.get()) }
    testAnnotationProcessor { extendsFrom(configurations.annotationProcessor.get()) }
}

dependencyManagement {
    imports {
        // Spring Modulith 2.x targets Spring Boot 4.x + Spring Framework 7.
        // (1.x targeted Boot 3.x — do not use 1.x here.)
        mavenBom("org.springframework.modulith:spring-modulith-bom:2.0.5")
        // AWS SDK v2 BOM — for S3-compatible storage (MinIO dev / Cloudflare R2 prod)
        mavenBom("software.amazon.awssdk:bom:2.31.0")
    }
}

dependencies {
    // ── Core Spring Boot starters ──
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // ── Flyway migrations ──
    // spring-boot-starter-flyway is required (not just flyway-core) — Spring Boot 4.0
    // restructured autoconfigure into per-feature artifacts, so flyway-core alone no
    // longer triggers the Flyway autoconfiguration. The starter pulls in the right
    // autoconfigure artifact.
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    // Flyway 10+ requires a separate DB-specific module for PostgreSQL support.
    implementation("org.flywaydb:flyway-database-postgresql")

    // ── Spring Modulith 2.0 — module boundary enforcement ──
    // starter-core: module detection + boundary verification.
    // starter-jpa: JPA-backed event publication registry (persisted domain events) — added
    // in Phase 3 for the order → billing OrderDeliveredEvent flow. Persisted events survive a
    // crash between publish and listener completion and are retried on restart. Requires the
    // event_publication table: created by Flyway V5 in prod/dev; in tests Hibernate ddl-auto
    // builds it from Modulith's JPA entity (Flyway is disabled there).
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    implementation("org.springframework.modulith:spring-modulith-starter-jpa")

    // ── JasperReports — Order Invoice + Monthly Billing PDFs (Phase 3) ──
    // 7.x is the Jakarta-based line (matches Spring Boot 4 / Jakarta EE 11). Split into
    // feature modules: -pdf adds the OpenPDF export backend, -jdt the Eclipse compiler used
    // to compile report expressions at fill time. Layouts are built programmatically via the
    // JasperDesign API in JasperPdfAdapter (not .jrxml), compiled once and cached.
    implementation("net.sf.jasperreports:jasperreports:7.0.1")
    implementation("net.sf.jasperreports:jasperreports-pdf:7.0.1")
    implementation("net.sf.jasperreports:jasperreports-jdt:7.0.1")

    // ── Apache POI — Excel (.xlsx) export of the reports (Phase 5) ──
    // poi-ooxml pulls in poi (core) + the OOXML stack (xmlbeans, commons-compress). Standalone —
    // no servlet/Jakarta coupling, so any 5.x works under Spring Boot 4.
    implementation("org.apache.poi:poi-ooxml:5.4.1")

    // ── AWS SDK v2 — S3-compatible (MinIO in dev, Cloudflare R2 in prod) ──
    implementation("software.amazon.awssdk:s3")

    // ── Bucket4j — in-memory per-IP rate limiting for the public order endpoints ──
    // Bucket4j 8.10+ ships JDK-specific core artifacts; the jdk17 build runs on Temurin 25.
    implementation("com.bucket4j:bucket4j_jdk17-core:8.14.0")

    // ── Lombok — reduces boilerplate on DTOs and config beans ──
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // ── PostgreSQL driver ──
    runtimeOnly("org.postgresql:postgresql")

    // ── H2 in-memory DB (test only — replaces PostgreSQL so tests run without Docker) ──
    testRuntimeOnly("com.h2database:h2")

    // ── Test dependencies ──
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // Spring Boot 4.0 split test slices into per-feature artifacts; @DataJpaTest is no
    // longer transitive via starter-test and must be declared explicitly.
    testImplementation("org.springframework.boot:spring-boot-data-jpa-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    // ArchUnit — additional explicit architecture rules beyond Modulith's boundary check
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
