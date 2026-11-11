import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/utils/phone_utils.dart';

void main() {
  test('8 chiffres saisis sans indicatif -> +228 (Togo)', () {
    expect(normalizePhoneNumber('90123456'), '+22890123456');
    expect(normalizePhoneNumber(' 90 12 34 56 '), '+22890123456');
  });

  test('numéro déjà au format complet inchangé', () {
    expect(normalizePhoneNumber('+22890123456'), '+22890123456');
    expect(normalizePhoneNumber('+229 0190123456'), '+2290190123456');
  });

  test('10 chiffres -> +229 (Bénin)', () {
    expect(normalizePhoneNumber('0190123456'), '+2290190123456');
  });

  test('indicatif sans plus ou avec 00', () {
    expect(normalizePhoneNumber('22890123456'), '+22890123456');
    expect(normalizePhoneNumber('0022890123456'), '+22890123456');
  });
}
