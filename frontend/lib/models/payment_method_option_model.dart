/// A payment method offered at checkout. FedaPay's hosted page (opened via
/// `OrderModel.paymentUrl`, see `CLAUDE.md` §7) lets the payer pick the
/// actual channel itself — this selector mainly drives which country/method
/// combination looks right to the buyer before redirecting; the country half
/// (see `PaymentCountry`) is what actually reaches the backend today.
enum PaymentMethodType { tMoney, flooz, card }

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
  PaymentMethodOptionModel(type: PaymentMethodType.card, title: 'Carte bancaire', subtitle: 'Visa, Mastercard — via FedaPay', badgeLabel: ''),
];
