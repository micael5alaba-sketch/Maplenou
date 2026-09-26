import 'cart_item_model.dart';

/// Miroir de `CartResponse` (`cart/dto/CartResponse.java`).
class CartModel {
  final List<CartItemModel> items;
  final num totalAmount;

  const CartModel({required this.items, required this.totalAmount});

  int get itemCount => items.fold(0, (sum, item) => sum + item.quantity);

  static const empty = CartModel(items: [], totalAmount: 0);

  factory CartModel.fromJson(Map<String, dynamic> json) {
    final items = (json['items'] as List<dynamic>? ?? [])
        .map((item) => CartItemModel.fromJson(item as Map<String, dynamic>))
        .toList();
    return CartModel(items: items, totalAmount: json['totalAmount'] as num? ?? 0);
  }
}
