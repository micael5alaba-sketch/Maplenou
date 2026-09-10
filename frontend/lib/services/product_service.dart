import 'dart:convert';

import 'package:http/http.dart' as http;

import '../config/api_config.dart';
import '../models/product_model.dart';

/// Fetches products from the Spring Boot backend's public catalog endpoint
/// (`GET /api/products`, keyset-paginated — see `CursorPage` on the
/// backend).
class ProductService {
  /// The backend has no "popular" ranking yet — this just pulls the first
  /// page of the catalog. Swap for a dedicated endpoint once one exists.
  Future<List<ProductModel>> getPopularProducts({int size = 8}) {
    return _fetchCatalog(size: size);
  }

  /// Catalog results, optionally filtered by category.
  Future<List<ProductModel>> getCategoryResults({String? categoryId, int size = 20}) {
    return _fetchCatalog(categoryId: categoryId, size: size);
  }

  Future<List<ProductModel>> _fetchCatalog({String? categoryId, int size = 20}) async {
    final uri = Uri.parse('${ApiConfig.baseUrl}/api/products').replace(
      queryParameters: {
        'categoryId': ?categoryId,
        'size': '$size',
      },
    );
    final response = await http.get(uri).timeout(
      const Duration(seconds: 8),
      onTimeout: () => throw Exception('Le serveur ne répond pas (délai dépassé)'),
    );

    if (response.statusCode != 200) {
      throw Exception('Échec du chargement des produits (${response.statusCode})');
    }

    final page = jsonDecode(response.body) as Map<String, dynamic>;
    final content = page['content'] as List<dynamic>? ?? [];
    return content
        .map((json) => ProductModel.fromSummaryJson(json as Map<String, dynamic>))
        .toList();
  }
}
