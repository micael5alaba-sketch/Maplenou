/// Shown on [SellerProfileScreen]: shop identity plus editable settings.
class SellerProfileModel {
  final String shopName;
  final String? logoUrl;
  final bool isCertified;
  final String location;
  final int memberSinceYear;
  final String contactEmail;

  /// NINEA/TVA fiscal number — optional, empty until the seller fills it in.
  final String? taxId;

  const SellerProfileModel({
    required this.shopName,
    this.logoUrl,
    required this.isCertified,
    required this.location,
    required this.memberSinceYear,
    required this.contactEmail,
    this.taxId,
  });
}
