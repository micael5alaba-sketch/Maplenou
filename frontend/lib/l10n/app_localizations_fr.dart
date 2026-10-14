// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for French (`fr`).
class AppLocalizationsFr extends AppLocalizations {
  AppLocalizationsFr([String locale = 'fr']) : super(locale);

  @override
  String get navHome => 'Home';

  @override
  String get navCategories => 'Catégories';

  @override
  String get navCart => 'Panier';

  @override
  String get navProfile => 'Profil';

  @override
  String get navDashboard => 'Dashboard';

  @override
  String get navDeliveries => 'Courses';

  @override
  String get navMessages => 'Messages';

  @override
  String get navOrders => 'Commandes';

  @override
  String get navProducts => 'Produits';

  @override
  String get drawerMyOrders => 'Mes commandes';

  @override
  String get drawerSettings => 'Paramètres';

  @override
  String get drawerLogout => 'Déconnexion';

  @override
  String get loginTitle => 'Connexion';

  @override
  String get loginMfaTitle => 'Vérification en deux étapes';

  @override
  String get loginSubtitle => 'Connectez-vous avec votre numéro de téléphone.';

  @override
  String get loginMfaSubtitle =>
      'Entrez le code à 6 chiffres de votre application d\'authentification.';

  @override
  String get phoneNumberHint => 'Numéro de téléphone';

  @override
  String get phoneNumberRequired => 'Numéro de téléphone requis.';

  @override
  String get passwordHint => 'Mot de passe';

  @override
  String get passwordRequired => 'Mot de passe requis.';

  @override
  String get totpCodeHint => 'Code à 6 chiffres';

  @override
  String get loginButton => 'Se connecter';

  @override
  String get verifyButton => 'Vérifier';

  @override
  String get noAccountYet => 'Pas encore de compte ? ';

  @override
  String get signUpLink => 'S\'inscrire';

  @override
  String get registerTitle => 'Créer un compte';

  @override
  String get registerSubtitle => 'Rejoignez Maplenou en quelques secondes.';

  @override
  String get fullNameHint => 'Nom complet';

  @override
  String get fullNameRequired => 'Nom complet requis.';

  @override
  String get phoneNumberInvalid => 'Numéro de téléphone invalide.';

  @override
  String get passwordRule =>
      'Au moins 8 caractères, une majuscule et un chiffre.';

  @override
  String get registerButton => 'S\'inscrire';
}
