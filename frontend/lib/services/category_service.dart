import 'dart:convert';

import 'package:http/http.dart' as http;

import '../config/api_config.dart';
import '../models/category_model.dart';

/// Fetches root categories from the Spring Boot backend
/// (`GET /api/categories`).
class CategoryService {
  Future<List<CategoryModel>> getCategories() async {
    final uri = Uri.parse('${ApiConfig.baseUrl}/api/categories');
    final response = await http.get(uri).timeout(
      const Duration(seconds: 8),
      onTimeout: () => throw Exception('Le serveur ne répond pas (délai dépassé)'),
    );

    if (response.statusCode != 200) {
      throw Exception('Échec du chargement des catégories (${response.statusCode})');
    }

    final data = jsonDecode(response.body) as List<dynamic>;
    return data
        .map((json) => CategoryModel.fromJson(json as Map<String, dynamic>))
        .where((category) => category.active)
        .toList();
  }
}
