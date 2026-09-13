/// Handles placing an order from [CheckoutScreen].
///
/// Fully mocked: no payment integration exists on the backend
/// (FedaPay/Stripe explicitly deferred — see `CLAUDE.md` §7), so
/// "confirming payment" only simulates a delay and generates an order
/// number locally. Real order placement would be
/// `POST /api/orders` (already built and working, just not payment-backed
/// yet) once a payment step actually exists in front of it.
class CheckoutService {
  static const num deliveryFee = 9000;

  Future<String> placeOrder() async {
    await Future.delayed(const Duration(seconds: 2));
    final sequence = (DateTime.now().millisecondsSinceEpoch % 9000) + 1000;
    return 'MPL-$sequence';
  }
}
