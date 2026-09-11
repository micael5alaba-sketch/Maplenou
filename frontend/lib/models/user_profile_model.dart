/// The current user's profile, as shown on [ProfileScreen].
class UserProfileModel {
  final String fullName;
  final String email;
  final String? avatarUrl;
  final bool isVerified;
  final int ongoingOrdersCount;
  final String primaryAddressSummary;
  final String paymentMethodsSummary;

  const UserProfileModel({
    required this.fullName,
    required this.email,
    this.avatarUrl,
    this.isVerified = false,
    this.ongoingOrdersCount = 0,
    required this.primaryAddressSummary,
    required this.paymentMethodsSummary,
  });
}
