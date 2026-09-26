import 'package:flutter/material.dart';

import '../services/api_client.dart';
import '../services/courier_review_service.dart';
import '../theme/app_color_scheme.dart';

/// Dialogue de notation du livreur (1 à 5 étoiles + commentaire optionnel),
/// ouvert depuis [OrderTrackingScreen] une fois la sous-commande DELIVERED.
/// Soumet via [CourierReviewService] (`POST /api/reviews/couriers`).
class CourierRatingDialog extends StatefulWidget {
  final String deliveryId;
  final String courierName;

  const CourierRatingDialog({super.key, required this.deliveryId, required this.courierName});

  static Future<bool?> show(BuildContext context, {required String deliveryId, required String courierName}) {
    return showDialog<bool>(
      context: context,
      builder: (_) => CourierRatingDialog(deliveryId: deliveryId, courierName: courierName),
    );
  }

  @override
  State<CourierRatingDialog> createState() => _CourierRatingDialogState();
}

class _CourierRatingDialogState extends State<CourierRatingDialog> {
  int _rating = 0;
  final _commentController = TextEditingController();
  bool _submitting = false;
  String? _error;

  @override
  void dispose() {
    _commentController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_rating == 0) {
      setState(() => _error = 'Choisissez une note de 1 à 5 étoiles.');
      return;
    }
    setState(() {
      _submitting = true;
      _error = null;
    });
    try {
      await CourierReviewService().submitReview(
        deliveryId: widget.deliveryId,
        rating: _rating,
        comment: _commentController.text,
      );
      if (!mounted) return;
      Navigator.of(context).pop(true);
    } on ApiException catch (e) {
      setState(() {
        _submitting = false;
        _error = e.statusCode == 409 ? 'Vous avez déjà noté ce livreur pour cette livraison.' : e.message;
      });
    } catch (_) {
      setState(() {
        _submitting = false;
        _error = "Impossible d'envoyer la note pour le moment.";
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return AlertDialog(
      title: Text('Noter ${widget.courierName}'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: List.generate(5, (i) {
              final filled = i < _rating;
              return IconButton(
                icon: Icon(filled ? Icons.star_rounded : Icons.star_border_rounded, color: Colors.amber, size: 32),
                onPressed: _submitting ? null : () => setState(() => _rating = i + 1),
              );
            }),
          ),
          const SizedBox(height: 8),
          TextField(
            controller: _commentController,
            enabled: !_submitting,
            maxLines: 3,
            maxLength: 2000,
            decoration: const InputDecoration(
              hintText: 'Un commentaire (optionnel)',
              border: OutlineInputBorder(),
            ),
          ),
          if (_error != null) ...[
            const SizedBox(height: 4),
            Text(_error!, style: TextStyle(color: colors.error, fontSize: 12)),
          ],
        ],
      ),
      actions: [
        TextButton(
          onPressed: _submitting ? null : () => Navigator.of(context).pop(false),
          child: const Text('Annuler'),
        ),
        ElevatedButton(
          onPressed: _submitting ? null : _submit,
          child: _submitting
              ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
              : const Text('Envoyer'),
        ),
      ],
    );
  }
}
