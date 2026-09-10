import 'package:shared_preferences/shared_preferences.dart';

/// Remembers which profile the user picked, on-device (no network call).
///
/// Used by [SplashScreen] to decide whether a returning user can skip
/// straight to their profile's home screen, or whether this is their
/// first visit and they still need to go through profile selection.
class SessionService {
  static const _selectedRoleIdKey = 'selected_role_id';

  Future<String?> getSelectedRoleId() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(_selectedRoleIdKey);
  }

  Future<void> saveSelectedRoleId(String roleId) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_selectedRoleIdKey, roleId);
  }
}
