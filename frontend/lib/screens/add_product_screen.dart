import 'package:flutter/material.dart';

import '../models/product_specification_entry.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/primary_button.dart';

/// "Nouveau Produit" form, reached from [VendorCatalogScreen]'s add button.
///
/// The maquette's "Organisation" section only had clothing fields (Taille,
/// Couleur, Matière) — replaced here by a generic, growable list of
/// label/value "Caractéristiques" so the same form works for any product
/// type (électronique, alimentation, bijoux...), matching how
/// [VendorProductModel]/the catalog are documented as deliberately generic.
/// It also maps directly onto the real backend's `Product.specifications`
/// (JSONB list of `{label, value}`, see `MODELE_DONNEES.md`), so wiring
/// this form to `POST /api/shops/mine/products` later needs no reshaping.
///
/// Fully mocked for now: submitting just simulates a network delay, then
/// pops back to the catalog. Image slots are placeholders — no
/// image_picker dependency yet.
class AddProductScreen extends StatefulWidget {
  const AddProductScreen({super.key});

  @override
  State<AddProductScreen> createState() => _AddProductScreenState();
}

class _AddProductScreenState extends State<AddProductScreen> {
  static const _defaultCategory = 'Choisir une catégorie';
  static const _categories = [
    _defaultCategory,
    'Mode',
    'Beauté',
    'Maison',
    'Électronique',
    'Alimentation',
    'Bijoux',
    'Chaussures',
    'Accessoires',
  ];

  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _descriptionController = TextEditingController();
  final _priceController = TextEditingController();
  final _oldPriceController = TextEditingController();
  final _quantityController = TextEditingController(text: '1');

  String _category = _defaultCategory;
  final List<ProductSpecificationEntry> _specifications = [];
  bool _isSubmitting = false;

  @override
  void dispose() {
    _nameController.dispose();
    _descriptionController.dispose();
    _priceController.dispose();
    _oldPriceController.dispose();
    _quantityController.dispose();
    super.dispose();
  }

  void _showComingSoon(String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  void _addSpecification() {
    setState(() => _specifications.add(ProductSpecificationEntry()));
  }

  void _removeSpecification(int index) {
    setState(() => _specifications.removeAt(index));
  }

  Future<void> _handlePublish() async {
    if (!_formKey.currentState!.validate()) return;
    if (_category == _defaultCategory) {
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(const SnackBar(content: Text('Choisis une catégorie.')));
      return;
    }

    setState(() => _isSubmitting = true);
    // Mocké — pas encore branché sur POST /api/shops/mine/products.
    await Future.delayed(const Duration(seconds: 2));
    if (!mounted) return;
    setState(() => _isSubmitting = false);

    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(const SnackBar(content: Text('Produit publié !')));
    Navigator.of(context).pop(true);
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Scaffold(
      backgroundColor: colors.lightBackground,
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(colors),
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
                child: Form(
                  key: _formKey,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const SizedBox(height: 8),
                      Text(
                        'Nouveau Produit',
                        style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: colors.primary),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        'Ajouter le produit avec ses caractéristiques.',
                        style: TextStyle(fontSize: 13, color: colors.textMuted),
                      ),
                      const SizedBox(height: 24),
                      _buildImagesSection(colors),
                      const SizedBox(height: 16),
                      _buildBasicInfoSection(colors),
                      const SizedBox(height: 16),
                      _buildOrganisationSection(colors),
                      const SizedBox(height: 16),
                      _buildPriceSection(colors),
                      const SizedBox(height: 24),
                      Row(
                        children: [
                          Expanded(
                            child: SizedBox(
                              height: 54,
                              child: OutlinedButton(
                                onPressed: () => Navigator.of(context).pop(),
                                style: OutlinedButton.styleFrom(
                                  foregroundColor: colors.textDark,
                                  side: BorderSide(color: colors.border),
                                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(30)),
                                ),
                                child: const Text('Annuler', style: TextStyle(fontWeight: FontWeight.w600)),
                              ),
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: PrimaryButton(
                              label: 'Publier le produit',
                              isLoading: _isSubmitting,
                              onPressed: _handlePublish,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader(AppColorScheme colors) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: Row(
        children: [
          IconButton(
            icon: Icon(Icons.arrow_back_rounded, color: colors.textDark),
            onPressed: () => Navigator.of(context).pop(),
          ),
          Expanded(
            child: Text(
              'Ajouter un produit',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 17, fontWeight: FontWeight.bold, color: colors.textDark),
            ),
          ),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildCard(AppColorScheme colors, String title, List<Widget> children) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 12, offset: const Offset(0, 4)),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: colors.primary)),
          const SizedBox(height: 16),
          ...children,
        ],
      ),
    );
  }

  Widget _buildImagesSection(AppColorScheme colors) {
    return _buildCard(colors, 'Photos du produit', [
      Text("Ajouter jusqu'à 4 photos", style: TextStyle(fontSize: 12, color: colors.textMuted)),
      const SizedBox(height: 12),
      GridView.count(
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        crossAxisCount: 2,
        mainAxisSpacing: 12,
        crossAxisSpacing: 12,
        childAspectRatio: 1.3,
        children: [
          _buildImageSlot(colors, 'Photo principale', filled: true),
          for (var i = 0; i < 3; i++) _buildImageSlot(colors, 'Ajouter une photo'),
        ],
      ),
    ]);
  }

  Widget _buildImageSlot(AppColorScheme colors, String label, {bool filled = false}) {
    return InkWell(
      borderRadius: BorderRadius.circular(14),
      onTap: () => _showComingSoon('La sélection de photo'),
      child: DottedBorderBox(
        color: filled ? colors.inputFill : colors.surface,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(filled ? Icons.add_photo_alternate_rounded : Icons.add_rounded, color: colors.textMuted),
            const SizedBox(height: 6),
            Text(label, textAlign: TextAlign.center, style: TextStyle(fontSize: 11, color: colors.textMuted)),
          ],
        ),
      ),
    );
  }

  Widget _buildBasicInfoSection(AppColorScheme colors) {
    return _buildCard(colors, 'Informations Basiques', [
      _buildLabel(colors, 'Nom du produit'),
      const SizedBox(height: 8),
      TextFormField(
        controller: _nameController,
        decoration: _inputDecoration(colors, 'ex. Tunique en lin tissée main'),
        validator: (value) => (value == null || value.trim().length < 2) ? 'Indique le nom du produit.' : null,
      ),
      const SizedBox(height: 16),
      _buildLabel(colors, 'Description'),
      const SizedBox(height: 8),
      TextFormField(
        controller: _descriptionController,
        maxLines: 3,
        decoration: _inputDecoration(colors, "Décris l'article, ses particularités et son origine..."),
      ),
    ]);
  }

  Widget _buildOrganisationSection(AppColorScheme colors) {
    return _buildCard(colors, 'Organisation', [
      _buildLabel(colors, 'Catégorie'),
      const SizedBox(height: 8),
      DropdownButtonFormField<String>(
        initialValue: _category,
        decoration: _inputDecoration(colors, null),
        items: _categories.map((c) => DropdownMenuItem(value: c, child: Text(c))).toList(),
        onChanged: (value) {
          if (value != null) setState(() => _category = value);
        },
      ),
      const SizedBox(height: 20),
      Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          _buildLabel(colors, 'Caractéristiques (optionnel)'),
          TextButton.icon(
            onPressed: _addSpecification,
            icon: Icon(Icons.add_rounded, size: 18, color: colors.primary),
            label: Text('Ajouter', style: TextStyle(color: colors.primary, fontWeight: FontWeight.w600)),
          ),
        ],
      ),
      if (_specifications.isEmpty)
        Text(
          'Ex : Marque, Matière, Origine, Pointure — libre à toi.',
          style: TextStyle(fontSize: 12, color: colors.textMuted),
        )
      else
        for (var i = 0; i < _specifications.length; i++) ...[
          const SizedBox(height: 10),
          _buildSpecificationRow(colors, i),
        ],
    ]);
  }

  Widget _buildSpecificationRow(AppColorScheme colors, int index) {
    final entry = _specifications[index];

    // Keyed by the entry's own identity (not its index) so Flutter keeps
    // each TextFormField's editing state attached to the right row when an
    // earlier row is removed and every later index shifts down.
    return Row(
      key: ValueKey(entry),
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Expanded(
          child: TextFormField(
            initialValue: entry.label,
            decoration: _inputDecoration(colors, 'Libellé (ex: Matière)'),
            onChanged: (value) => entry.label = value,
          ),
        ),
        const SizedBox(width: 8),
        Expanded(
          child: TextFormField(
            initialValue: entry.value,
            decoration: _inputDecoration(colors, 'Valeur (ex: Coton bio)'),
            onChanged: (value) => entry.value = value,
          ),
        ),
        IconButton(
          icon: Icon(Icons.close_rounded, color: colors.error, size: 20),
          onPressed: () => _removeSpecification(index),
        ),
      ],
    );
  }

  Widget _buildPriceSection(AppColorScheme colors) {
    return _buildCard(colors, 'Prix', [
      _buildLabel(colors, 'Prix du produit (FCFA)'),
      const SizedBox(height: 8),
      TextFormField(
        controller: _priceController,
        keyboardType: const TextInputType.numberWithOptions(decimal: false),
        decoration: _inputDecoration(colors, '0'),
        validator: (value) {
          final price = num.tryParse(value ?? '');
          if (price == null || price < 500) return 'Le prix minimum est 500 FCFA.';
          return null;
        },
      ),
      const SizedBox(height: 16),
      _buildLabel(colors, 'Prix barré (optionnel)'),
      const SizedBox(height: 8),
      TextFormField(
        controller: _oldPriceController,
        keyboardType: const TextInputType.numberWithOptions(decimal: false),
        decoration: _inputDecoration(colors, 'Prix avant réduction'),
      ),
      const SizedBox(height: 16),
      _buildLabel(colors, 'Quantité en stock'),
      const SizedBox(height: 8),
      TextFormField(
        controller: _quantityController,
        keyboardType: TextInputType.number,
        decoration: _inputDecoration(colors, '1'),
        validator: (value) {
          final qty = int.tryParse(value ?? '');
          if (qty == null || qty < 0) return 'Quantité invalide.';
          return null;
        },
      ),
    ]);
  }

  Widget _buildLabel(AppColorScheme colors, String label) {
    return Text(label, style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: colors.textDark));
  }

  InputDecoration _inputDecoration(AppColorScheme colors, String? hint) {
    final border = OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: BorderSide(color: colors.border),
    );

    return InputDecoration(
      hintText: hint,
      hintStyle: TextStyle(color: colors.textMuted, fontSize: 14),
      filled: true,
      fillColor: colors.inputFill,
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      border: border,
      enabledBorder: border,
      focusedBorder: border.copyWith(borderSide: BorderSide(color: colors.primary, width: 1.6)),
      errorBorder: border.copyWith(borderSide: BorderSide(color: colors.error)),
      focusedErrorBorder: border.copyWith(borderSide: BorderSide(color: colors.error, width: 1.6)),
    );
  }
}

/// Dashed-border placeholder box used for the image slots — plain
/// [DecoratedBox] borders can't be dashed, so this paints one manually.
class DottedBorderBox extends StatelessWidget {
  final Color color;
  final Widget child;

  const DottedBorderBox({super.key, required this.color, required this.child});

  @override
  Widget build(BuildContext context) {
    return CustomPaint(
      painter: _DashedBorderPainter(borderColor: Theme.of(context).extension<AppColorScheme>()!.border),
      child: Container(
        decoration: BoxDecoration(color: color, borderRadius: BorderRadius.circular(14)),
        child: child,
      ),
    );
  }
}

class _DashedBorderPainter extends CustomPainter {
  final Color borderColor;

  _DashedBorderPainter({required this.borderColor});

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = borderColor
      ..strokeWidth = 1.4
      ..style = PaintingStyle.stroke;

    final rrect = RRect.fromRectAndRadius(Offset.zero & size, const Radius.circular(14));
    final path = Path()..addRRect(rrect);
    const dashWidth = 6.0;
    const dashSpace = 4.0;

    for (final metric in path.computeMetrics()) {
      var distance = 0.0;
      while (distance < metric.length) {
        canvas.drawPath(
          metric.extractPath(distance, distance + dashWidth),
          paint,
        );
        distance += dashWidth + dashSpace;
      }
    }
  }

  @override
  bool shouldRepaint(covariant _DashedBorderPainter oldDelegate) => oldDelegate.borderColor != borderColor;
}
