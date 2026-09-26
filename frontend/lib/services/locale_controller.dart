import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Préférence de langue locale à l'appareil (voir `CLAUDE.md`/
/// `DEMANDES_MODIFICATIONS_BACKEND.md` §9 : "à revoir seulement si on veut
/// la synchroniser entre appareils" — pas de backend pour ça).
///
/// L'app entière n'existe qu'en français aujourd'hui (aucune infrastructure
/// `flutter_localizations`/ARB) : ce contrôleur retient un choix réel et le
/// fait survivre à un redémarrage, mais seul `french` change effectivement
/// quelque chose à l'affichage. Les autres valeurs sont proposées pour que
/// le réglage soit honnête sur ce qui existe, pas pour simuler une
/// traduction qui n'existe pas.
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

  bool get isAvailable => this == AppLanguage.french;
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
