/// Pays choisi par l'acheteur pour le paiement Mobile Money à l'étape
/// checkout. Transmis au backend (`PlaceOrderRequest.paymentCountry`) qui le
/// relaie à FedaPay pour présélectionner le bon pays sur sa page hébergée —
/// sans ça, FedaPay retombe sur son défaut (Côte d'Ivoire) quel que soit le
/// numéro de l'acheteur, ce qui est le bug rapporté ("c'est toujours le
/// numéro de la Côte d'Ivoire qui vient").
enum PaymentCountry { togo, benin, coteDIvoire }

extension PaymentCountryX on PaymentCountry {
  String get isoCode => switch (this) {
        PaymentCountry.togo => 'tg',
        PaymentCountry.benin => 'bj',
        PaymentCountry.coteDIvoire => 'ci',
      };

  String get label => switch (this) {
        PaymentCountry.togo => 'Togo',
        PaymentCountry.benin => 'Bénin',
        PaymentCountry.coteDIvoire => "Côte d'Ivoire",
      };

  String get flag => switch (this) {
        PaymentCountry.togo => '🇹🇬',
        PaymentCountry.benin => '🇧🇯',
        PaymentCountry.coteDIvoire => '🇨🇮',
      };
}
