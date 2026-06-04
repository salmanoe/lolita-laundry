package id.co.lolita.laundry;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Two-layer architecture verification — no Spring context required.
 *
 * <p><b>Layer 1 — Spring Modulith:</b> {@code ApplicationModules.verify()} walks every
 * module's package tree and asserts that no module imports another module's non-public
 * classes. This is the coarse-grained boundary check.
 *
 * <p><b>Layer 2 — ArchUnit rules:</b> explicit, targeted checks for the hexagonal
 * conventions we enforce in this project. These produce clear violation messages that
 * name the offending class and rule. They catch things Modulith doesn't (e.g. an
 * {@code @Entity} sneaking into the domain package within a single module).
 *
 * <p>Note: the individual {@code @ApplicationModuleTest} tests in each module package
 * verify that each module's Spring context bootstraps correctly in isolation.
 * This class only checks static structure — it never starts a Spring context.
 */
class ModuleStructureTest {

    static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("id.co.lolita.laundry");
    }

    // ── Spring Modulith boundary check ────────────────────────────────────────

    @Test
    void modulesShouldRespectBoundaries() {
        ApplicationModules.of(BackendApplication.class).verify();
    }

    // ── ArchUnit hexagonal rules ──────────────────────────────────────────────

    @Test
    void domainClassesShouldNotDependOnSpringOrJpa() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "jakarta.persistence..")
                .because("Domain classes must be pure Java — no Spring or JPA imports")
                .check(classes);
    }

    @Test
    void controllersShouldOnlyDependOnPortInterfaces() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..adapter.in.web..")
                .should().dependOnClassesThat().resideInAPackage("..application..")
                .because("Controllers must depend on inbound port interfaces (domain.port.in), " +
                        "not on application service implementations")
                .check(classes);
    }

    @Test
    void jpaEntitiesShouldNotResideInDomain() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..domain..")
                .should().beAnnotatedWith("jakarta.persistence.Entity")
                .because("JPA @Entity classes belong in adapter/out/persistence, not in the domain")
                .check(classes);
    }
}
