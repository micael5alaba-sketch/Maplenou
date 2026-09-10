/// Formats a whole-number amount as Franc CFA, e.g. `25000` -> `"25 000 FCFA"`.
String formatFcfa(num amount) {
  final digits = amount.round().toString();
  final buffer = StringBuffer();

  for (var i = 0; i < digits.length; i++) {
    final remainingDigits = digits.length - i;
    if (i != 0 && remainingDigits % 3 == 0) {
      buffer.write(' ');
    }
    buffer.write(digits[i]);
  }

  return '${buffer.toString()} FCFA';
}

/// Formats a past date as a short relative label, e.g. "il y a 6 j".
String formatRelativeDate(DateTime date) {
  final diff = DateTime.now().difference(date);
  if (diff.inDays >= 30) {
    final months = (diff.inDays / 30).floor();
    return 'il y a $months mois';
  }
  if (diff.inDays >= 1) return 'il y a ${diff.inDays} j';
  if (diff.inHours >= 1) return 'il y a ${diff.inHours} h';
  return "à l'instant";
}
