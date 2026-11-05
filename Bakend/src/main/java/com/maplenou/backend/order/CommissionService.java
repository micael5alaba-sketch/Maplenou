package com.maplenou.backend.order;

import com.maplenou.backend.catalog.Product;
import com.maplenou.backend.catalog.ProductVariant;
import com.maplenou.backend.catalog.Shop;
import com.maplenou.backend.seller.SellerFeeCategory;
import com.maplenou.backend.seller.SellerProfile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Commission Maplenou INTEGREE au prix affiche (document "Frais vendeurs et commissions
 * acheteurs", qui remplace l'ancien bareme 25 % / 22,5 % preleve sur le vendeur).
 *
 * Le vendeur definit un prix de base ; l'acheteur voit et paie ce prix de base + la commission,
 * calculee sur le prix de base (hors livraison) selon la categorie tarifaire du vendeur :
 *   Categorie 1 - Standard      : 5 %
 *   Categorie 2 - Prix eleves   : 3 %
 * La commission n'est jamais un supplement ajoute au paiement et n'est pas preleve sur le solde
 * du vendeur : celui-ci recoit son prix de base (net = somme des prix de base).
 *
 * Arrondi : la commission est arrondie au FCFA entier PAR UNITE (puis multipliee par la quantite),
 * pour que prix affiche, total de ligne et commission totale restent toujours coherents.
 *
 * Methodes statiques : les DTO de reponse (prix affiche des fiches produit, panier, favoris) en
 * ont besoin sans acces aux beans Spring. Isole ici, comme avant, pour rester facile a ajuster.
 */
@Service
public class CommissionService {

    private static final BigDecimal RATE_STANDARD = new BigDecimal("0.0500");
    private static final BigDecimal RATE_HIGH_PRICE = new BigDecimal("0.0300");

    public static BigDecimal rateFor(SellerFeeCategory category) {
        return category == SellerFeeCategory.HIGH_PRICE ? RATE_HIGH_PRICE : RATE_STANDARD;
    }

    /** Commission incluse dans le prix affiche d'UNE unite (FCFA entier, 2 decimales de stockage). */
    public static BigDecimal commissionPerUnit(BigDecimal basePrice, SellerFeeCategory category) {
        return basePrice.multiply(rateFor(category))
                .setScale(0, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.UNNECESSARY);
    }

    /** Prix affiche a l'acheteur : prix de base du vendeur + commission integree. */
    public static BigDecimal displayPrice(BigDecimal basePrice, SellerFeeCategory category) {
        return basePrice.add(commissionPerUnit(basePrice, category));
    }

    /** Categorie tarifaire du vendeur proprietaire de la boutique (Standard si pas de profil). */
    public static SellerFeeCategory categoryOf(Shop shop) {
        SellerProfile profile = shop.getOwner().getSellerProfile();
        return profile != null ? profile.getFeeCategory() : SellerFeeCategory.STANDARD;
    }

    /** Prix de base effectif d'une variante : surcharge de variante, sinon prix de base du produit. */
    public static BigDecimal effectiveBasePrice(ProductVariant variant) {
        return variant.getPriceOverride() != null
                ? variant.getPriceOverride()
                : variant.getProduct().getBasePrice();
    }

    /** Prix affiche (commission incluse) pour un montant de base d'un produit donne. */
    public static BigDecimal displayPriceFor(Product product, BigDecimal basePrice) {
        return basePrice == null ? null : displayPrice(basePrice, categoryOf(product.getShop()));
    }
}
