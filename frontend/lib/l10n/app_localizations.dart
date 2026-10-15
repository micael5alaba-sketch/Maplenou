import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_en.dart';
import 'app_localizations_fr.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of AppLocalizations
/// returned by `AppLocalizations.of(context)`.
///
/// Applications need to include `AppLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'l10n/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: AppLocalizations.localizationsDelegates,
///   supportedLocales: AppLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the AppLocalizations.supportedLocales
/// property.
abstract class AppLocalizations {
  AppLocalizations(String locale)
    : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static AppLocalizations? of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations);
  }

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
        delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
      ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[
    Locale('en'),
    Locale('fr'),
  ];

  /// Buyer bottom nav: home tab label
  ///
  /// In fr, this message translates to:
  /// **'Home'**
  String get navHome;

  /// Buyer bottom nav: categories tab label
  ///
  /// In fr, this message translates to:
  /// **'Catégories'**
  String get navCategories;

  /// Buyer bottom nav: cart tab label
  ///
  /// In fr, this message translates to:
  /// **'Panier'**
  String get navCart;

  /// Bottom nav: profile tab label (shared buyer/seller/courier)
  ///
  /// In fr, this message translates to:
  /// **'Profil'**
  String get navProfile;

  /// Seller/courier bottom nav: dashboard tab label
  ///
  /// In fr, this message translates to:
  /// **'Dashboard'**
  String get navDashboard;

  /// Courier bottom nav: deliveries tab label
  ///
  /// In fr, this message translates to:
  /// **'Courses'**
  String get navDeliveries;

  /// Courier bottom nav: messages tab label
  ///
  /// In fr, this message translates to:
  /// **'Messages'**
  String get navMessages;

  /// Seller bottom nav: orders tab label
  ///
  /// In fr, this message translates to:
  /// **'Commandes'**
  String get navOrders;

  /// Seller bottom nav: products tab label
  ///
  /// In fr, this message translates to:
  /// **'Produits'**
  String get navProducts;

  /// App drawer: my orders menu item
  ///
  /// In fr, this message translates to:
  /// **'Mes commandes'**
  String get drawerMyOrders;

  /// App drawer: settings menu item
  ///
  /// In fr, this message translates to:
  /// **'Paramètres'**
  String get drawerSettings;

  /// App drawer: logout menu item
  ///
  /// In fr, this message translates to:
  /// **'Déconnexion'**
  String get drawerLogout;

  /// Login screen title
  ///
  /// In fr, this message translates to:
  /// **'Connexion'**
  String get loginTitle;

  /// Login screen title when 2FA challenge is shown
  ///
  /// In fr, this message translates to:
  /// **'Vérification en deux étapes'**
  String get loginMfaTitle;

  /// Login screen subtitle
  ///
  /// In fr, this message translates to:
  /// **'Connectez-vous avec votre numéro de téléphone.'**
  String get loginSubtitle;

  /// Login screen subtitle during 2FA challenge
  ///
  /// In fr, this message translates to:
  /// **'Entrez le code à 6 chiffres de votre application d\'authentification.'**
  String get loginMfaSubtitle;

  /// Phone number field hint, shared login/register
  ///
  /// In fr, this message translates to:
  /// **'Numéro de téléphone'**
  String get phoneNumberHint;

  /// Login: phone number required validation message
  ///
  /// In fr, this message translates to:
  /// **'Numéro de téléphone requis.'**
  String get phoneNumberRequired;

  /// Password field hint, shared login/register
  ///
  /// In fr, this message translates to:
  /// **'Mot de passe'**
  String get passwordHint;

  /// Login: password required validation message
  ///
  /// In fr, this message translates to:
  /// **'Mot de passe requis.'**
  String get passwordRequired;

  /// 2FA TOTP code field hint
  ///
  /// In fr, this message translates to:
  /// **'Code à 6 chiffres'**
  String get totpCodeHint;

  /// Login submit button label
  ///
  /// In fr, this message translates to:
  /// **'Se connecter'**
  String get loginButton;

  /// 2FA verify submit button label
  ///
  /// In fr, this message translates to:
  /// **'Vérifier'**
  String get verifyButton;

  /// Login screen: prompt before the sign-up link
  ///
  /// In fr, this message translates to:
  /// **'Pas encore de compte ? '**
  String get noAccountYet;

  /// Login screen: sign-up link text
  ///
  /// In fr, this message translates to:
  /// **'S\'inscrire'**
  String get signUpLink;

  /// Login screen: forgot password link text
  ///
  /// In fr, this message translates to:
  /// **'Mot de passe oublié ?'**
  String get forgotPasswordLink;

  /// Forgot password screen title
  ///
  /// In fr, this message translates to:
  /// **'Mot de passe oublié'**
  String get forgotPasswordTitle;

  /// Forgot password screen subtitle
  ///
  /// In fr, this message translates to:
  /// **'Entrez votre numéro de téléphone, nous vous enverrons un code par email pour réinitialiser votre mot de passe.'**
  String get forgotPasswordSubtitle;

  /// Forgot password screen: send code button
  ///
  /// In fr, this message translates to:
  /// **'Envoyer le code'**
  String get forgotPasswordSendCodeButton;

  /// Link back to the login screen
  ///
  /// In fr, this message translates to:
  /// **'Retour à la connexion'**
  String get backToLoginLink;

  /// Reset password screen title
  ///
  /// In fr, this message translates to:
  /// **'Réinitialiser le mot de passe'**
  String get resetPasswordTitle;

  /// Reset password screen subtitle
  ///
  /// In fr, this message translates to:
  /// **'Entrez le code à 6 chiffres reçu par email et votre nouveau mot de passe.'**
  String get resetPasswordSubtitle;

  /// Email OTP code field hint
  ///
  /// In fr, this message translates to:
  /// **'Code à 6 chiffres'**
  String get otpCodeHint;

  /// Reset password: OTP code required validation message
  ///
  /// In fr, this message translates to:
  /// **'Le code est requis.'**
  String get otpCodeRequired;

  /// New password field hint
  ///
  /// In fr, this message translates to:
  /// **'Nouveau mot de passe'**
  String get newPasswordHint;

  /// New password field helper text
  ///
  /// In fr, this message translates to:
  /// **'Au moins 8 caractères, une majuscule et un chiffre.'**
  String get newPasswordRequirements;

  /// Reset password screen submit button
  ///
  /// In fr, this message translates to:
  /// **'Réinitialiser le mot de passe'**
  String get resetPasswordButton;

  /// Reset password success message
  ///
  /// In fr, this message translates to:
  /// **'Mot de passe réinitialisé !'**
  String get resetPasswordSuccessTitle;

  /// Reset password success subtitle
  ///
  /// In fr, this message translates to:
  /// **'Vous pouvez maintenant vous connecter avec votre nouveau mot de passe.'**
  String get resetPasswordSuccessSubtitle;

  /// Register screen title
  ///
  /// In fr, this message translates to:
  /// **'Créer un compte'**
  String get registerTitle;

  /// Register screen subtitle
  ///
  /// In fr, this message translates to:
  /// **'Rejoignez Maplenou en quelques secondes.'**
  String get registerSubtitle;

  /// Full name field hint
  ///
  /// In fr, this message translates to:
  /// **'Nom complet'**
  String get fullNameHint;

  /// Register: full name required validation message
  ///
  /// In fr, this message translates to:
  /// **'Nom complet requis.'**
  String get fullNameRequired;

  /// Register: phone number invalid validation message
  ///
  /// In fr, this message translates to:
  /// **'Numéro de téléphone invalide.'**
  String get phoneNumberInvalid;

  /// Register: password strength rule / validation message
  ///
  /// In fr, this message translates to:
  /// **'Au moins 8 caractères, une majuscule et un chiffre.'**
  String get passwordRule;

  /// Register submit button label
  ///
  /// In fr, this message translates to:
  /// **'S\'inscrire'**
  String get registerButton;
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture<AppLocalizations>(lookupAppLocalizations(locale));
  }

  @override
  bool isSupported(Locale locale) =>
      <String>['en', 'fr'].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'en':
      return AppLocalizationsEn();
    case 'fr':
      return AppLocalizationsFr();
  }

  throw FlutterError(
    'AppLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
    'an issue with the localizations generation tool. Please file an issue '
    'on GitHub with a reproducible sample app and the gen-l10n configuration '
    'that was used.',
  );
}
