package com.maplenou.backend.payment;

import com.maplenou.backend.order.Order;
import com.maplenou.backend.payment.dto.CreateTransactionRequest;
import com.maplenou.backend.payment.dto.FedaPayCurrency;
import com.maplenou.backend.payment.dto.FedaPayCustomer;
import com.maplenou.backend.payment.dto.FedaPayPhoneNumber;
import com.maplenou.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Démarre le paiement d'une commande côté FedaPay : crée la transaction, génère le lien de
 * paiement hébergé, renvoie l'URL à ouvrir côté client (voir `OrderService.placeOrder`).
 */
@Service
@RequiredArgsConstructor
public class FedaPayCheckoutService {

    private final FedaPayClient fedaPayClient;

    @Value("${app.fedapay.callback-url}")
    private String callbackUrl;

    /**
     * @param paymentCountry pays choisi par l'acheteur à l'étape paiement (ex: "tg", "bj", "ci"),
     *                       ou null pour retomber sur la détection par indicatif téléphonique.
     * @return l'id de transaction FedaPay (à stocker sur l'Order) et l'URL de paiement.
     */
    public FedaPayCheckoutResult startCheckout(Order order, String paymentCountry) {
        User buyer = order.getBuyer();

        CreateTransactionRequest request = new CreateTransactionRequest(
                "Commande " + order.getOrderNumber() + " — Maplenou",
                order.getTotalAmount().intValue(), // FCFA = devise sans décimales, pas de centimes
                new FedaPayCurrency("XOF"),
                new FedaPayCustomer(
                        firstNameOf(buyer.getFullName()),
                        lastNameOf(buyer.getFullName()),
                        buyer.getEmail(),
                        toFedaPayPhone(buyer.getPhoneNumber(), paymentCountry)
                ),
                callbackUrl
        );

        FedaPayClient.CreatedTransaction created = fedaPayClient.createTransaction(request);
        String paymentUrl = created.paymentUrl() != null
                ? created.paymentUrl()
                : fedaPayClient.generatePaymentUrl(created.id());
        return new FedaPayCheckoutResult(created.id(), paymentUrl);
    }

    public record FedaPayCheckoutResult(long transactionId, String paymentUrl) {
    }

    private String firstNameOf(String fullName) {
        int space = fullName.indexOf(' ');
        return space > 0 ? fullName.substring(0, space) : fullName;
    }

    private String lastNameOf(String fullName) {
        int space = fullName.indexOf(' ');
        return space > 0 ? fullName.substring(space + 1) : fullName;
    }

    private static final java.util.Set<String> SUPPORTED_PAYMENT_COUNTRIES = java.util.Set.of("tg", "bj", "ci");

    /**
     * Le numéro est stocké tel que saisi à l'inscription (avec ou sans indicatif +228/+229,
     * voir `RegisterRequest`) : on normalise vers la forme attendue par FedaPay
     * ({@code {number, country}}). Si l'acheteur a explicitement choisi un pays de paiement à
     * l'étape checkout (sélecteur pays/opérateur côté app), ce choix prime — c'est ce qui évite
     * que la page FedaPay hébergée retombe sur son défaut (Côte d'Ivoire) au lieu du Togo/Bénin.
     * Sans choix explicite, on infère depuis l'indicatif, Togo par défaut faute d'indicatif.
     */
    private FedaPayPhoneNumber toFedaPayPhone(String rawPhoneNumber, String paymentCountry) {
        String digits = rawPhoneNumber.replaceAll("[^0-9]", "");
        if (paymentCountry != null && SUPPORTED_PAYMENT_COUNTRIES.contains(paymentCountry.toLowerCase())) {
            String country = paymentCountry.toLowerCase();
            return new FedaPayPhoneNumber(digits.replaceFirst("^(228|229|225)", ""), country);
        }
        if (rawPhoneNumber.startsWith("+229") || digits.startsWith("229")) {
            return new FedaPayPhoneNumber(digits.replaceFirst("^229", ""), "bj");
        }
        if (rawPhoneNumber.startsWith("+228") || digits.startsWith("228")) {
            return new FedaPayPhoneNumber(digits.replaceFirst("^228", ""), "tg");
        }
        return new FedaPayPhoneNumber(digits, "tg");
    }
}
