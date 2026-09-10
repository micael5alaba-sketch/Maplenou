import 'dart:async';

import 'package:flutter/material.dart';

import '../models/category_model.dart';
import 'category_widget.dart';

/// Horizontal row of [CategoryWidget]s that scrolls itself automatically,
/// looping back to the start once it reaches the end — while staying fully
/// draggable by the user at any time.
///
/// A manual drag pauses the auto-scroll for a few seconds so it doesn't
/// fight the user's gesture, then resumes on its own.
class CategoryCarousel extends StatefulWidget {
  final List<CategoryModel> categories;
  final void Function(CategoryModel category) onCategoryTap;

  const CategoryCarousel({
    super.key,
    required this.categories,
    required this.onCategoryTap,
  });

  @override
  State<CategoryCarousel> createState() => _CategoryCarouselState();
}

class _CategoryCarouselState extends State<CategoryCarousel> {
  static const _tickInterval = Duration(seconds: 3);
  static const _scrollStep = 90.0;
  static const _resumeDelay = Duration(seconds: 4);

  final _scrollController = ScrollController();
  Timer? _autoScrollTimer;
  Timer? _resumeTimer;

  @override
  void initState() {
    super.initState();
    _startAutoScroll();
  }

  @override
  void dispose() {
    _autoScrollTimer?.cancel();
    _resumeTimer?.cancel();
    _scrollController.dispose();
    super.dispose();
  }

  void _startAutoScroll() {
    _autoScrollTimer?.cancel();
    _autoScrollTimer = Timer.periodic(_tickInterval, (_) => _advance());
  }

  void _advance() {
    if (!_scrollController.hasClients) return;
    final position = _scrollController.position;
    final next = position.pixels + _scrollStep;

    if (next >= position.maxScrollExtent) {
      _scrollController.animateTo(
        0,
        duration: const Duration(milliseconds: 600),
        curve: Curves.easeInOut,
      );
    } else {
      _scrollController.animateTo(
        next,
        duration: const Duration(milliseconds: 600),
        curve: Curves.easeInOut,
      );
    }
  }

  /// Called when the user starts dragging: pause auto-scroll, then queue it
  /// back up a little while after they let go.
  void _onUserInteraction() {
    _autoScrollTimer?.cancel();
    _resumeTimer?.cancel();
    _resumeTimer = Timer(_resumeDelay, _startAutoScroll);
  }

  @override
  Widget build(BuildContext context) {
    return NotificationListener<ScrollNotification>(
      onNotification: (notification) {
        if (notification is ScrollStartNotification && notification.dragDetails != null) {
          _onUserInteraction();
        }
        return false;
      },
      child: ListView.separated(
        controller: _scrollController,
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 16),
        itemCount: widget.categories.length,
        separatorBuilder: (context, index) => const SizedBox(width: 22),
        itemBuilder: (context, index) {
          final category = widget.categories[index];
          return CategoryWidget(
            category: category,
            onTap: () => widget.onCategoryTap(category),
          );
        },
      ),
    );
  }
}
