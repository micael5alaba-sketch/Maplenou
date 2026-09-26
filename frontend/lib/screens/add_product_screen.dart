import 'package:flutter/material.dart';

import '../models/category_model.dart';
import '../models/product_specification_entry.dart';
import '../models/product_variant_entry.dart';
import '../services/api_client.dart';
import '../services/category_service.dart';
import '../services/media_upload_service.dart';
import '../services/vendor_catalog_service.dart';
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
/// (JSONB list of `{label, value}`, see `MODELE_DONNEES.md`).
///
/// Variantes : le backend accepte un libellé libre par variante, pas une
/// vraie structure d'attributs (Taille/Couleur séparés) — pour une
/// combinaison, le vendeur tape directement "M / Rouge" comme un seul
/// libellé (voir `ProductVariantEntry`).
///
/// Branché sur `POST /api/shops/mine/products` (voir
/// `VendorCatalogService.createProduct`). Les photos sont uploadées sur
/// Cloudinary via [MediaUploadService] dès la sélection (pas seulement à la
/// publication), puis rattachées au produit une fois créé.
class AddProductScreen extends StatefulWidget {
  const AddProductScreen({super.key});

  @override
  State<AddProductScreen> createState() => _AddProductScreenState();
}

class _AddProductScreenState extends State<AddProductScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _descriptionController = TextEditingController();
  final _priceController = TextEditingController();
  final _oldPriceController = TextEditingController();
  final _weightController = TextEditingController();
  final _delayController = TextEditingController();

  late final Future<List<CategoryModel>> _categoriesFuture = CategoryService().getCategories();
  CategoryModel? _category;
  final List<ProductSpecificationEntry> _specifications = [];
  final List<ProductVariantEntry> _variants = [ProductVariantEntry(label: 'Standard', stockQuantity: '1')];
  bool _deliveryAvailable = true;
  bool _isSubmitting = false;
  String? _errorMessage;

  final _mediaUploadService = MediaUploadService();
  final List<String?> _imageUrls = List<String?>.filled(4, null);
  final List<bool> _isUploadingImage = List<bool>.filled(4, false);

  @override
  void dispose() {
    _nameController.dispose();
    _descriptionController.dispose();
    _priceController.dispose();
    _oldPriceController.dispose();
    _weightController.dispose();
    _delayController.dispose();
    super.dispose();
  }

  Future<void> _pickImage(int index) async {
    setState(() => _isUploadingImage[index] = true);
    try {
      final url = await _mediaUploadService.pickImageAndUpload(folder: 'products');
      if (url != null) setState(() => _imageUrls[index] = url);
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(content: Text(e.message)));
    } finally {
      if (mounted) setState(() => _isUploadingImage[index] = false);
    }
  }

  void _removeImage(int index) {
    setState(() => _imageUrls[index] = null);
  }

  void _addSpecification() {
    setState(() => _specifications.add(ProductSpecificationEntry()));
  }

  void _removeSpecification(int index) {
    setState(() => _specifications.removeAt(index));
  }

  void _addVariant() {
    setState(() => _variants.add(ProductVariantEntry()));
  }

  void _removeVariant(int index) {
    setState(() => _variants.removeAt(index));
  }

  Future<void> _handlePublish() async {
    if (!_formKey.currentState!.validate()) return;
    if (_category == null) {
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(const SnackBar(content: Text('Choisis une catégorie.')));
      return;
    }
    if (_variants.any((v) => v.label.trim().isEmpty || int.tryParse(v.stockQuantity) == null || int.parse(v.stockQuantity) < 0)) {
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(const SnackBar(content: Text('Chaque variante a besoin d\'un libellé et d\'un stock valide.')));
      return;
    }

    setState(() {
      _isSubmitting = true;
      _errorMessage = null;
    });
    try {
      await VendorCatalogService().createProduct(
        name: _nameController.text.trim(),
        description: _descriptionController.text.trim().isEmpty ? null : _descriptionController.text.trim(),
        basePrice: num.parse(_priceController.text),
        oldPrice: _oldPriceController.text.trim().isEmpty ? null : num.parse(_oldPriceController.text),
        categoryId: _category!.id,
        variants: _variants
            .map((v) => {
                  'label': v.label.trim(),
                  'stockQuantity': int.parse(v.stockQuantity),
                  if (v.priceOverride.trim().isNotEmpty) 'priceOverride': num.parse(v.priceOverride),
                })
            .toList(),
        specifications: _specifications
            .where((s) => s.label.trim().isNotEmpty && s.value.trim().isNotEmpty)
            .map((s) => {'label': s.label.trim(), 'value': s.value.trim()})
            .toList(),
        imageUrls: _imageUrls.whereType<String>().toList(),
        packageWeightKg: _weightController.text.trim().isEmpty ? null : num.parse(_weightController.text),
        estimatedDeliveryDelay: _delayController.text.trim().isEmpty ? null : _delayController.text.trim(),
        deliveryAvailable: _deliveryAvailable,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(const SnackBar(content: Text('Produit publié !')));
      Navigator.of(context).pop(true);
    } on ApiException catch (e) {
      setState(() => _errorMessage = e.message);
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
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
                      if (_errorMessage != null) ...[
                        Container(
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: colors.error.withValues(alpha: 0.08),
                            borderRadius: BorderRadius.circular(12),
                            border: Border.all(color: colors.error.withValues(alpha: 0.3)),
                          ),
                          child: Text(_errorMessage!, style: TextStyle(color: colors.error, fontSize: 13)),
                        ),
                        const SizedBox(height: 16),
                      ],
                      _buildImagesSection(colors),
                      const SizedBox(height: 16),
                      _buildBasicInfoSection(colors),
                      const SizedBox(height: 16),
                      _buildOrganisationSection(colors),
                      const SizedBox(height: 16),
                      _buildVariantsSection(colors),
                      const SizedBox(height: 16),
                      _buildPriceSection(colors),
                      const SizedBox(height: 16),
                      _buildDeliverySection(colors),
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
                              onPressed: _isUploadingImage.any((u) => u) ? null : _handlePublish,
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
          for (var i = 0; i < _imageUrls.length; i++)
            _buildImageSlot(colors, i, label: i == 0 ? 'Photo principale' : 'Ajouter une photo'),
        ],
      ),
    ]);
  }

  Widget _buildImageSlot(AppColorScheme colors, int index, {required String label}) {
    final imageUrl = _imageUrls[index];
    final isUploading = _isUploadingImage[index];

    return InkWell(
      borderRadius: BorderRadius.circular(14),
      onTap: isUploading ? null : () => _pickImage(index),
      child: DottedBorderBox(
        color: imageUrl != null ? colors.inputFill : colors.surface,
        child: Stack(
          fit: StackFit.expand,
          children: [
            if (imageUrl != null)
              ClipRRect(
                borderRadius: BorderRadius.circular(14),
                child: Image.network(imageUrl, fit: BoxFit.cover),
              )
            else
              Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.add_rounded, color: colors.textMuted),
                  const SizedBox(height: 6),
                  Text(label, textAlign: TextAlign.center, style: TextStyle(fontSize: 11, color: colors.textMuted)),
                ],
              ),
            if (isUploading)
              ColoredBox(
                color: Colors.black.withValues(alpha: 0.35),
                child: Center(
                  child: SizedBox(
                    width: 22,
                    height: 22,
                    child: CircularProgressIndicator(strokeWidth: 2.4, color: colors.surface),
                  ),
                ),
              ),
            if (imageUrl != null && !isUploading)
              Positioned(
                top: 6,
                right: 6,
                child: InkWell(
                  borderRadius: BorderRadius.circular(20),
                  onTap: () => _removeImage(index),
                  child: Container(
                    padding: const EdgeInsets.all(4),
                    decoration: BoxDecoration(color: Colors.black.withValues(alpha: 0.55), shape: BoxShape.circle),
                    child: const Icon(Icons.close_rounded, color: Colors.white, size: 16),
                  ),
                ),
              ),
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
      FutureBuilder<List<CategoryModel>>(
        future: _categoriesFuture,
        builder: (context, snapshot) {
          if (!snapshot.hasData) {
            return SizedBox(
              height: 48,
              child: Center(
                child: SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(strokeWidth: 2, color: colors.primary),
                ),
              ),
            );
          }
          final categories = snapshot.data!;
          return DropdownButtonFormField<CategoryModel>(
            initialValue: _category,
            decoration: _inputDecoration(colors, 'Choisir une catégorie'),
            items: categories.map((c) => DropdownMenuItem(value: c, child: Text(c.name))).toList(),
            onChanged: (value) => setState(() => _category = value),
          );
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
    ]);
  }

  Widget _buildVariantsSection(AppColorScheme colors) {
    return _buildCard(colors, 'Variantes', [
      Text(
        "Une ligne par variante vendue séparément (ex: \"S\", \"M / Rouge\"...) — chacune avec son propre stock, et un prix différent du prix de base si besoin.",
        style: TextStyle(fontSize: 12, color: colors.textMuted),
      ),
      for (var i = 0; i < _variants.length; i++) ...[
        const SizedBox(height: 12),
        _buildVariantRow(colors, i),
      ],
      const SizedBox(height: 12),
      TextButton.icon(
        onPressed: _addVariant,
        icon: Icon(Icons.add_rounded, size: 18, color: colors.primary),
        label: Text('Ajouter une variante', style: TextStyle(color: colors.primary, fontWeight: FontWeight.w600)),
      ),
    ]);
  }

  Widget _buildVariantRow(AppColorScheme colors, int index) {
    final entry = _variants[index];

    // Keyed by the entry's own identity, same reasoning as _buildSpecificationRow.
    return Container(
      key: ValueKey(entry),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(color: colors.inputFill, borderRadius: BorderRadius.circular(14)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: TextFormField(
                  initialValue: entry.label,
                  decoration: _inputDecoration(colors, 'Libellé (ex: M, Rouge...)'),
                  onChanged: (value) => entry.label = value,
                ),
              ),
              if (_variants.length > 1)
                IconButton(
                  icon: Icon(Icons.close_rounded, color: colors.error, size: 20),
                  onPressed: () => _removeVariant(index),
                ),
            ],
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              Expanded(
                child: TextFormField(
                  initialValue: entry.stockQuantity,
                  keyboardType: TextInputType.number,
                  decoration: _inputDecoration(colors, 'Stock'),
                  onChanged: (value) => entry.stockQuantity = value,
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: TextFormField(
                  initialValue: entry.priceOverride,
                  keyboardType: const TextInputType.numberWithOptions(decimal: false),
                  decoration: _inputDecoration(colors, 'Prix (optionnel)'),
                  onChanged: (value) => entry.priceOverride = value,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildDeliverySection(AppColorScheme colors) {
    return _buildCard(colors, 'Livraison (optionnel)', [
      _buildLabel(colors, 'Poids du colis (kg)'),
      const SizedBox(height: 8),
      TextFormField(
        controller: _weightController,
        keyboardType: const TextInputType.numberWithOptions(decimal: true),
        decoration: _inputDecoration(colors, 'ex. 0.8'),
      ),
      const SizedBox(height: 16),
      _buildLabel(colors, 'Délai de livraison estimé'),
      const SizedBox(height: 8),
      TextFormField(
        controller: _delayController,
        decoration: _inputDecoration(colors, 'ex. 2 à 4 jours'),
      ),
      const SizedBox(height: 16),
      Row(
        children: [
          Expanded(child: _buildLabel(colors, 'Livraison disponible pour ce produit')),
          Switch(
            value: _deliveryAvailable,
            activeThumbColor: colors.primary,
            onChanged: (value) => setState(() => _deliveryAvailable = value),
          ),
        ],
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
