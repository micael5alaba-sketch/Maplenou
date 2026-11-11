package com.maplenou.backend.revenue;

import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.order.SubOrderStatus;
import com.maplenou.backend.revenue.dto.RevenueEntryResponse;
import com.maplenou.backend.revenue.dto.RevenueSource;
import com.maplenou.backend.revenue.dto.RevenueSummaryResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Registre des revenus de la plateforme : ce que Maplenou garde, source par source, avec la
 * référence de chaque ligne. Calculé directement depuis les données déjà enregistrées par les
 * commandes, livraisons et retraits (aucune saisie manuelle, donc aucune dérive possible) :
 * <ul>
 *   <li>commission : reconnue quand la sous-commande est LIVRÉE (comme les reversements vendeurs) ;
 *       une sous-commande annulée ou retournée n'y figure jamais ;</li>
 *   <li>part livraison : créée à chaque livraison terminée ;</li>
 *   <li>frais mensuels vendeurs : montants réellement déduits d'un reversement (et rétablis,
 *       donc retirés du registre, si le reversement est rejeté).</li>
 * </ul>
 * Les frais de l'agrégateur de paiement (FedaPay) ne sont pas encore déduits : ils ne sont pas
 * exposés par l'API de paiement, voir la documentation.
 */
@Service
@Transactional(readOnly = true)
public class RevenueService {

    /** Garde-fou : agrégation journalière bornée (volume actuel faible, pas de calcul sur des années). */
    private static final int MAX_DAYS = 366;
    private static final int MAX_ROWS = 50_000;

    @PersistenceContext
    private EntityManager em;

    public RevenueSummaryResponse summary(LocalDate from, LocalDate to) {
        validate(from, to);
        Instant start = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        Map<LocalDate, BigDecimal[]> byDay = new TreeMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            byDay.put(d, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
        }

        accumulate(byDay, 0, rows("SELECT so.commissionAmount, d.deliveredAt FROM Delivery d JOIN d.subOrder so "
                + "WHERE so.status = :st AND d.deliveredAt >= :s AND d.deliveredAt < :e", start, end, SubOrderStatus.DELIVERED));
        accumulate(byDay, 1, rows("SELECT e.commissionAmount, e.createdAt FROM CourierEarning e "
                + "WHERE e.createdAt >= :s AND e.createdAt < :e", start, end, null));
        accumulate(byDay, 2, rows("SELECT f.amount, f.createdAt FROM SellerFeeDeduction f "
                + "WHERE f.createdAt >= :s AND f.createdAt < :e", start, end, null));

        BigDecimal commissions = BigDecimal.ZERO, delivery = BigDecimal.ZERO, fees = BigDecimal.ZERO;
        List<RevenueSummaryResponse.DailyRevenue> daily = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal[]> e : byDay.entrySet()) {
            BigDecimal[] v = e.getValue();
            commissions = commissions.add(v[0]);
            delivery = delivery.add(v[1]);
            fees = fees.add(v[2]);
            daily.add(new RevenueSummaryResponse.DailyRevenue(e.getKey(), v[0], v[1], v[2]));
        }
        // Parts versées aux partenaires revendeurs (commissions créditées sur la période) : un coût, déduit du total.
        BigDecimal partnerShares = BigDecimal.ZERO;
        for (Object[] r : rows("SELECT c.partnerShare, c.creditedAt FROM ResellerCommission c "
                + "WHERE c.status = com.maplenou.backend.reseller.ResellerCommissionStatus.CREDITED "
                + "AND c.creditedAt >= :s AND c.creditedAt < :e", start, end, null)) {
            partnerShares = partnerShares.add(BigDecimal.valueOf(((Number) r[0]).longValue()));
        }
        return new RevenueSummaryResponse(from, to, commissions, delivery, fees,
                commissions.add(delivery).add(fees).subtract(partnerShares), partnerShares, daily);
    }

    /** Lignes du registre pour une source, pagination classique (plus récentes d'abord). */
    public Page<RevenueEntryResponse> entries(RevenueSource source, LocalDate from, LocalDate to, Pageable pageable) {
        validate(from, to);
        Instant start = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        String select;
        String count;
        switch (source) {
            case COMMISSION -> {
                select = "SELECT so.id, o.orderNumber, sh.name, so.commissionAmount, d.deliveredAt FROM Delivery d "
                        + "JOIN d.subOrder so JOIN so.order o JOIN so.shop sh "
                        + "WHERE so.status = :st AND d.deliveredAt >= :s AND d.deliveredAt < :e ORDER BY d.deliveredAt DESC";
                count = "SELECT COUNT(d) FROM Delivery d JOIN d.subOrder so "
                        + "WHERE so.status = :st AND d.deliveredAt >= :s AND d.deliveredAt < :e";
            }
            case DELIVERY_SHARE -> {
                select = "SELECT e.id, o.orderNumber, a.fullName, e.commissionAmount, e.createdAt FROM CourierEarning e "
                        + "JOIN e.delivery d JOIN d.subOrder so JOIN so.order o JOIN e.agent a "
                        + "WHERE e.createdAt >= :s AND e.createdAt < :e ORDER BY e.createdAt DESC";
                count = "SELECT COUNT(e) FROM CourierEarning e WHERE e.createdAt >= :s AND e.createdAt < :e";
            }
            default -> {
                select = "SELECT f.id, m.billingMonth, sh.name, f.amount, f.createdAt FROM SellerFeeDeduction f "
                        + "JOIN f.monthlyFee m JOIN m.shop sh WHERE f.createdAt >= :s AND f.createdAt < :e ORDER BY f.createdAt DESC";
                count = "SELECT COUNT(f) FROM SellerFeeDeduction f WHERE f.createdAt >= :s AND f.createdAt < :e";
            }
        }

        var q = em.createQuery(select, Object[].class).setParameter("s", start).setParameter("e", end);
        var c = em.createQuery(count, Long.class).setParameter("s", start).setParameter("e", end);
        if (source == RevenueSource.COMMISSION) {
            q.setParameter("st", SubOrderStatus.DELIVERED);
            c.setParameter("st", SubOrderStatus.DELIVERED);
        }
        List<Object[]> rows = q.setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize()).getResultList();

        List<RevenueEntryResponse> content = rows.stream().map(r -> new RevenueEntryResponse(
                source,
                (UUID) r[0],
                String.valueOf(r[1]),
                String.valueOf(r[2]),
                (BigDecimal) r[3],
                (Instant) r[4])).toList();
        return new PageImpl<>(content, pageable, c.getSingleResult());
    }

    // ----- Privé -----

    private void validate(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Période invalide : la date de fin doit suivre la date de début");
        }
        if (from.plusDays(MAX_DAYS).isBefore(to)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Période trop longue (maximum " + MAX_DAYS + " jours)");
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> rows(String jpql, Instant start, Instant end, SubOrderStatus status) {
        var query = em.createQuery(jpql).setParameter("s", start).setParameter("e", end);
        if (status != null) query.setParameter("st", status);
        return query.setMaxResults(MAX_ROWS).getResultList();
    }

    private void accumulate(Map<LocalDate, BigDecimal[]> byDay, int index, List<Object[]> rows) {
        for (Object[] r : rows) {
            LocalDate day = ((Instant) r[1]).atZone(ZoneOffset.UTC).toLocalDate();
            BigDecimal[] bucket = byDay.get(day);
            if (bucket != null) bucket[index] = bucket[index].add((BigDecimal) r[0]);
        }
    }
}
