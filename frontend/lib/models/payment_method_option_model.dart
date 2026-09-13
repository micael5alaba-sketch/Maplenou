/// A payment method offered at checkout — purely informational for now:
/// no payment integration exists on the backend (FedaPay/Stripe explicitly
/// deferred, see `CLAUDE.md` §7). Selecting one and "paying" only
/// simulates success.
enum PaymentMethodType { tMoney, flooz, stripe }

class PaymentMethodOptionModel {
  final PaymentMethodType type;
  final String title;
  final String subtitle;
  final String badgeLabel;

  const PaymentMethodOptionModel({
    required this.type,
    required this.title,
    required this.subtitle,
    required this.badgeLabel,
  });
}

const List<PaymentMethodOptionModel> kCheckoutPaymentMethods = [
  PaymentMethodOptionModel(type: PaymentMethodType.tMoney, title: 'Mixx by Yas (TMoney)', subtitle: 'Paiement mobile sécurisé', badgeLabel: 'TMoney'),
  PaymentMethodOptionModel(type: PaymentMethodType.flooz, title: 'Flooz (Moov Money)', subtitle: 'Paiement mobile sécurisé', badgeLabel: 'Moov'),
  PaymentMethodOptionModel(type: PaymentMethodType.stripe, title: 'Stripe (Carte Bancaire)', subtitle: 'Visa, Mastercard', badgeLabel: ''),
];
