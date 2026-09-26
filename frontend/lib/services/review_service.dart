import 'dart:convert';

import 'package:http/http.dart' as http;

import '../config/api_config.dart';
import '../models/my_review_model.dart';
import '../models/review_model.dart';
import 'api_client.dart';

/// `review/ReviewController.java` — lecture publique des avis produit/
/// boutique (pas besoin d'auth pour lister, voir `SecurityConfig`), plus
/// "mes avis" (authentifié).
class ReviewService {
  final _client = ApiClient();

  /// `GET /api/users/me/reviews` — mes avis produits et boutiques confondus.
  Future<List<MyReviewModel>> getMyReviews({int size = 20}) async {
    final data = await _client.get('/api/users/me/reviews', query: {'size': '$size'}) as Map<String, dynamic>;
    final content = data['content'] as List<dynamic>? ?? [];
    return content.map((json) => MyReviewModel.fromJson(json as Map<String, dynamic>)).toList();
  }

  Future<List<ReviewModel>> getProductReviews(String productId, {int size = 20}) async {
    final uri = Uri.parse('${ApiConfig.baseUrl}/api/products/$productId/reviews')
        .replace(queryParameters: {'size': '$size'});
    final response = await http.get(uri).timeout(
      const Duration(seconds: 8),
      onTimeout: () => throw Exception('Le serveur ne répond pas (délai dépassé)'),
    );
    if (response.statusCode != 200) {
      throw Exception('Échec du chargement des avis (${response.statusCode})');
    }
    final page = jsonDecode(response.body) as Map<String, dynamic>;
    final content = page['content'] as List<dynamic>? ?? [];
    return content.map((json) => ReviewModel.fromJson(json as Map<String, dynamic>)).toList();
  }

  /// `GET /api/shops/{shopId}/reviews/summary` — note moyenne d'une boutique.
  Future<double> getShopAverageRating(String shopId) async {
    final uri = Uri.parse('${ApiConfig.baseUrl}/api/shops/$shopId/reviews/summary');
    final response = await http.get(uri).timeout(
      const Duration(seconds: 8),
      onTimeout: () => throw Exception('Le serveur ne répond pas (délai dépassé)'),
    );
    if (response.statusCode != 200) {
      throw Exception('Échec du chargement de la note (${response.statusCode})');
    }
    final data = jsonDecode(response.body) as Map<String, dynamic>;
    return (data['averageRating'] as num).toDouble();
  }
}
