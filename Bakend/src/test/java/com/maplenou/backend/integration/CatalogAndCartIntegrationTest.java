package com.maplenou.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.maplenou.backend.auth.dto.RegisterRequest;
import com.maplenou.backend.cart.CartRepository;
import com.maplenou.backend.cart.FavoriteRepository;
import com.maplenou.backend.catalog.CategoryRepository;
import com.maplenou.backend.catalog.ProductRepository;
import com.maplenou.backend.catalog.ShopStatus;
import com.maplenou.backend.catalog.dto.*;
import com.maplenou.backend.cart.dto.AddFavoriteRequest;
import com.maplenou.backend.cart.dto.AddToCartRequest;
import com.maplenou.backend.cart.dto.UpdateCartItemRequest;
import com.maplenou.backend.catalog.ShopRepository;
import com.maplenou.backend.seller.SellerProfileRepository;
import com.maplenou.backend.seller.SellerStatus;
import com.maplenou.backend.seller.dto.ApplySellerRequest;
import com.maplenou.backend.seller.dto.UpdateSellerStatusRequest;
import com.maplenou.backend.user.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CatalogAndCartIntegrationTest extends AbstractIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private SellerProfileRepository sellerProfileRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private FavoriteRepository favoriteRepository;

    private final long rand = System.nanoTime() % 100_000_000L;

    private UUID buyerId;
    private UUID sellerId;
    private UUID sellerProfileId;
    private UUID shopId;
    private UUID parentCategoryId;
    private UUID subCategoryId;
    private UUID productId;
    private String buyerToken;
    private String sellerToken;
    private String adminToken;

    @Test
    void fullCatalogAndCartFlow() throws Exception {
        adminToken = login("+22890000000", "Micael2005@");

        // ----- Onboarding vendeur -----
        String buyerPhone = "+22899" + String.format("%07d", rand % 10_000_000L);
        String sellerPhone = "+22881" + String.format("%07d", rand % 10_000_000L);

        JsonNode buyerAuth = post("/api/auth/register", new RegisterRequest("Cart Buyer", buyerPhone, "Passw0rd!"), null, 201);
        buyerId = UUID.fromString(buyerAuth.get("user").get("id").asText());
        buyerToken = buyerAuth.get("accessToken").asText();

        JsonNode sellerAuth = post("/api/auth/register", new RegisterRequest("Catalog Seller", sellerPhone, "Passw0rd!"), null, 201);
        sellerId = UUID.fromString(sellerAuth.get("user").get("id").asText());

        JsonNode apply = post("/api/sellers/apply", new ApplySellerRequest("Boutique Catalogue " + rand),
                sellerAuth.get("accessToken").asText(), 201);
        sellerProfileId = UUID.fromString(apply.get("id").asText());
        patch("/api/sellers/admin/" + sellerProfileId + "/status",
                new UpdateSellerStatusRequest(SellerStatus.APPROVED, "ok"), adminToken, 200);
        sellerToken = login(sellerPhone, "Passw0rd!");

        // ----- Catégories (admin) -----
        JsonNode parentCat = post("/api/categories",
                new CreateCategoryRequest("Électronique " + rand, null, null, null), adminToken, 201);
        parentCategoryId = UUID.fromString(parentCat.get("id").asText());

        JsonNode subCat = post("/api/categories",
                new CreateCategoryRequest("Téléphones " + rand, null, parentCategoryId, null), adminToken, 201);
        subCategoryId = UUID.fromString(subCat.get("id").asText());

        JsonNode roots = get("/api/categories", null, 200);
        assertTrue(roots.isArray());
        JsonNode subs = get("/api/categories/" + parentCategoryId + "/subcategories", null, 200);
        assertEquals(1, subs.size());

        String slug = subCat.get("slug").asText();
        JsonNode bySlug = get("/api/categories/slug/" + slug, null, 200);
        assertEquals(subCategoryId.toString(), bySlug.get("id").asText());

        JsonNode updatedCat = patch("/api/categories/" + subCategoryId,
                new UpdateCategoryRequest("Téléphones MAJ " + rand, true, parentCategoryId, null), adminToken, 200);
        assertEquals("Téléphones MAJ " + rand, updatedCat.get("name").asText());

        // Un non-admin ne peut pas créer de catégorie
        post("/api/categories", new CreateCategoryRequest("Interdit", null, null, null), buyerToken, 403);

        // ----- Boutique -----
        JsonNode shop = post("/api/shops",
                new CreateShopRequest("Boutique Catalogue " + rand, "desc", "Lomé", "Centre", null), sellerToken, 201);
        shopId = UUID.fromString(shop.get("id").asText());
        assertEquals("PENDING", shop.get("status").asText());

        JsonNode mine = get("/api/shops/mine", sellerToken, 200);
        assertEquals(shopId.toString(), mine.get("id").asText());

        JsonNode updatedShop = patch("/api/shops/mine",
                new UpdateShopRequest("Boutique Catalogue MAJ " + rand, null, null, null, null, null, null),
                sellerToken, 200);
        assertEquals("Boutique Catalogue MAJ " + rand, updatedShop.get("name").asText());

        // Approbation admin (obligatoire pour créer des produits / passer commande)
        JsonNode approvedShop = patch("/api/admin/shops/" + shopId + "/status",
                new UpdateShopStatusRequest(ShopStatus.APPROVED), adminToken, 200);
        assertEquals("APPROVED", approvedShop.get("status").asText());

        String publicShopSlug = updatedShop.get("slug").asText();
        JsonNode publicShop = get("/api/shops/" + publicShopSlug, null, 200);
        assertEquals(shopId.toString(), publicShop.get("id").asText());

        JsonNode adminShopList = get("/api/admin/shops?status=APPROVED", adminToken, 200);
        assertTrue(adminShopList.get("content").isArray());

        // ----- Produits -----
        JsonNode createdProduct = post("/api/shops/mine/products",
                new CreateProductRequest("Téléphone Test " + rand, "Un bon téléphone",
                        new BigDecimal("15000.00"), null, subCategoryId,
                        List.of(new CreateVariantRequest("64Go", null, 10, "SKU-" + rand)), null),
                sellerToken, 201);
        productId = UUID.fromString(createdProduct.get("id").asText());
        assertEquals("DRAFT", createdProduct.get("status").asText());
        UUID variantId = UUID.fromString(createdProduct.get("variants").get(0).get("id").asText());

        // Prix trop bas rejeté
        post("/api/shops/mine/products",
                new CreateProductRequest("Trop pas cher", null, new BigDecimal("100.00"), null, subCategoryId,
                        List.of(new CreateVariantRequest("v", null, 1, "SKU-LOW-" + rand)), null),
                sellerToken, 400);

        JsonNode myProducts = get("/api/shops/mine/products", sellerToken, 200);
        assertTrue(myProducts.get("content").isArray());

        JsonNode myProductDetail = get("/api/shops/mine/products/" + productId, sellerToken, 200);
        assertEquals(productId.toString(), myProductDetail.get("id").asText());

        // Produit pas encore ACTIVE -> pas visible pour ajout panier
        post("/api/cart/items", new AddToCartRequest(variantId, 1), buyerToken, 400);

        JsonNode activated = patch("/api/shops/mine/products/" + productId,
                new UpdateProductRequest(null, null, null, null, null, com.maplenou.backend.catalog.ProductStatus.ACTIVE, null),
                sellerToken, 200);
        assertEquals("ACTIVE", activated.get("status").asText());

        // Variantes
        JsonNode newVariant = post("/api/shops/mine/products/" + productId + "/variants",
                new CreateVariantRequest("128Go", new BigDecimal("18000.00"), 5, "SKU-B-" + rand),
                sellerToken, 201);
        UUID variant2Id = UUID.fromString(newVariant.get("id").asText());

        JsonNode updatedVariant = patch("/api/shops/mine/products/" + productId + "/variants/" + variant2Id,
                new UpdateVariantRequest(null, null, 3, null), sellerToken, 200);
        assertEquals(3, updatedVariant.get("stockQuantity").asInt());
        delete("/api/shops/mine/products/" + productId + "/variants/" + variant2Id, sellerToken, 204);

        // Images
        JsonNode image = post("/api/shops/mine/products/" + productId + "/images",
                new AddImageRequest("https://res.cloudinary.com/demo/image/upload/v1/products/test.jpg", (short) 0),
                sellerToken, 201);
        UUID imageId = UUID.fromString(image.get("id").asText());
        // La suppression tente un best-effort Cloudinary (non configuré en dev) sans jamais faire échouer la requête
        delete("/api/shops/mine/products/" + productId + "/images/" + imageId, sellerToken, 204);

        // ----- Catalogue public -----
        JsonNode publicProduct = get("/api/products/" + createdProduct.get("slug").asText(), null, 200);
        assertEquals(productId.toString(), publicProduct.get("id").asText());

        JsonNode publicCatalog = get("/api/products?shopId=" + shopId + "&size=10", null, 200);
        assertTrue(publicCatalog.get("content").isArray());
        JsonNode filteredByPrice = get("/api/products?minPrice=1&maxPrice=1000000", null, 200);
        assertTrue(filteredByPrice.get("content").isArray());

        // ----- Panier -----
        JsonNode cartAfterAdd = post("/api/cart/items", new AddToCartRequest(variantId, 2), buyerToken, 200);
        assertEquals(1, cartAfterAdd.get("items").size());
        assertEquals(2, cartAfterAdd.get("items").get(0).get("quantity").asInt());
        UUID cartItemId = UUID.fromString(cartAfterAdd.get("items").get(0).get("cartItemId").asText());

        JsonNode cart = get("/api/cart", buyerToken, 200);
        assertEquals(1, cart.get("items").size());

        JsonNode cartAfterUpdate = patch("/api/cart/items/" + cartItemId, new UpdateCartItemRequest(5), buyerToken, 200);
        assertEquals(5, cartAfterUpdate.get("items").get(0).get("quantity").asInt());

        JsonNode cartAfterRemove = delete("/api/cart/items/" + cartItemId, buyerToken, 200);
        assertEquals(0, cartAfterRemove.get("items").size());

        // Re-ajouter puis vider entièrement
        post("/api/cart/items", new AddToCartRequest(variantId, 1), buyerToken, 200);
        delete("/api/cart", buyerToken, 204);
        JsonNode emptied = get("/api/cart", buyerToken, 200);
        assertEquals(0, emptied.get("items").size());

        // ----- Favoris -----
        JsonNode favorite = post("/api/favorites", new AddFavoriteRequest(productId), buyerToken, 201);
        assertEquals(productId.toString(), favorite.get("productId").asText());
        JsonNode favorites = get("/api/favorites", buyerToken, 200);
        assertTrue(favorites.get("content").size() >= 1);
        delete("/api/favorites/" + productId, buyerToken, 204);

        // Soft delete produit
        delete("/api/shops/mine/products/" + productId, sellerToken, 204);
    }

    @AfterAll
    void cleanup() {
        try { if (buyerId != null) favoriteRepository.findByUserIdAndProductId(buyerId, productId).ifPresent(favoriteRepository::delete); } catch (Exception ignored) { }
        try { if (buyerId != null) cartRepository.findByUserId(buyerId).ifPresent(cartRepository::delete); } catch (Exception ignored) { }
        try { if (productId != null) productRepository.deleteById(productId); } catch (Exception ignored) { }
        try { if (shopId != null) shopRepository.deleteById(shopId); } catch (Exception ignored) { }
        try { if (sellerProfileId != null) sellerProfileRepository.deleteById(sellerProfileId); } catch (Exception ignored) { }
        try { if (subCategoryId != null) categoryRepository.deleteById(subCategoryId); } catch (Exception ignored) { }
        try { if (parentCategoryId != null) categoryRepository.deleteById(parentCategoryId); } catch (Exception ignored) { }
        try { if (buyerId != null) userRepository.deleteById(buyerId); } catch (Exception ignored) { }
        try { if (sellerId != null) userRepository.deleteById(sellerId); } catch (Exception ignored) { }
    }
}
