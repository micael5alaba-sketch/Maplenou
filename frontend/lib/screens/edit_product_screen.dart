import 'package:flutter/material.dart';

import '../models/category_model.dart';
import '../models/product_specification_entry.dart';
import '../services/api_client.dart';
import '../services/category_service.dart';
import '../services/media_upload_service.dart';
import '../services/vendor_catalog_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/primary_button.dart';
import 'add_product_screen.dart' show DottedBorderBox;

/// "Modifier le produit" — reached from [VendorCatalogScreen]'s 3-dot menu.
///
/// Charge la fiche complète (`GET /api/shops/mine/products/{id}`) puis
/// permet de modifier les champs principaux (un seul `PATCH` global au clic
/// sur "Enregistrer"), pendant que les variantes et les photos s'appliquent
/// immédiatement à chaque action (ajout/suppression/modification), comme sur
/// [AddressesScreen] — plus simple et plus sûr qu'un diff différé sur des
/// sous-ressources qui ont chacune leur propre endpoint côté backend.
class EditProductScreen extends StatefulWidget {
  final String productId;

  const EditProductScreen({super.key, required this.productId});

  @override
  State<EditProductScreen> createState() => _EditProductScreenState();
}

class _Variant {
  String id;
  final TextEditingController label;
  final TextEditingController stock;
  final TextEditingController price;
  bool isSaving = false;

  _Variant({required this.id, required String label, required String stock, required String price})
      : label = TextEditingController(text: label),
        stock = TextEditingController(text: stock),
        price = TextEditingController(text: price);

  void dispose() {
    label.dispose();
    stock.dispose();
    price.dispose();
  }
}

class _Image {
  String id;
  String url;
  _Image({required this.id, required this.url});
}

class _EditProductScreenState extends State<EditProductScreen> {
  final _service = VendorCatalogService();
  final _mediaUploadService = MediaUploadService();
  final _formKey = GlobalKey<FormState>();

  late Future<void> _loadFuture;

  final _nameController = TextEditingController();
  final _descriptionController = TextEditingController();
  final _priceController = TextEditingController();
  final _oldPriceController = TextEditingController();
  final _weightController = TextEditingController();
  final _delayController = TextEditingController();

  late final Future<List<CategoryModel>> _categoriesFuture = CategoryService().getCategories();
  CategoryModel? _category;
  String _status = 'DRAFT';
  bool _deliveryAvailable = true;
  final List<ProductSpecificationEntry> _specifications = [];
  final List<_Variant> _variants = [];
  final List<_Image> _images = [];
  bool _isUploadingImage = false;
  bool _isSubmitting = false;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _loadFuture = _load();
  }

  @override
  void dispose() {
    _nameController.dispose();
    _descriptionController.dispose();
    _priceController.dispose();
    _oldPriceController.dispose();
    _weightController.dispose();
    _delayController.dispose();
    for (final v in _variants) {
      v.dispose();
    }
    super.dispose();
  }

  Future<void> _load() async {
    final detail = await _service.getProductDetail(widget.productId);
    _nameController.text = detail['name'] as String;
    _descriptionController.text = (detail['description'] as String?) ?? '';
    _priceController.text = '${detail['basePrice']}';
    _oldPriceController.text = detail['oldPrice'] != null ? '${detail['oldPrice']}' : '';
    _weightController.text = detail['packageWeightKg'] != null ? '${detail['packageWeightKg']}' : '';
    _delayController.text = (detail['estimatedDeliveryDelay'] as String?) ?? '';
    _deliveryAvailable = detail['deliveryAvailable'] as bool? ?? true;
    _status = detail['status'] as String? ?? 'DRAFT';

    final categories = await _categoriesFuture;
    final categoryId = detail['categoryId'] as String?;
    final matches = categories.where((c) => c.id == categoryId);
    _category = matches.isEmpty ? null : matches.first;

    final specs = detail['specifications'] as List<dynamic>? ?? [];
    _specifications.clear();
    _specifications.addAll(specs.map((s) {
      final map = s as Map<String, dynamic>;
      return ProductSpecificationEntry(label: map['label'] as String? ?? '', value: map['value'] as String? ?? '');
    }));

    final variants = detail['variants'] as List<dynamic>? ?? [];
    _variants.clear();
    _variants.addAll(variants.map((v) {
      final map = v as Map<String, dynamic>;
      return _Variant(
        id: map['id'] as String,
        label: map['label'] as String? ?? '',
        stock: '${map['stockQuantity']}',
        price: map['priceOverride'] != null ? '${map['priceOverride']}' : '',
      );
    }));

    final images = detail['images'] as List<dynamic>? ?? [];
    _images.clear();
    _images.addAll(images.map((i) {
      final map = i as Map<String, dynamic>;
      return _Image(id: map['id'] as String, url: map['url'] as String);
    }));
  }

  void _showMessage(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  Future<void> _addImage() async {
    setState(() => _isUploadingImage = true);
    try {
      final url = await _mediaUploadService.pickImageAndUpload(folder: 'products');
      if (url == null) return;
      await _service.addImage(widget.productId, url, _images.length);
      if (!mounted) return;
      setState(() => _images.add(_Image(id: '', url: url)));
      // Recharger pour récupérer le vrai id de l'image (nécessaire pour pouvoir la supprimer ensuite).
      final detail = await _service.getProductDetail(widget.productId);
      final images = detail['images'] as List<dynamic>? ?? [];
      if (!mounted) return;
      setState(() {
        _images
          ..clear()
          ..addAll(images.map((i) {
            final map = i as Map<String, dynamic>;
            return _Image(id: map['id'] as String, url: map['url'] as String);
          }));
      });
    } on ApiException catch (e) {
      _showMessage(e.message);
    } finally {
      if (mounted) setState(() => _isUploadingImage = false);
    }
  }

  Future<void> _removeImage(_Image image) async {
    try {
      await _service.deleteImage(widget.productId, image.id);
      if (!mounted) return;
      setState(() => _images.remove(image));
    } on ApiException catch (e) {
      _showMessage(e.message);
    }
  }

  Future<void> _saveVariant(_Variant variant) async {
    final stock = int.tryParse(variant.stock.text.trim());
    if (variant.label.text.trim().isEmpty || stock == null || stock < 0) {
      _showMessage('Libellé et stock valides requis pour cette variante.');
      return;
    }
    setState(() => variant.isSaving = true);
    try {
      await _service.updateVariant(
        widget.productId,
        variant.id,
        label: variant.label.text.trim(),
        stockQuantity: stock,
        priceOverride: variant.price.text.trim().isEmpty ? null : num.tryParse(variant.price.text.trim()),
      );
      _showMessage('Variante "${variant.label.text.trim()}" mise à jour.');
    } on ApiException catch (e) {
      _showMessage(e.message);
    } finally {
      if (mounted) setState(() => variant.isSaving = false);
    }
  }

  Future<void> _addVariant() async {
    try {
      final created = await _service.addVariant(widget.productId, label: 'Nouvelle variante', stockQuantity: 0);
      if (!mounted) return;
      setState(() => _variants.add(_Variant(
            id: created['id'] as String,
            label: created['label'] as String,
            stock: '${created['stockQuantity']}',
            price: '',
          )));
    } on ApiException catch (e) {
      _showMessage(e.message);
    }
  }

  Future<void> _removeVariant(_Variant variant) async {
    if (_variants.length <= 1) {
      _showMessage('Un produit doit garder au moins une variante.');
      return;
    }
    try {
      await _service.deleteVariant(widget.productId, variant.id);
      if (!mounted) return;
      setState(() {
        _variants.remove(variant);
        variant.dispose();
      });
    } on ApiException catch (e) {
      _showMessage(e.message);
    }
  }

  void _addSpecification() => setState(() => _specifications.add(ProductSpecificationEntry()));
  void _removeSpecification(int index) => setState(() => _specifications.removeAt(index));

  Future<void> _handleSave() async {
    if (!_formKey.currentState!.validate()) return;
    if (_category == null) {
      _showMessage('Choisis une catégorie.');
      return;
    }

    setState(() {
      _isSubmitting = true;
      _errorMessage = null;
    });
    try {
      await _service.updateProduct(
        productId: widget.productId,
        name: _nameController.text.trim(),
        description: _descriptionController.text.trim().isEmpty ? null : _descriptionController.text.trim(),
        basePrice: num.parse(_priceController.text),
        oldPrice: _oldPriceController.text.trim().isEmpty ? null : num.parse(_oldPriceController.text),
        categoryId: _category!.id,
        status: _status,
        specifications: _specifications
            .where((s) => s.label.trim().isNotEmpty && s.value.trim().isNotEmpty)
            .map((s) => {'label': s.label.trim(), 'value': s.value.trim()})
            .toList(),
        packageWeightKg: _weightController.text.trim().isEmpty ? null : num.parse(_weightController.text),
        estimatedDeliveryDelay: _delayController.text.trim().isEmpty ? null : _delayController.text.trim(),
        deliveryAvailable: _deliveryAvailable,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(const SnackBar(content: Text('Produit mis à jour.')));
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
              child: FutureBuilder<void>(
                future: _loadFuture,
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return Center(child: CircularProgressIndicator(color: colors.primary));
                  }
                  if (snapshot.hasError) {
                    return Center(
                      child: Padding(
                        padding: const EdgeInsets.all(32),
                        child: Text(
                          snapshot.error is ApiException ? (snapshot.error as ApiException).message : 'Impossible de charger ce produit.',
                          textAlign: TextAlign.center,
                          style: TextStyle(color: colors.textMuted),
                        ),
                      ),
                    );
                  }
                  return _buildForm(colors);
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildForm(AppColorScheme colors) {
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
      child: Form(
        key: _formKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
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
            _buildStatusSection(colors),
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
                  child: PrimaryButton(label: 'Enregistrer', isLoading: _isSubmitting, onPressed: _handleSave),
                ),
              ],
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
          IconButton(icon: Icon(Icons.arrow_back_rounded, color: colors.textDark), onPressed: () => Navigator.of(context).pop()),
          Expanded(
            child: Text('Modifier le produit', textAlign: TextAlign.center, style: TextStyle(fontSize: 17, fontWeight: FontWeight.bold, color: colors.textDark)),
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
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 12, offset: const Offset(0, 4))],
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

  Widget _buildStatusSection(AppColorScheme colors) {
    return _buildCard(colors, 'Visibilité', [
      Row(
        children: [
          Expanded(
            child: Text(
              _status == 'ACTIVE' ? 'Visible dans le catalogue public' : 'Masqué (brouillon)',
              style: TextStyle(color: colors.textDark, fontWeight: FontWeight.w600),
            ),
          ),
          Switch(
            value: _status == 'ACTIVE',
            activeThumbColor: colors.primary,
            onChanged: (value) => setState(() => _status = value ? 'ACTIVE' : 'DRAFT'),
          ),
        ],
      ),
    ]);
  }

  Widget _buildImagesSection(AppColorScheme colors) {
    return _buildCard(colors, 'Photos du produit', [
      GridView.count(
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        crossAxisCount: 2,
        mainAxisSpacing: 12,
        crossAxisSpacing: 12,
        childAspectRatio: 1.3,
        children: [
          for (final image in _images) _buildExistingImageSlot(colors, image),
          _buildAddImageSlot(colors),
        ],
      ),
    ]);
  }

  Widget _buildExistingImageSlot(AppColorScheme colors, _Image image) {
    return DottedBorderBox(
      color: colors.inputFill,
      child: Stack(
        fit: StackFit.expand,
        children: [
          ClipRRect(borderRadius: BorderRadius.circular(14), child: Image.network(image.url, fit: BoxFit.cover)),
          Positioned(
            top: 6,
            right: 6,
            child: InkWell(
              borderRadius: BorderRadius.circular(20),
              onTap: () => _removeImage(image),
              child: Container(
                padding: const EdgeInsets.all(4),
                decoration: BoxDecoration(color: Colors.black.withValues(alpha: 0.55), shape: BoxShape.circle),
                child: const Icon(Icons.close_rounded, color: Colors.white, size: 16),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAddImageSlot(AppColorScheme colors) {
    return InkWell(
      borderRadius: BorderRadius.circular(14),
      onTap: _isUploadingImage ? null : _addImage,
      child: DottedBorderBox(
        color: colors.surface,
        child: _isUploadingImage
            ? Center(child: CircularProgressIndicator(strokeWidth: 2.4, color: colors.primary))
            : Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.add_rounded, color: colors.textMuted),
                  const SizedBox(height: 6),
                  Text('Ajouter une photo', textAlign: TextAlign.center, style: TextStyle(fontSize: 11, color: colors.textMuted)),
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
      TextFormField(controller: _descriptionController, maxLines: 3, decoration: _inputDecoration(colors, "Décris l'article...")),
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
            return SizedBox(height: 48, child: Center(child: CircularProgressIndicator(strokeWidth: 2, color: colors.primary)));
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
      for (var i = 0; i < _specifications.length; i++) ...[
        const SizedBox(height: 10),
        _buildSpecificationRow(colors, i),
      ],
    ]);
  }

  Widget _buildSpecificationRow(AppColorScheme colors, int index) {
    final entry = _specifications[index];
    return Row(
      key: ValueKey(entry),
      children: [
        Expanded(
          child: TextFormField(
            initialValue: entry.label,
            decoration: _inputDecoration(colors, 'Libellé'),
            onChanged: (value) => entry.label = value,
          ),
        ),
        const SizedBox(width: 8),
        Expanded(
          child: TextFormField(
            initialValue: entry.value,
            decoration: _inputDecoration(colors, 'Valeur'),
            onChanged: (value) => entry.value = value,
          ),
        ),
        IconButton(icon: Icon(Icons.close_rounded, color: colors.error, size: 20), onPressed: () => _removeSpecification(index)),
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
        'Chaque variante s\'enregistre séparément (bouton "Enregistrer" sur sa ligne).',
        style: TextStyle(fontSize: 12, color: colors.textMuted),
      ),
      for (final variant in _variants) ...[
        const SizedBox(height: 12),
        _buildVariantRow(colors, variant),
      ],
      const SizedBox(height: 12),
      TextButton.icon(
        onPressed: _addVariant,
        icon: Icon(Icons.add_rounded, size: 18, color: colors.primary),
        label: Text('Ajouter une variante', style: TextStyle(color: colors.primary, fontWeight: FontWeight.w600)),
      ),
    ]);
  }

  Widget _buildVariantRow(AppColorScheme colors, _Variant variant) {
    return Container(
      key: ValueKey(variant),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(color: colors.inputFill, borderRadius: BorderRadius.circular(14)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(child: TextFormField(controller: variant.label, decoration: _inputDecoration(colors, 'Libellé'))),
              IconButton(icon: Icon(Icons.close_rounded, color: colors.error, size: 20), onPressed: () => _removeVariant(variant)),
            ],
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              Expanded(child: TextFormField(controller: variant.stock, keyboardType: TextInputType.number, decoration: _inputDecoration(colors, 'Stock'))),
              const SizedBox(width: 8),
              Expanded(child: TextFormField(controller: variant.price, keyboardType: const TextInputType.numberWithOptions(decimal: false), decoration: _inputDecoration(colors, 'Prix (optionnel)'))),
            ],
          ),
          const SizedBox(height: 8),
          SizedBox(
            width: double.infinity,
            child: OutlinedButton(
              onPressed: variant.isSaving ? null : () => _saveVariant(variant),
              style: OutlinedButton.styleFrom(foregroundColor: colors.primary, side: BorderSide(color: colors.primary)),
              child: variant.isSaving
                  ? SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: colors.primary))
                  : const Text('Enregistrer cette variante'),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDeliverySection(AppColorScheme colors) {
    return _buildCard(colors, 'Livraison (optionnel)', [
      _buildLabel(colors, 'Poids du colis (kg)'),
      const SizedBox(height: 8),
      TextFormField(controller: _weightController, keyboardType: const TextInputType.numberWithOptions(decimal: true), decoration: _inputDecoration(colors, 'ex. 0.8')),
      const SizedBox(height: 16),
      _buildLabel(colors, 'Délai de livraison estimé'),
      const SizedBox(height: 8),
      TextFormField(controller: _delayController, decoration: _inputDecoration(colors, 'ex. 2 à 4 jours')),
      const SizedBox(height: 16),
      Row(
        children: [
          Expanded(child: _buildLabel(colors, 'Livraison disponible pour ce produit')),
          Switch(value: _deliveryAvailable, activeThumbColor: colors.primary, onChanged: (value) => setState(() => _deliveryAvailable = value)),
        ],
      ),
    ]);
  }

  Widget _buildLabel(AppColorScheme colors, String label) {
    return Text(label, style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: colors.textDark));
  }

  InputDecoration _inputDecoration(AppColorScheme colors, String? hint) {
    final border = OutlineInputBorder(borderRadius: BorderRadius.circular(14), borderSide: BorderSide(color: colors.border));
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
