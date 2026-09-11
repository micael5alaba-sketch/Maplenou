import 'package:flutter/material.dart';

import '../models/product_model.dart';
import 'product_card.dart';

/// Horizontal "Vous pourriez aussi aimer" strip, reusing the same
/// [ProductCard] as the rest of the catalog for visual consistency.
class RelatedProductsCarousel extends StatelessWidget {
  final List<ProductModel> products;
  final void Function(ProductModel product) onProductTap;

  const RelatedProductsCarousel({
    super.key,
    required this.products,
    required this.onProductTap,
  });

  @override
  Widget build(BuildContext context) {
    if (products.isEmpty) return const SizedBox.shrink();

    return SizedBox(
      height: 240,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: products.length,
        separatorBuilder: (context, index) => const SizedBox(width: 14),
        itemBuilder: (context, index) {
          final product = products[index];
          return SizedBox(
            width: 160,
            child: ProductCard(
              product: product,
              onTap: () => onProductTap(product),
            ),
          );
        },
      ),
    );
  }
}
