import '../models/courier_profile_model.dart';

/// Provides the identity/stats shown on [CourierProfileScreen]. Mocked for
/// now — no network call. `User` has no `role = DELIVERY_AGENT`-specific
/// profile fields yet (level label, rating, balance) beyond the base
/// account, so this stays fully mocked even once auth is wired up until
/// that's designed.
class CourierProfileService {
  CourierProfileModel getProfile() {
    return const CourierProfileModel(
      fullName: 'Komi Assou',
      levelLabel: 'Livreur Premium',
      isVerified: true,
      rating: 4.9,
      deliveriesCount: 128,
      yearsExperience: 2,
      availableBalance: 45500,
    );
  }
}
