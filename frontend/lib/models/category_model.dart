/// A product category, as returned by `GET /api/categories`.
class CategoryModel {
  final String id;
  final String name;
  final String slug;
  final String? parentId;
  final bool active;

  /// Category photo (Cloudinary URL), or null if none is set yet.
  ///
  /// Corresponds to an `imageUrl` field the backend doesn't expose yet on
  /// `CategoryResponse` (see `DEMANDES_MODIFICATIONS_BACKEND.md`). Stays
  /// `null` until it's added — [CategoryWidget] falls back to a generic
  /// icon avatar in that case.
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
