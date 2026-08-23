package com.maplenou.backend.media;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CloudinaryUrlUtilsTest {

    @Test
    void extractsPublicIdFromVersionedUrl() {
        String url = "https://res.cloudinary.com/demo/image/upload/v1690000000/products/abc123.jpg";
        assertEquals("products/abc123", CloudinaryUrlUtils.extractPublicId(url));
    }

    @Test
    void extractsPublicIdWithoutVersion() {
        String url = "https://res.cloudinary.com/demo/image/upload/products/abc123.png";
        assertEquals("products/abc123", CloudinaryUrlUtils.extractPublicId(url));
    }

    @Test
    void extractsPublicIdWithoutExtension() {
        String url = "https://res.cloudinary.com/demo/image/upload/v42/proof-of-delivery/xyz";
        assertEquals("proof-of-delivery/xyz", CloudinaryUrlUtils.extractPublicId(url));
    }

    @Test
    void returnsNullForNonCloudinaryUrl() {
        assertNull(CloudinaryUrlUtils.extractPublicId("https://example.com/image.jpg"));
    }

    @Test
    void returnsNullForNullInput() {
        assertNull(CloudinaryUrlUtils.extractPublicId(null));
    }
}
