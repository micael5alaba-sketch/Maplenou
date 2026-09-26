/// Miroir de `Role` (backend, `user/Role.java`) — rôles internes explicites
/// uniquement. `null` = compte acheteur standard, sans rôle particulier.
enum UserRole { admin, deliveryAgent }

UserRole? _userRoleFromJson(String? value) {
  switch (value) {
    case 'ADMIN':
      return UserRole.admin;
    case 'DELIVERY_AGENT':
      return UserRole.deliveryAgent;
    default:
      return null;
  }
}

/// Miroir de `SellerStatus` (backend, `seller/SellerStatus.java`).
enum SellerAccountStatus { pending, approved, suspended, rejected }

SellerAccountStatus? _sellerStatusFromJson(String? value) {
  switch (value) {
    case 'PENDING':
      return SellerAccountStatus.pending;
    case 'APPROVED':
      return SellerAccountStatus.approved;
    case 'SUSPENDED':
      return SellerAccountStatus.suspended;
    case 'REJECTED':
      return SellerAccountStatus.rejected;
    default:
      return null;
  }
}

/// Compte utilisateur unique (voir `CLAUDE.md` §3) : un seul modèle pour
/// Acheteur/Vendeur/Livreur, les capacités se lisent sur [role] et
/// [sellerStatus], pas sur des comptes séparés.
class UserModel {
  final String id;
  final String fullName;
  final String phoneNumber;
  final String? email;
  final String? avatarUrl;
  final UserRole? role;
  final bool phoneVerified;
  final SellerAccountStatus? sellerStatus;

  const UserModel({
    required this.id,
    required this.fullName,
    required this.phoneNumber,
    this.email,
    this.avatarUrl,
    this.role,
    required this.phoneVerified,
    this.sellerStatus,
  });

  bool get isDeliveryAgent => role == UserRole.deliveryAgent;

  bool get isApprovedSeller => sellerStatus == SellerAccountStatus.approved;

  bool get hasPendingSellerApplication => sellerStatus == SellerAccountStatus.pending;

  factory UserModel.fromJson(Map<String, dynamic> json) {
    return UserModel(
      id: json['id'] as String,
      fullName: json['fullName'] as String,
      phoneNumber: json['phoneNumber'] as String,
      email: json['email'] as String?,
      avatarUrl: json['avatarUrl'] as String?,
      role: _userRoleFromJson(json['role'] as String?),
      phoneVerified: json['phoneVerified'] as bool? ?? false,
      sellerStatus: _sellerStatusFromJson(json['sellerStatus'] as String?),
    );
  }
}
