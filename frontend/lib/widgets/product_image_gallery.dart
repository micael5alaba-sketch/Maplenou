import 'package:flutter/material.dart';

import '../theme/app_color_scheme.dart';

/// Main product photo with a thumbnail strip below it.
///
/// Tapping a thumbnail swaps the main image; the selected thumbnail gets a
/// green border. When there are more photos than fit in the strip, the
/// last visible thumbnail is dimmed with a "+N" badge for the rest.
class ProductImageGallery extends StatefulWidget {
  final List<String> images;

  static const int maxVisibleThumbnails = 4;

  const ProductImageGallery({super.key, required this.images});

  @override
  State<ProductImageGallery> createState() => _ProductImageGalleryState();
}

class _ProductImageGalleryState extends State<ProductImageGallery> {
  int _selectedIndex = 0;

  @override
  Widget build(BuildContext context) {
    final images = widget.images;
    if (images.isEmpty) {
      return ClipRRect(
        borderRadius: BorderRadius.circular(20),
        child: AspectRatio(aspectRatio: 1, child: _buildPlaceholder()),
      );
    }

    final visibleCount = images.length > ProductImageGallery.maxVisibleThumbnails
        ? ProductImageGallery.maxVisibleThumbnails
        : images.length;

    return Column(
      children: [
        ClipRRect(
          borderRadius: BorderRadius.circular(20),
          child: AspectRatio(
            aspectRatio: 1,
            child: _buildImage(images[_selectedIndex]),
          ),
        ),
        if (images.length > 1) ...[
          const SizedBox(height: 12),
          SizedBox(
            height: 72,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              itemCount: visibleCount,
              separatorBuilder: (context, index) => const SizedBox(width: 10),
              itemBuilder: (context, index) => _buildThumbnail(index, images, visibleCount),
            ),
          ),
        ],
      ],
    );
  }

  Widget _buildThumbnail(int index, List<String> images, int visibleCount) {
    final isSelected = _selectedIndex == index;
    final isLastVisible = index == visibleCount - 1 && images.length > visibleCount;
    final remaining = images.length - visibleCount;

    return GestureDetector(
      onTap: () => setState(() => _selectedIndex = index),
      child: Stack(
        children: [
          Container(
            width: 72,
            height: 72,
            padding: const EdgeInsets.all(2),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(12),
              border: Border.all(
                color: isSelected ? context.colors.primary : Colors.transparent,
                width: 2,
              ),
            ),
            child: ClipRRect(
              borderRadius: BorderRadius.circular(9),
              child: _buildImage(images[index]),
            ),
          ),
          if (isLastVisible)
            Positioned.fill(
              child: DecoratedBox(
                decoration: BoxDecoration(
                  color: Colors.black.withValues(alpha: 0.45),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Center(
                  child: Text(
                    '+$remaining',
                    style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16),
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildImage(String url) {
    return Image.network(
      url,
      fit: BoxFit.cover,
      errorBuilder: (context, error, stackTrace) => _buildPlaceholder(),
      loadingBuilder: (context, child, progress) =>
          progress == null ? child : _buildPlaceholder(loading: true),
    );
  }

  Widget _buildPlaceholder({bool loading = false}) {
    final colors = context.colors;

    return Container(
      color: colors.inputFill,
      alignment: Alignment.center,
      child: loading
          ? SizedBox(
              width: 22,
              height: 22,
              child: CircularProgressIndicator(strokeWidth: 2, color: colors.primary),
            )
          : Icon(Icons.image_not_supported_outlined, color: colors.textMuted, size: 32),
    );
  }
}
