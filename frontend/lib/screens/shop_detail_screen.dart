import 'package:flutter/material.dart';
import 'package:flutter_staggered_grid_view/flutter_staggered_grid_view.dart';

import '../models/product_model.dart';
import '../models/shop_model.dart';
import '../services/api_client.dart';
import '../services/cart_service.dart';
import '../services/conversation_service.dart';
import '../services/product_service.dart';
import '../services/shop_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/product_card.dart';
import 'product_details_screen.dart';
import 'seller_conversation_screen.dart';

/// Fiche boutique publique — permet à un acheteur de visiter une boutique et
/// de voir tous ses produits. Signalé manquant dans le rapport de
/// modification du 28/10/2026 : le backend (`GET /api/shops/{slug}`,
/// `GET /api/products?shopId=...`) était déjà prêt, aucun écran ne l'utilisait.
///
/// Reached from [ProductDetailsScreen] (ligne nom de boutique, maintenant
/// cliquable) en passant [shopSlug].
class ShopDetailScreen extends StatefulWidget {
  final String shopSlug;

  const ShopDetailScreen({super.key, required this.shopSlug});

  @override
  State<ShopDetailScreen> createState() => _ShopDetailScreenState();
}

class _ShopDetailScreenState extends State<ShopDetailScreen> {
  late Future<(ShopModel, List<ProductModel>)> _loadFuture;
  bool _isContacting = false;
  bool _hoursExpanded = false;

  @override
  void initState() {
    super.initState();
    _loadFuture = _load();
  }

  Future<(ShopModel, List<ProductModel>)> _load() async {
    final shop = await ShopService().getBySlug(widget.shopSlug);
    final products = await ProductService().getShopProducts(shop.id);
    return (shop, products);
  }

  Future<void> _contactShop(ShopModel shop) async {
    setState(() => _isContacting = true);
    try {
      final conversationId = await ConversationService().startConversationWithShop(shop.id);
      if (!mounted) return;
      Navigator.of(context).push(MaterialPageRoute(
        builder: (_) => SellerConversationScreen(conversationId: conversationId, participantName: shop.name),
      ));
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(content: Text(e.message)));
    } finally {
      if (mounted) setState(() => _isContacting = false);
    }
  }

  void _openProductDetails(ProductModel product, List<ProductModel> catalog) {
    Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => ProductDetailsScreen(product: product, relatedCatalog: catalog)),
    );
  }

  Future<void> _addToCart(ProductModel product) async {
    try {
      await CartService().addProductQuickly(product);
      if (!mounted) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(const SnackBar(content: Text('Ajouté au panier.')));
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Scaffold(
      backgroundColor: colors.homeBackground,
      body: SafeArea(
        child: FutureBuilder<(ShopModel, List<ProductModel>)>(
          future: _loadFuture,
          builder: (context, snapshot) {
            if (snapshot.connectionState == ConnectionState.waiting) {
              return Column(children: [_buildHeader(colors, null), const Expanded(child: Center(child: CircularProgressIndicator()))]);
            }
            if (snapshot.hasError || !snapshot.hasData) {
              return Column(children: [_buildHeader(colors, null), Expanded(child: _buildErrorState(colors, snapshot.error))]);
            }
            final (shop, products) = snapshot.data!;
            return Column(
              children: [
                _buildHeader(colors, shop),
                Expanded(child: _buildContent(colors, shop, products)),
              ],
            );
          },
        ),
      ),
    );
  }

  Widget _buildHeader(AppColorScheme colors, ShopModel? shop) {
    return Container(
      color: colors.surface,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
      child: Row(
        children: [
          IconButton(icon: Icon(Icons.arrow_back_rounded, color: colors.textDark), onPressed: () => Navigator.of(context).pop()),
          Expanded(
            child: Text(
              shop?.name ?? 'Boutique',
              textAlign: TextAlign.center,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark),
            ),
          ),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildErrorState(AppColorScheme colors, Object? error) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.error_outline_rounded, size: 48, color: colors.error),
            const SizedBox(height: 12),
            Text(
              error is ApiException ? error.message : 'Impossible de charger cette boutique.',
              textAlign: TextAlign.center,
              style: TextStyle(color: colors.textMuted),
            ),
            const SizedBox(height: 12),
            TextButton(onPressed: () => setState(() => _loadFuture = _load()), child: const Text('Réessayer')),
          ],
        ),
      ),
    );
  }

  Widget _buildContent(AppColorScheme colors, ShopModel shop, List<ProductModel> products) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _buildShopCard(colors, shop),
        if (shop.openingHours.isNotEmpty) ...[
          const SizedBox(height: 12),
          _buildOpeningHoursCard(colors, shop),
        ],
        const SizedBox(height: 20),
        Text('Produits (${products.length})', style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
        const SizedBox(height: 12),
        if (products.isEmpty)
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 32),
            child: Center(
              child: Text('Aucun produit disponible pour le moment.', style: TextStyle(color: colors.textMuted)),
            ),
          )
        else
          MasonryGridView.count(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            crossAxisCount: 2,
            mainAxisSpacing: 16,
            crossAxisSpacing: 16,
            itemCount: products.length,
            itemBuilder: (context, index) {
              final product = products[index];
              return ProductCard(
                product: product,
                onTap: () => _openProductDetails(product, products),
                onAddToCart: () => _addToCart(product),
              );
            },
          ),
      ],
    );
  }

  Widget _buildShopCard(AppColorScheme colors, ShopModel shop) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              CircleAvatar(
                radius: 28,
                backgroundColor: colors.inputFill,
                backgroundImage: shop.logoUrl != null ? NetworkImage(shop.logoUrl!) : null,
                child: shop.logoUrl == null
                    ? Icon(Icons.storefront_outlined, color: colors.primary, size: 26)
                    : null,
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(shop.name, style: TextStyle(fontWeight: FontWeight.bold, fontSize: 17, color: colors.textDark)),
                    if (shop.location.isNotEmpty) ...[
                      const SizedBox(height: 2),
                      Row(
                        children: [
                          Icon(Icons.location_on_outlined, size: 13, color: colors.textMuted),
                          const SizedBox(width: 3),
                          Expanded(
                            child: Text(shop.location, style: TextStyle(fontSize: 12, color: colors.textMuted), overflow: TextOverflow.ellipsis),
                          ),
                        ],
                      ),
                    ],
                  ],
                ),
              ),
            ],
          ),
          if (shop.description != null && shop.description!.isNotEmpty) ...[
            const SizedBox(height: 12),
            Text(shop.description!, style: TextStyle(color: colors.textDark, height: 1.4)),
          ],
          const SizedBox(height: 14),
          SizedBox(
            width: double.infinity,
            child: OutlinedButton.icon(
              onPressed: _isContacting ? null : () => _contactShop(shop),
              style: OutlinedButton.styleFrom(
                foregroundColor: colors.primary,
                side: BorderSide(color: colors.primary),
                padding: const EdgeInsets.symmetric(vertical: 12),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
              ),
              icon: _isContacting
                  ? SizedBox(width: 14, height: 14, child: CircularProgressIndicator(strokeWidth: 2, color: colors.primary))
                  : const Icon(Icons.chat_bubble_outline_rounded, size: 18),
              label: const Text('Contacter la boutique'),
            ),
          ),
        ],
      ),
    );
  }

  /// Horaires d'ouverture — point 1 du rapport de modification du 28/10/2026 : un acheteur
  /// visitant une boutique n'avait aucun moyen de savoir si/quand elle est ouverte.
  Widget _buildOpeningHoursCard(AppColorScheme colors, ShopModel shop) {
    final now = DateTime.now();
    final todayMatches = shop.openingHours.where((h) => h.dayOfWeek == now.weekday);
    final today = todayMatches.isEmpty ? null : todayMatches.first;
    final statusText = _todayStatus(today, now);

    return Container(
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3))],
      ),
      child: Theme(
        data: Theme.of(context).copyWith(dividerColor: Colors.transparent),
        child: ExpansionTile(
          initiallyExpanded: _hoursExpanded,
          onExpansionChanged: (value) => setState(() => _hoursExpanded = value),
          tilePadding: const EdgeInsets.symmetric(horizontal: 16),
          leading: Icon(Icons.access_time_rounded, color: colors.primary, size: 20),
          title: Text('Horaires d\'ouverture', style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark, fontSize: 14)),
          subtitle: Text(statusText.label, style: TextStyle(color: statusText.isOpen ? colors.primary : colors.textMuted, fontSize: 12, fontWeight: FontWeight.w500)),
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
              child: Column(
                children: [
                  for (final entry in shop.openingHours)
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 4),
                      child: Row(
                        children: [
                          SizedBox(
                            width: 90,
                            child: Text(
                              entry.dayLabel,
                              style: TextStyle(
                                color: colors.textDark,
                                fontWeight: entry.dayOfWeek == now.weekday ? FontWeight.bold : FontWeight.normal,
                                fontSize: 13,
                              ),
                            ),
                          ),
                          Text(
                            entry.closed ? 'Fermé' : '${_formatTime(entry.opensAt)} - ${_formatTime(entry.closesAt)}',
                            style: TextStyle(color: entry.closed ? colors.textMuted : colors.textDark, fontSize: 13),
                          ),
                        ],
                      ),
                    ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  ({String label, bool isOpen}) _todayStatus(OpeningHourModel? today, DateTime now) {
    if (today == null) return (label: 'Horaires non renseignés', isOpen: false);
    if (today.closed) return (label: 'Fermé aujourd\'hui', isOpen: false);
    final opens = _parseMinutes(today.opensAt);
    final closes = _parseMinutes(today.closesAt);
    final nowMinutes = now.hour * 60 + now.minute;
    if (opens != null && closes != null && nowMinutes >= opens && nowMinutes < closes) {
      return (label: 'Ouvert jusqu\'à ${_formatTime(today.closesAt)}', isOpen: true);
    }
    return (label: 'Fermé · ouvre à ${_formatTime(today.opensAt)}', isOpen: false);
  }

  int? _parseMinutes(String? time) {
    if (time == null) return null;
    final parts = time.split(':');
    if (parts.length < 2) return null;
    return (int.tryParse(parts[0]) ?? 0) * 60 + (int.tryParse(parts[1]) ?? 0);
  }

  String _formatTime(String? time) {
    if (time == null) return '--:--';
    final parts = time.split(':');
    if (parts.length < 2) return time;
    return '${parts[0]}:${parts[1]}';
  }
}
