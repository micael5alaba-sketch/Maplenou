import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Préférence de langue locale à l'appareil (voir `CLAUDE.md`/
/// `DEMANDES_MODIFICATIONS_BACKEND.md` §9 : "à revoir seulement si on veut
/// la synchroniser entre appareils" — pas de backend pour ça).
///
/// `flutter_localizations`/ARB (`lib/l10n/`) sont en place et `english` est
/// réellement traduit — mais seulement pour une partie de l'app (navigation,
/// connexion/inscription) pour l'instant, pas encore pour chaque écran.
/// `ewe` reste proposé mais non traduit, pour que le réglage soit honnête
/// sur ce qui existe, pas pour simuler une traduction qui n'existe pas.
enum AppLanguage { french, english, ewe }

extension AppLanguageLabel on AppLanguage {
  String get label {
    switch (this) {
      case AppLanguage.french:
        return 'Français';
      case AppLanguage.english:
        return 'English';
      case AppLanguage.ewe:
        return 'Ewé';
    }
  }

  bool get isAvailable => this == AppLanguage.french || this == AppLanguage.english;

  Locale get locale {
    switch (this) {
      case AppLanguage.french:
        return const Locale('fr');
      case AppLanguage.english:
        return const Locale('en');
      case AppLanguage.ewe:
        return const Locale('fr');
    }
  }
}

final localeController = LocaleController();

class LocaleController extends ValueNotifier<AppLanguage> {
  static const _languageKey = 'app_language';

  LocaleController() : super(AppLanguage.french);

  Future<void> load() async {
    final prefs = await SharedPreferences.getInstance();
    final stored = prefs.getString(_languageKey);
    value = AppLanguage.values.firstWhere(
      (lang) => lang.name == stored,
      orElse: () => AppLanguage.french,
    );
  }

  Future<void> setLanguage(AppLanguage language) async {
    value = language;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_languageKey, language.name);
  }
}
