package com.maplenou.backend.catalog;

import com.maplenou.backend.catalog.dto.*;
import com.maplenou.backend.common.CursorPage;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.media.CloudinaryUrlUtils;
import com.maplenou.backend.media.MediaService;
import com.maplenou.backend.review.ProductReviewRepository;
import com.maplenou.backend.review.dto.RatingSummaryResponse;
import com.maplenou.backend.user.User;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final ProductReviewRepository productReviewRepository;
    private final ShopService shopService;
    private final CategoryService categoryService;
    private final MediaService mediaService;

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final BigDecimal MIN_PRICE = new BigDecimal("500.00");

    // ----- Vendeur : produits -----

    @Transactional
    public ProductDetailResponse create(User owner, CreateProductRequest request) {
        Shop shop = shopService.getMine(owner);
        requireApprovedShop(shop);

        if (request.basePrice().compareTo(MIN_PRICE) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Le prix minimum est 500 FCFA");
        }

        Category category = categoryService.getById(request.categoryId());
        String slug = generateUniqueSlug(request.name());

        Product product = Product.builder()
                .shop(shop)
                .category(category)
                .name(request.name())
                .slug(slug)
                .description(request.description())
                .basePrice(request.basePrice())
                .oldPrice(request.oldPrice())
                .specifications(request.specifications() != null ? request.specifications() : new ArrayList<>())
                .status(ProductStatus.DRAFT)
                .deleted(false)
                .build();

        for (CreateVariantRequest vReq : request.variants()) {
            String sku = resolveSku(vReq.sku(), slug);
            product.getVariants().add(ProductVariant.builder()
                    .product(product)
                    .label(vReq.label())
                    .priceOverride(vReq.priceOverride())
                    .stockQuantity(vReq.stockQuantity())
                    .sku(sku)
                    .build());
        }

        Product saved = productRepository.save(product);
        // Rechargement avec JOIN FETCH pour avoir les associations complètes
        return ProductDetailResponse.from(loadWithDetails(saved.getId()));
    }

    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> listMine(User owner, Pageable pageable) {
        Shop shop = shopService.getMine(owner);
        // EntityGraph appliqué dans le repository → shop, category, images chargés
        Page<Product> page = productRepository.findByShopIdAndDeletedFalse(shop.getId(), pageable);
        Map<UUID, RatingSummaryResponse> ratings = loadRatingStats(page.getContent());
        return page.map(p -> toSummaryWithRating(p, ratings));
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getMineDetail(User owner, UUID productId) {
        Shop shop = shopService.getMine(owner);
        Product product = loadWithDetails(productId);
        requireOwnership(shop, product);
        return ProductDetailResponse.from(product);
    }

    @Transactional
    public ProductDetailResponse update(User owner, UUID productId, UpdateProductRequest request) {
        Shop shop = shopService.getMine(owner);
        Product product = getEntityById(productId);
        requireOwnership(shop, product);

        if (request.name() != null && !request.name().isBlank()) {
            product.setName(request.name());
        }
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (request.basePrice() != null) {
            if (request.basePrice().compareTo(MIN_PRICE) < 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Le prix minimum est 500 FCFA");
            }
            product.setBasePrice(request.basePrice());
        }
        if (request.categoryId() != null) {
            product.setCategory(categoryService.getById(request.categoryId()));
        }
        if (request.status() != null) {
            product.setStatus(request.status());
        }
        if (request.oldPrice() != null) {
            product.setOldPrice(request.oldPrice());
        }
        if (request.specifications() != null) {
            product.setSpecifications(request.specifications());
        }

        productRepository.save(product);
        return ProductDetailResponse.from(loadWithDetails(productId));
    }

    @Transactional
    public void softDelete(User owner, UUID productId) {
        Shop shop = shopService.getMine(owner);
        Product product = getEntityById(productId);
        requireOwnership(shop, product);
        product.setDeleted(true);
        product.setStatus(ProductStatus.ARCHIVED);
        productRepository.save(product);
    }

    // ----- Vendeur : variantes -----

    @Transactional
    public VariantResponse addVariant(User owner, UUID productId, CreateVariantRequest request) {
        Shop shop = shopService.getMine(owner);
        Product product = getEntityById(productId);
        requireOwnership(shop, product);

        String sku = resolveSku(request.sku(), product.getSlug());

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .label(request.label())
                .priceOverride(request.priceOverride())
                .stockQuantity(request.stockQuantity())
                .sku(sku)
                .build();

        return VariantResponse.from(variantRepository.save(variant));
    }

    @Transactional
    public VariantResponse updateVariant(User owner, UUID productId, UUID variantId,
                                          UpdateVariantRequest request) {
        Shop shop = shopService.getMine(owner);
        Product product = getEntityById(productId);
        requireOwnership(shop, product);

        ProductVariant variant = variantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Variante introuvable"));

        if (request.label() != null && !request.label().isBlank()) {
            variant.setLabel(request.label());
        }
        if (request.priceOverride() != null) {
            variant.setPriceOverride(request.priceOverride());
        }
        if (request.stockQuantity() != null) {
            variant.setStockQuantity(request.stockQuantity());
        }
        if (request.sku() != null && !request.sku().isBlank() && !request.sku().equals(variant.getSku())) {
            if (variantRepository.existsBySku(request.sku())) {
                throw new ApiException(HttpStatus.CONFLICT, "SKU déjà utilisé : " + request.sku());
            }
            variant.setSku(request.sku());
        }

        return VariantResponse.from(variantRepository.save(variant));
    }

    @Transactional
    public void deleteVariant(User owner, UUID productId, UUID variantId) {
        Shop shop = shopService.getMine(owner);
        Product product = getEntityById(productId);
        requireOwnership(shop, product);

        ProductVariant variant = variantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Variante introuvable"));
        variantRepository.delete(variant);
    }

    // ----- Vendeur : images -----

    @Transactional
    public ImageResponse addImage(User owner, UUID productId, AddImageRequest request) {
        Shop shop = shopService.getMine(owner);
        Product product = getEntityById(productId);
        requireOwnership(shop, product);

        ProductImage image = ProductImage.builder()
                .product(product)
                .url(request.url())
                .position(request.position())
                .build();

        return ImageResponse.from(imageRepository.save(image));
    }

    @Transactional
    public void deleteImage(User owner, UUID productId, UUID imageId) {
        Shop shop = shopService.getMine(owner);
        Product product = getEntityById(productId);
        requireOwnership(shop, product);

        ProductImage image = imageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Image introuvable"));
        imageRepository.delete(image);
        mediaService.deleteAssetQuietly(CloudinaryUrlUtils.extractPublicId(image.getUrl()));
    }

    // ----- Public : catalogue (keyset pagination) -----

    @Transactional(readOnly = true)
    public CursorPage<ProductSummaryResponse> listCatalog(UUID categoryId, UUID shopId,
                                                           BigDecimal minPrice, BigDecimal maxPrice,
                                                           String cursor, int size) {
        Specification<Product> spec = buildCatalogSpec(categoryId, shopId, minPrice, maxPrice);

        if (cursor != null) {
            CursorData data = decodeCursor(cursor);
            Instant afterCreatedAt = data.createdAt();
            UUID afterId = data.id();

            // Keyset : résultats antérieurs au dernier item vu (pas d'OFFSET)
            spec = spec.and((root, query, cb) -> {
                var byDate = cb.lessThan(root.get("createdAt"), afterCreatedAt);
                var sameDate = cb.equal(root.get("createdAt"), afterCreatedAt);
                var byId = cb.lessThan(root.<UUID>get("id"), afterId);
                return cb.or(byDate, cb.and(sameDate, byId));
            });
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        // +1 pour détecter s'il y a une page suivante sans COUNT(*)
        Pageable pageable = PageRequest.of(0, size + 1, sort);
        // EntityGraph appliqué dans le repository → shop, category, images chargés
        List<Product> results = productRepository.findAll(spec, pageable).getContent();

        boolean hasNext = results.size() > size;
        List<Product> page = hasNext ? results.subList(0, size) : results;

        String nextCursor = null;
        if (hasNext) {
            Product last = page.get(page.size() - 1);
            nextCursor = encodeCursor(last.getCreatedAt(), last.getId());
        }

        Map<UUID, RatingSummaryResponse> ratings = loadRatingStats(page);

        return new CursorPage<>(
                page.stream().map(p -> toSummaryWithRating(p, ratings)).toList(),
                nextCursor,
                hasNext
        );
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getDetailBySlug(String slug) {
        Product product = productRepository.findBySlugWithDetails(slug)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Produit introuvable"));
        return ProductDetailResponse.from(product);
    }

    // ----- Utilitaires privés -----

    private Product loadWithDetails(UUID id) {
        return productRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Produit introuvable"));
    }

    private Product getEntityById(UUID id) {
        return productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Produit introuvable"));
    }

    private Specification<Product> buildCatalogSpec(UUID categoryId, UUID shopId,
                                                     BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), ProductStatus.ACTIVE));
            predicates.add(cb.isFalse(root.get("deleted")));
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (shopId != null) {
                predicates.add(cb.equal(root.get("shop").get("id"), shopId));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("basePrice"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("basePrice"), maxPrice));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void requireApprovedShop(Shop shop) {
        if (shop.getStatus() != ShopStatus.APPROVED) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Votre boutique doit être approuvée pour publier des produits");
        }
    }

    private void requireOwnership(Shop shop, Product product) {
        if (!product.getShop().getId().equals(shop.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Ce produit n'appartient pas à votre boutique");
        }
    }

    private String generateUniqueSlug(String name) {
        String base = slugify(name);
        String slug = base;
        int suffix = 1;
        while (productRepository.existsBySlug(slug)) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }

    private String slugify(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
        String slug = NON_ALPHANUMERIC.matcher(normalized).replaceAll("-");
        return slug.replaceAll("^-+|-+$", "");
    }

    /** SKU fourni tel quel (après vérification d'unicité) ou généré automatiquement si absent. */
    private String resolveSku(String providedSku, String productSlug) {
        if (providedSku != null && !providedSku.isBlank()) {
            if (variantRepository.existsBySku(providedSku)) {
                throw new ApiException(HttpStatus.CONFLICT, "SKU déjà utilisé : " + providedSku);
            }
            return providedSku;
        }
        return generateUniqueSku(productSlug);
    }

    private String generateUniqueSku(String base) {
        String prefix = base.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "-");
        String sku;
        do {
            sku = prefix + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        } while (variantRepository.existsBySku(sku));
        return sku;
    }

    /** Notes moyennes + nombre d'avis de plusieurs produits en une seule requête (évite le N+1). */
    private Map<UUID, RatingSummaryResponse> loadRatingStats(List<Product> products) {
        if (products.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = products.stream().map(Product::getId).toList();
        return productReviewRepository.findRatingStatsByProductIds(ids).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> new RatingSummaryResponse(
                                ((Number) row[1]).doubleValue(),
                                ((Number) row[2]).longValue())
                ));
    }

    private ProductSummaryResponse toSummaryWithRating(Product p, Map<UUID, RatingSummaryResponse> ratings) {
        RatingSummaryResponse stats = ratings.getOrDefault(p.getId(), new RatingSummaryResponse(0.0, 0L));
        return ProductSummaryResponse.from(p, stats.averageRating(), stats.reviewCount());
    }

    private String encodeCursor(Instant createdAt, UUID id) {
        String raw = createdAt.toEpochMilli() + "|" + id;
        return Base64.getUrlEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private CursorData decodeCursor(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|");
            return new CursorData(
                    Instant.ofEpochMilli(Long.parseLong(parts[0])),
                    UUID.fromString(parts[1])
            );
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Curseur de pagination invalide");
        }
    }

    private record CursorData(Instant createdAt, UUID id) {}
}
