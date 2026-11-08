package com.maplenou.support.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class PublicDeploymentGuardTest {

    private static MaplenouProperties props(String secret) {
        return new MaplenouProperties(new MaplenouProperties.Security(secret, "iss", null, false),
                null, null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    void localRunsAreNotChecked() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("demo");
        assertThat(PublicDeploymentGuard.check(env, props(""))).isEmpty();
    }

    @Test
    void onRenderTheDemoProfileAndAWeakSecretAreRefused() {
        MockEnvironment env = new MockEnvironment().withProperty("RENDER", "true");
        env.setActiveProfiles("demo");
        assertThat(PublicDeploymentGuard.check(env, props("demo-only-secret-change-me-0123456789abcdef"))).hasSize(2);
    }

    @Test
    void onRenderAProductionSetupStarts() {
        MockEnvironment env = new MockEnvironment().withProperty("RENDER", "true");
        assertThat(PublicDeploymentGuard.check(env, props("x9Q2v7Lw0pZr4Tn8Yb6Hc1Md5Kf3Gs7Ue0Ja2Wq4="))).isEmpty();
    }
}
