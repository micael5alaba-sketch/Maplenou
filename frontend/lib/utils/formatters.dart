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
