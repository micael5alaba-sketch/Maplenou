/// A product category, as returned by `GET /api/categories`.
class CategoryModel {
  final String id;
  final String name;
  final String slug;
  final String? parentId;
  final bool active;

  /// Category photo (Cloudinary URL), from `CategoryResponse.imageUrl` —
  /// `null` falls back to a generic icon avatar ([CategoryWidget]).
  final String? imageUrl;

  const CategoryModel({
    required this.id,
    required this.name,
    required this.slug,
    this.parentId,
    this.active = true,
    this.imageUrl,
  });

  factory CategoryModel.fromJson(Map<String, dynamic> json) {
    return CategoryModel(
      id: json['id'] as String,
      name: json['name'] as String,
      slug: json['slug'] as String,
      parentId: json['parentId'] as String?,
      active: json['active'] as bool? ?? true,
      imageUrl: json['imageUrl'] as String?,
    );
  }
}
