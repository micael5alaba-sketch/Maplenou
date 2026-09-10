import '../models/user_profile_model.dart';

/// Provides the current user's profile shown on [ProfileScreen].
///
/// Mocked for now — no network call. Replace [getCurrentProfile] with a
/// real `GET /api/users/me` call to the Spring Boot backend once wired up;
/// the return type stays the same.
class ProfileService {
  UserProfileModel getCurrentProfile() {
    return const UserProfileModel(
      fullName: 'Ama Konan',
      email: 'ama.konan@example.com',
      isVerified: true,
      ongoingOrdersCount: 2,
      primaryAddressSummary: 'Lomé, Quartier Adidogomé',
      paymentMethodsSummary: 'T-Money, Flooz, Carte',
    );
  }
}
