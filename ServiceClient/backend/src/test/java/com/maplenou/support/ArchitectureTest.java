package com.maplenou.support;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Hexagonal architecture guard (reference-architecture §3, DEC-CS11-01). Each business module is
 * {@code <module>.domain} (pure Java), {@code <module>.application} (use cases + {@code port}) and
 * {@code <module>.adapter.in|out} (Spring web, JPA, HTTP clients…). The build fails when a rule is broken.
 */
class ArchitectureTest {

    private static final String ROOT = "com.maplenou.support";
    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT);
    }

    @Test
    void domainIsFrameworkFree() {
        ArchRule rule = noClasses().that().resideInAPackage(ROOT + "..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", "org.hibernate..", "tools.jackson..",
                        "com.fasterxml..", "io.micrometer..", "com.google.firebase..",
                        ROOT + "..application..", ROOT + "..adapter..", ROOT + ".platform..", ROOT + ".demo..")
                .because("business rules must stay testable without Spring, a database or the network");
        rule.check(classes);
    }

    @Test
    void applicationDependsOnPortsNotOnTechnology() {
        ArchRule rule = noClasses().that().resideInAPackage(ROOT + "..application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "jakarta.persistence..", "org.hibernate..", "org.springframework.data..",
                        "org.springframework.web..", "org.springframework.http..", "org.springframework.security..",
                        "org.springframework.jdbc..", "jakarta.servlet..", "com.google.firebase..",
                        ROOT + "..adapter..", ROOT + ".platform..", ROOT + ".demo..")
                .because("use cases talk to the outside world only through their ports");
        rule.check(classes);
    }

    @Test
    void inboundAdaptersDoNotUseOutboundAdapters() {
        noClasses().that().resideInAPackage(ROOT + "..adapter.in..")
                .should().dependOnClassesThat().resideInAPackage(ROOT + "..adapter.out..")
                .because("a controller or listener goes through an application service, never straight to a repository")
                .check(classes);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ticket", "sla", "notification", "attachment", "context", "identity", "report"})
    void modulesOnlySeeEachOthersApplicationAndDomain(String module) {
        noClasses().that().resideOutsideOfPackages(ROOT + "." + module + "..", ROOT + ".demo..")
                .should().dependOnClassesThat().resideInAPackage(ROOT + "." + module + ".adapter..")
                .because("adapters are private to their module")
                .check(classes);
    }

    @Test
    void businessModulesHaveNoCycles() {
        slices().matching(ROOT + ".(ticket|sla|notification|attachment|context|identity|report)..")
                .should().beFreeOfCycles()
                .check(classes);
    }

    @Test
    void sharedKernelAndPlatformDoNotDependOnModules() {
        noClasses().that().resideInAnyPackage(ROOT + ".shared..", ROOT + ".platform..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        ROOT + ".ticket..", ROOT + ".sla..", ROOT + ".notification..", ROOT + ".attachment..",
                        ROOT + ".context..", ROOT + ".identity..", ROOT + ".report..", ROOT + ".demo..")
                .check(classes);
    }

    @Test
    void onlyTheDemoPackageUsesDemoCode() {
        noClasses().that().resideOutsideOfPackage(ROOT + ".demo..")
                .and().doNotHaveSimpleName("MaplenouSupportApplication")
                .should().dependOnClassesThat().resideInAPackage(ROOT + ".demo..")
                .because("the demo profile must be removable without touching production code")
                .check(classes);
    }

    @Test
    void jpaEntitiesLiveInPersistenceAdapters() {
        classes().that().areAnnotatedWith("jakarta.persistence.Entity")
                .or().areAnnotatedWith("jakarta.persistence.MappedSuperclass")
                .should().resideInAnyPackage(ROOT + "..adapter.out.persistence..", ROOT + ".platform..")
                .check(classes);
    }

    @Test
    void controllersLiveInInboundWebAdapters() {
        classes().that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .should().resideInAnyPackage(ROOT + "..adapter.in.web..", ROOT + ".demo..")
                .check(classes);
    }
}
