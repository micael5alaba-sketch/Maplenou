/// Met un numéro saisi à la main au format stocké par l'inscription (`+228` + 8 chiffres pour le
/// Togo, `+229` + 10 chiffres pour le Bénin). L'écran d'inscription ajoute l'indicatif lui-même
/// (liste déroulante) et enregistre `+22890123456` ; sans cette normalisation, l'écran de
/// connexion envoyait `90123456` tel que tapé, un numéro qui n'existe pas en base : « Identifiants
/// invalides » à la reconnexion, alors que l'inscription (qui connecte directement) avait réussi.
String normalizePhoneNumber(String input) {
  final trimmed = input.trim();
  final cleaned = trimmed.replaceAll(RegExp(r'[\s\-.()]'), '');
  if (cleaned.isEmpty) return cleaned;

  if (cleaned.startsWith('+')) return cleaned;
  if (cleaned.startsWith('00')) return '+${cleaned.substring(2)}';
  if (!RegExp(r'^\d+$').hasMatch(cleaned)) return trimmed;

  if (cleaned.length == 8) return '+228$cleaned';
  if (cleaned.length == 10) return '+229$cleaned';
  if (cleaned.length == 11 && cleaned.startsWith('228')) return '+$cleaned';
  if (cleaned.length == 13 && cleaned.startsWith('229')) return '+$cleaned';
  return cleaned;
}
