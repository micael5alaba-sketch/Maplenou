package com.maplenou.backend.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Configuration explicite de Flyway.
 *
 * Spring Boot 4.x ne déclenche pas toujours l'autoconfiguration Flyway.
 * On crée le bean manuellement et on force entityManagerFactory à en dépendre
 * via un BeanFactoryPostProcessor, garantissant que Flyway migre
 * AVANT qu'Hibernate ne valide le schéma.
 */
@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .validateOnMigrate(true)
                .outOfOrder(false)
                .load();
    }

    /**
     * Force l'EntityManagerFactory à dépendre du bean "flyway".
     * Sans cela, Hibernate peut valider le schéma avant que Flyway ne le crée.
     */
    @Bean
    public static BeanFactoryPostProcessor flywayJpaDependencyPostProcessor() {
        return beanFactory -> {
            String[] jpaFactoryBeans = beanFactory.getBeanNamesForType(
                    jakarta.persistence.EntityManagerFactory.class, true, false);
            for (String name : jpaFactoryBeans) {
                BeanDefinition bd = beanFactory.getBeanDefinition(name);
                bd.setDependsOn(mergeWithFlyway(bd.getDependsOn()));
            }
            // Aussi pour le bean interne Spring "entityManagerFactory"
            try {
                BeanDefinition emf = beanFactory.getBeanDefinition("entityManagerFactory");
                emf.setDependsOn(mergeWithFlyway(emf.getDependsOn()));
            } catch (Exception ignored) { }
        };
    }

    private static String[] mergeWithFlyway(String[] existing) {
        if (existing == null) return new String[]{"flyway"};
        for (String dep : existing) {
            if ("flyway".equals(dep)) return existing;
        }
        String[] merged = new String[existing.length + 1];
        System.arraycopy(existing, 0, merged, 0, existing.length);
        merged[existing.length] = "flyway";
        return merged;
    }
}
