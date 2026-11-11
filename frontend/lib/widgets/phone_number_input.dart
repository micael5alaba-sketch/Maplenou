import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../l10n/app_localizations.dart';
import '../theme/app_color_scheme.dart';

/// Indicatif + longueur exacte attendue pour les deux marchés cibles (Togo : +228, 8 chiffres ;
/// Bénin : +229, 10 chiffres, préfixe "01" inclus depuis le 30/11/2024).
class PhoneCountry {
  final String flag;
  final String dialCode;
  final String name;
  final int digitCount;

  const PhoneCountry({required this.flag, required this.dialCode, required this.name, required this.digitCount});
}

const List<PhoneCountry> phoneCountries = [
  PhoneCountry(flag: '🇹🇬', dialCode: '+228', name: 'Togo', digitCount: 8),
  PhoneCountry(flag: '🇧🇯', dialCode: '+229', name: 'Bénin', digitCount: 10),
];

/// Numéro complet tel qu'enregistré à l'inscription : indicatif du pays choisi + chiffres saisis.
String fullPhoneNumber(PhoneCountry country, String digits) =>
    country.dialCode + digits.replaceAll(RegExp(r'[^0-9]'), '');

/// Choix du pays (liste déroulante) + saisie des chiffres, identique à l'inscription : la
/// connexion doit envoyer le même numéro complet que celui enregistré (indicatif + chiffres), sans
/// que l'utilisateur ait à retaper l'indicatif lui-même.
class PhoneNumberInput extends StatelessWidget {
  final TextEditingController controller;
  final PhoneCountry country;
  final ValueChanged<PhoneCountry> onCountryChanged;

  const PhoneNumberInput({
    super.key,
    required this.controller,
    required this.country,
    required this.onCountryChanged,
  });

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final l10n = AppLocalizations.of(context)!;
    final border = OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: BorderSide(color: colors.border),
    );

    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          height: 52,
          padding: const EdgeInsets.symmetric(horizontal: 10),
          decoration: BoxDecoration(
            color: colors.inputFill,
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: colors.border),
          ),
          child: DropdownButtonHideUnderline(
            child: DropdownButton<PhoneCountry>(
              value: country,
              items: phoneCountries
                  .map((c) => DropdownMenuItem(
                        value: c,
                        child: Text('${c.flag} ${c.dialCode}', style: TextStyle(color: colors.textDark)),
                      ))
                  .toList(),
              onChanged: (selected) {
                if (selected != null) onCountryChanged(selected);
              },
            ),
          ),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: TextFormField(
            controller: controller,
            keyboardType: TextInputType.phone,
            inputFormatters: [
              FilteringTextInputFormatter.digitsOnly,
              LengthLimitingTextInputFormatter(country.digitCount),
            ],
            decoration: InputDecoration(
              hintText: l10n.phoneNumberHint,
              prefixIcon: Icon(Icons.phone_outlined, color: colors.textMuted),
              hintStyle: TextStyle(color: colors.textMuted, fontSize: 14),
              filled: true,
              fillColor: colors.inputFill,
              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
              border: border,
              enabledBorder: border,
              focusedBorder: border.copyWith(borderSide: BorderSide(color: colors.primary, width: 1.6)),
              errorBorder: border.copyWith(borderSide: BorderSide(color: colors.error)),
              focusedErrorBorder: border.copyWith(borderSide: BorderSide(color: colors.error, width: 1.6)),
            ),
            validator: (value) {
              final digits = (value ?? '').replaceAll(RegExp(r'[^0-9]'), '');
              if (digits.length != country.digitCount) {
                return l10n.phoneNumberDigitsRequired(country.name, country.digitCount);
              }
              return null;
            },
          ),
        ),
      ],
    );
  }
}
