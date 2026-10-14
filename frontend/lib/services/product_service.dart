import 'dart:convert';

import 'package:http/http.dart' as http;

import '../config/api_config.dart';
import '../models/product_model.dart';

/// One page of catalog results plus the cursor to fetch the next one —
/// mirrors the backend's `CursorPage<ProductSummaryResponse>`.
class CatalogPage {
  final List<ProductModel> products;
  final String? nextCursor;
  final bool hasNext;

  const CatalogPage({required this.products, required this.nextCursor, required this.hasNext});
}

/// Fetches products from the Spring Boot backend's public catalog endpoint
/// (`GET /api/products`, keyset-paginated — see `CursorPage` on the
/// backend). Supports the real filters the backend exposes: category, price
/// range and full-text search (`ProductController.listCatalog`).
class ProductService {
  /// The backend has no "popular" ranking yet — this just pulls the first
  /// page of the catalog. Swap for a dedicated endpoint once one exists.
  Future<List<ProductModel>> getPopularProducts({int size = 8}) async {
    return (await _fetchCatalog(size: size)).products;
  }

  /// Catalog results, optionally filtered by category/prix/recherche.
  Future<List<ProductModel>> getCategoryResults({
    String? categoryId,
    num? minPrice,
    num? maxPrice,
    String? search,
    int size = 20,
  }) async {
    return (await _fetchCatalog(
      categoryId: categoryId,
      minPrice: minPrice,
      maxPrice: maxPrice,
      search: search,
      size: size,
    )).products;
  }

  /// Fiche complète d'un produit (`GET /api/products/{slug}`) : variantes
  /// réelles, images, description, spécifications — voir
  /// `ProductDetailResponse` côté backend. Renvoyé en JSON brut ; c'est
  /// [ProductDetailService] qui l'assemble avec le reste (avis, livraison).
  Future<Map<String, dynamic>> getProductDetail(String slug) async {
    final uri = Uri.parse('${ApiConfig.baseUrl}/api/products/$slug');
    final response = await http.get(uri).timeout(
      const Duration(seconds: 8),
      onTimeout: () => throw Exception('Le serveur ne répond pas (délai dépassé)'),
    );

    if (response.statusCode != 200) {
      throw Exception('Échec du chargement du produit (${response.statusCode})');
    }
    return jsonDecode(response.body) as Map<String, dynamic>;
  }

  Future<CatalogPage> _fetchCatalog({
    String? categoryId,
    num? minPrice,
    num? maxPrice,
    String? search,
    String? cursor,
    int size = 20,
  }) async {
    final query = <String, String>{'size': '$size'};
    if (categoryId != null) query['categoryId'] = categoryId;
    if (minPrice != null) query['minPrice'] = '$minPrice';
    if (maxPrice != null) query['maxPrice'] = '$maxPrice';
    if (search != null && search.trim().isNotEmpty) query['search'] = search.trim();
    if (cursor != null) query['cursor'] = cursor;

    final uri = Uri.parse('${ApiConfig.baseUrl}/api/products').replace(queryParameters: query);
    final response = await http.get(uri).timeout(
      const Duration(seconds: 8),
      onTimeout: () => throw Exception('Le serveur ne répond pas (délai dépassé)'),
    );

    if (response.statusCode != 200) {
      throw Exception('Échec du chargement des produits (${response.statusCode})');
    }

    final page = jsonDecode(response.body) as Map<String, dynamic>;
    final content = page['content'] as List<dynamic>? ?? [];
    return CatalogPage(
      products: content.map((json) => ProductModel.fromSummaryJson(json as Map<String, dynamic>)).toList(),
      nextCursor: page['nextCursor'] as String?,
      hasNext: page['hasNext'] as bool? ?? false,
    );
  }
}
