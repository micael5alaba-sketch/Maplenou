// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get navHome => 'Home';

  @override
  String get navCategories => 'Categories';

  @override
  String get navCart => 'Cart';

  @override
  String get navProfile => 'Profile';

  @override
  String get navDashboard => 'Dashboard';

  @override
  String get navDeliveries => 'Deliveries';

  @override
  String get navMessages => 'Messages';

  @override
  String get navOrders => 'Orders';

  @override
  String get navProducts => 'Products';

  @override
  String get drawerMyOrders => 'My orders';

  @override
  String get drawerSettings => 'Settings';

  @override
  String get drawerLogout => 'Log out';

  @override
  String get loginTitle => 'Login';

  @override
  String get loginMfaTitle => 'Two-step verification';

  @override
  String get loginSubtitle => 'Sign in with your phone number.';

  @override
  String get loginMfaSubtitle =>
      'Enter the 6-digit code from your authenticator app.';

  @override
  String get phoneNumberHint => 'Phone number';

  @override
  String get phoneNumberRequired => 'Phone number required.';

  @override
  String get passwordHint => 'Password';

  @override
  String get passwordRequired => 'Password required.';

  @override
  String get totpCodeHint => '6-digit code';

  @override
  String get loginButton => 'Sign in';

  @override
  String get verifyButton => 'Verify';

  @override
  String get noAccountYet => 'Don\'t have an account yet? ';

  @override
  String get signUpLink => 'Sign up';

  @override
  String get forgotPasswordLink => 'Forgot password?';

  @override
  String get forgotPasswordTitle => 'Forgot password';

  @override
  String get forgotPasswordSubtitle =>
      'Enter your phone number, we\'ll send you a code by email to reset your password.';

  @override
  String get forgotPasswordSendCodeButton => 'Send code';

  @override
  String get backToLoginLink => 'Back to login';

  @override
  String get resetPasswordTitle => 'Reset password';

  @override
  String get resetPasswordSubtitle =>
      'Enter the 6-digit code you received by email and your new password.';

  @override
  String get otpCodeHint => '6-digit code';

  @override
  String get otpCodeRequired => 'Code is required.';

  @override
  String get newPasswordHint => 'New password';

  @override
  String get newPasswordRequirements =>
      'At least 8 characters, one uppercase letter and one digit.';

  @override
  String get resetPasswordButton => 'Reset password';

  @override
  String get resetPasswordSuccessTitle => 'Password reset!';

  @override
  String get resetPasswordSuccessSubtitle =>
      'You can now sign in with your new password.';

  @override
  String get registerTitle => 'Create an account';

  @override
  String get registerSubtitle => 'Join Maplenou in a few seconds.';

  @override
  String get fullNameHint => 'Full name';

  @override
  String get fullNameRequired => 'Full name required.';

  @override
  String get phoneNumberInvalid => 'Invalid phone number.';

  @override
  String get passwordRule =>
      'At least 8 characters, one uppercase letter and one digit.';

  @override
  String get registerButton => 'Sign up';
}
