import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:share_plus/share_plus.dart';

import '../models/reseller_model.dart';
import '../services/api_client.dart';
import '../services/reseller_service.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';

/// Programme partenaire revendeur : présentation et demande d'adhésion, puis (une fois approuvé) catalogue
/// et liens à partager, commissions et portefeuille avec demande de retrait.
class ResellerScreen extends StatefulWidget {
  const ResellerScreen({super.key});

  @override
  State<ResellerScreen> createState() => _ResellerScreenState();
}

class _ResellerScreenState extends State<ResellerScreen> {
  final _service = ResellerService();
  bool _loading = true;
  String? _error;
  ResellerProfileModel? _profile;
  late ResellerSettingsModel _settings;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final results = await Future.wait([_service.getSettings(), _service.getMyProfile()]);
      _settings = results[0] as ResellerSettingsModel;
      _profile = results[1] as ResellerProfileModel?;
    } on ApiException catch (e) {
      _error = e.message;
    } catch (_) {
      _error = 'Impossible de charger le programme partenaire.';
    }
    if (mounted) setState(() => _loading = false);
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    return Scaffold(
      backgroundColor: colors.homeBackground,
      appBar: AppBar(
        backgroundColor: colors.surface,
        foregroundColor: colors.textDark,
        elevation: 0,
        title: const Text('Programme partenaire', style: TextStyle(fontWeight: FontWeight.bold)),
      ),
      body: SafeArea(child: _buildBody(colors)),
    );
  }

  Widget _buildBody(AppColorScheme colors) {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            Text(_error!, textAlign: TextAlign.center, style: TextStyle(color: colors.error)),
            const SizedBox(height: 12),
            OutlinedButton(onPressed: _load, child: const Text('Réessayer')),
          ]),
        ),
      );
    }
    final profile = _profile;
    if (profile == null || profile.status == 'REJECTED') {
      return _ApplyView(settings: _settings, rejection: profile?.statusReason, onApplied: _load);
    }
    if (profile.status == 'PENDING') {
      return _MessageView(
        icon: Icons.hourglass_top_rounded,
        title: 'Demande en cours d\'examen',
        message: 'Nous étudions votre demande. Vous serez prévenu dès qu\'une décision sera prise.',
      );
    }
    if (profile.status == 'APPROVED') {
      return _PartnerHome(settings: _settings, service: _service);
    }
    return _MessageView(
      icon: Icons.pause_circle_outline_rounded,
      title: profile.status == 'SUSPENDED' ? 'Compte partenaire suspendu' : 'Compte partenaire désactivé',
      message: profile.statusReason ?? 'Contactez le support pour plus d\'informations.',
    );
  }
}

class _MessageView extends StatelessWidget {
  final IconData icon;
  final String title;
  final String message;

  const _MessageView({required this.icon, required this.title, required this.message});

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(28),
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          Icon(icon, size: 56, color: colors.primary),
          const SizedBox(height: 14),
          Text(title, style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: colors.textDark)),
          const SizedBox(height: 8),
          Text(message, textAlign: TextAlign.center, style: TextStyle(color: colors.textMuted)),
        ]),
      ),
    );
  }
}

/// Présentation du programme (conditions chiffrées lues depuis les paramètres en vigueur) et demande.
class _ApplyView extends StatefulWidget {
  final ResellerSettingsModel settings;
  final String? rejection;
  final Future<void> Function() onApplied;

  const _ApplyView({required this.settings, required this.rejection, required this.onApplied});

  @override
  State<_ApplyView> createState() => _ApplyViewState();
}

class _ApplyViewState extends State<_ApplyView> {
  final _service = ResellerService();
  bool _accepted = false;
  bool _sending = false;

  Future<void> _apply() async {
    setState(() => _sending = true);
    try {
      await _service.apply();
      await widget.onApplied();
    } on ApiException catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  String _pct(double rate) => '${(rate * 100).toStringAsFixed(rate * 100 % 1 == 0 ? 0 : 1)} %';

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final s = widget.settings;
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text('Gagnez de l\'argent en recommandant des produits',
            style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: colors.textDark)),
        const SizedBox(height: 8),
        Text(
          'Partagez votre lien personnel. Quand un client achète grâce à lui et que la commande est livrée, '
          'vous recevez ${_pct(s.shareRate)} de la commission de Maplenou sur chaque unité vendue.',
          style: TextStyle(color: colors.textMuted),
        ),
        const SizedBox(height: 16),
        _Bullet('Produits éligibles : prix final d\'au moins ${formatFcfa(s.minUnitPrice)}.'),
        _Bullet('Commission Maplenou de ${_pct(s.rateLow)} jusqu\'à ${formatFcfa(s.tierThreshold)}, '
            'puis ${_pct(s.rateHigh)} au-dessus.'),
        _Bullet('Votre gain est calculé par unité, sur le prix réellement payé après réduction.'),
        _Bullet('Le dernier clic valide l\'emporte, pendant ${s.attributionDays} jours.'),
        _Bullet('Votre gain est crédité uniquement après la livraison. Aucun gain sur vos propres achats.'),
        _Bullet('Retrait à partir de ${formatFcfa(s.minWithdrawal)} (frais de ${_pct(s.withdrawalFeeRate)} '
            'prélevés au paiement), par Mix by Yas ou Moov Africa.'),
        if (widget.rejection != null) ...[
          const SizedBox(height: 12),
          Text('Votre précédente demande a été refusée : ${widget.rejection}', style: TextStyle(color: colors.error)),
        ],
        const SizedBox(height: 12),
        CheckboxListTile(
          contentPadding: EdgeInsets.zero,
          controlAffinity: ListTileControlAffinity.leading,
          value: _accepted,
          onChanged: (v) => setState(() => _accepted = v ?? false),
          title: Text('J\'ai lu et j\'accepte les conditions du programme partenaire.',
              style: TextStyle(fontSize: 13, color: colors.textDark)),
        ),
        const SizedBox(height: 8),
        FilledButton(
          onPressed: (_accepted && !_sending && s.programActive) ? _apply : null,
          child: _sending
              ? const SizedBox(height: 18, width: 18, child: CircularProgressIndicator(strokeWidth: 2))
              : Text(s.programActive ? 'Envoyer ma demande' : 'Programme temporairement fermé'),
        ),
      ],
    );
  }
}

class _Bullet extends StatelessWidget {
  final String text;
  const _Bullet(this.text);

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Icon(Icons.check_circle_outline_rounded, size: 18, color: colors.primary),
        const SizedBox(width: 8),
        Expanded(child: Text(text, style: TextStyle(color: colors.textDark, fontSize: 13))),
      ]),
    );
  }
}

/// Espace du partenaire approuvé : trois onglets.
class _PartnerHome extends StatelessWidget {
  final ResellerSettingsModel settings;
  final ResellerService service;

  const _PartnerHome({required this.settings, required this.service});

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 3,
      child: Column(children: [
        const TabBar(tabs: [Tab(text: 'Produits'), Tab(text: 'Gains'), Tab(text: 'Portefeuille')]),
        Expanded(
          child: TabBarView(children: [
            _CatalogTab(service: service),
            _CommissionsTab(service: service),
            _WalletTab(service: service, settings: settings),
          ]),
        ),
      ]),
    );
  }
}

class _CatalogTab extends StatefulWidget {
  final ResellerService service;
  const _CatalogTab({required this.service});

  @override
  State<_CatalogTab> createState() => _CatalogTabState();
}

class _CatalogTabState extends State<_CatalogTab> with AutomaticKeepAliveClientMixin {
  late Future<List<ResellerCatalogItem>> _future = widget.service.getCatalog();

  @override
  bool get wantKeepAlive => true;

  Future<void> _share(ResellerCatalogItem item) async {
    try {
      final url = await widget.service.createLink(item.productId);
      await Share.share('${item.name} — $url');
    } on ApiException catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  Future<void> _copy(ResellerCatalogItem item) async {
    try {
      final url = await widget.service.createLink(item.productId);
      await Clipboard.setData(ClipboardData(text: url));
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Lien copié')));
    } on ApiException catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);
    final colors = context.colors;
    return FutureBuilder<List<ResellerCatalogItem>>(
      future: _future,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError) {
          return Center(child: Text('${snapshot.error}', style: TextStyle(color: colors.error)));
        }
        final items = snapshot.data ?? const [];
        if (items.isEmpty) return Center(child: Text('Aucun produit éligible pour le moment.', style: TextStyle(color: colors.textMuted)));
        return RefreshIndicator(
          onRefresh: () async => setState(() => _future = widget.service.getCatalog()),
          child: ListView.separated(
            padding: const EdgeInsets.all(16),
            itemCount: items.length,
            separatorBuilder: (_, _) => const SizedBox(height: 10),
            itemBuilder: (context, i) {
              final item = items[i];
              return Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(color: colors.surface, borderRadius: BorderRadius.circular(14)),
                child: Row(children: [
                  ClipRRect(
                    borderRadius: BorderRadius.circular(10),
                    child: item.imageUrl == null
                        ? Container(width: 56, height: 56, color: colors.inputFill)
                        : Image.network(item.imageUrl!, width: 56, height: 56, fit: BoxFit.cover,
                            errorBuilder: (_, _, _) => Container(width: 56, height: 56, color: colors.inputFill)),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      Text(item.name, maxLines: 2, overflow: TextOverflow.ellipsis,
                          style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
                      Text('${formatFcfa(item.unitPrice)} · ${item.shopName}',
                          style: TextStyle(fontSize: 12, color: colors.textMuted)),
                      Text('Vous gagnez ${formatFcfa(item.partnerShare)} par unité',
                          style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: colors.primary)),
                    ]),
                  ),
                  IconButton(icon: const Icon(Icons.copy_rounded), tooltip: 'Copier le lien', onPressed: () => _copy(item)),
                  IconButton(icon: const Icon(Icons.share_rounded), tooltip: 'Partager', onPressed: () => _share(item)),
                ]),
              );
            },
          ),
        );
      },
    );
  }
}

class _CommissionsTab extends StatelessWidget {
  final ResellerService service;
  const _CommissionsTab({required this.service});

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    return FutureBuilder<List<ResellerCommissionModel>>(
      future: service.getCommissions(),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError) {
          return Center(child: Text('${snapshot.error}', style: TextStyle(color: colors.error)));
        }
        final items = snapshot.data ?? const [];
        if (items.isEmpty) {
          return Center(child: Text('Aucune vente pour le moment.', style: TextStyle(color: colors.textMuted)));
        }
        return ListView.separated(
          padding: const EdgeInsets.all(16),
          itemCount: items.length,
          separatorBuilder: (_, _) => const SizedBox(height: 8),
          itemBuilder: (context, i) {
            final c = items[i];
            return ListTile(
              tileColor: colors.surface,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              title: Text(c.productName, maxLines: 1, overflow: TextOverflow.ellipsis),
              subtitle: Text('${c.statusLabel} · ${formatRelativeDate(c.createdAt)}'),
              trailing: Text('+${formatFcfa(c.partnerShare)}',
                  style: TextStyle(fontWeight: FontWeight.bold, color: c.status == 'CANCELLED' ? colors.textMuted : colors.primary)),
            );
          },
        );
      },
    );
  }
}

class _WalletTab extends StatefulWidget {
  final ResellerService service;
  final ResellerSettingsModel settings;
  const _WalletTab({required this.service, required this.settings});

  @override
  State<_WalletTab> createState() => _WalletTabState();
}

class _WalletTabState extends State<_WalletTab> {
  late Future<List<dynamic>> _future = _fetch();

  Future<List<dynamic>> _fetch() => Future.wait([widget.service.getWallet(), widget.service.getWithdrawals()]);

  void _reload() => setState(() => _future = _fetch());

  Future<void> _withdraw(ResellerWalletModel wallet) async {
    final amountCtrl = TextEditingController();
    final phoneCtrl = TextEditingController();
    var method = 'MIX_BY_YAS';
    // Une clé par ouverture du formulaire : un renvoi de cette même demande ne crée jamais deux retraits.
    final idempotencyKey = 'wd-${DateTime.now().microsecondsSinceEpoch}-${UniqueKey().hashCode}';
    final submitted = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => StatefulBuilder(
        builder: (context, setLocal) => AlertDialog(
          title: const Text('Demander un retrait'),
          content: Column(mainAxisSize: MainAxisSize.min, children: [
            Text('Disponible : ${formatFcfa(wallet.available)} · minimum ${formatFcfa(widget.settings.minWithdrawal)}'),
            TextField(controller: amountCtrl, keyboardType: TextInputType.number, decoration: const InputDecoration(labelText: 'Montant (FCFA)')),
            DropdownButtonFormField<String>(
              initialValue: method,
              items: const [
                DropdownMenuItem(value: 'MIX_BY_YAS', child: Text('Mix by Yas')),
                DropdownMenuItem(value: 'MOOV_AFRICA', child: Text('Moov Africa')),
              ],
              onChanged: (v) => setLocal(() => method = v ?? method),
              decoration: const InputDecoration(labelText: 'Moyen de paiement'),
            ),
            TextField(controller: phoneCtrl, keyboardType: TextInputType.phone, decoration: const InputDecoration(labelText: 'Numéro de paiement')),
          ]),
          actions: [
            TextButton(onPressed: () => Navigator.of(dialogContext).pop(false), child: const Text('Annuler')),
            FilledButton(onPressed: () => Navigator.of(dialogContext).pop(true), child: const Text('Envoyer')),
          ],
        ),
      ),
    );
    if (submitted != true) return;
    final amount = int.tryParse(amountCtrl.text.trim());
    if (amount == null) return;
    try {
      await widget.service.requestWithdrawal(
          amount: amount, method: method, paymentPhone: phoneCtrl.text.trim(), idempotencyKey: idempotencyKey);
      _reload();
    } on ApiException catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    return FutureBuilder<List<dynamic>>(
      future: _future,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError) {
          return Center(child: Text('${snapshot.error}', style: TextStyle(color: colors.error)));
        }
        final wallet = snapshot.data![0] as ResellerWalletModel;
        final withdrawals = snapshot.data![1] as List<ResellerWithdrawalModel>;
        return RefreshIndicator(
          onRefresh: () async => _reload(),
          child: ListView(padding: const EdgeInsets.all(16), children: [
            Container(
              padding: const EdgeInsets.all(18),
              decoration: BoxDecoration(color: colors.primary, borderRadius: BorderRadius.circular(18)),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                const Text('Solde disponible', style: TextStyle(color: Colors.white70)),
                Text(formatFcfa(wallet.available),
                    style: const TextStyle(color: Colors.white, fontSize: 28, fontWeight: FontWeight.bold)),
                const SizedBox(height: 6),
                Text('En attente de livraison : ${formatFcfa(wallet.pending)}  ·  Retrait en cours : ${formatFcfa(wallet.reserved)}',
                    style: const TextStyle(color: Colors.white70, fontSize: 12)),
              ]),
            ),
            const SizedBox(height: 12),
            Text('Total gagné ${formatFcfa(wallet.totalEarned)} · Total retiré ${formatFcfa(wallet.totalPaid)} · '
                '${wallet.clicks} clics', style: TextStyle(fontSize: 12, color: colors.textMuted)),
            const SizedBox(height: 12),
            FilledButton.icon(
              onPressed: wallet.available >= widget.settings.minWithdrawal ? () => _withdraw(wallet) : null,
              icon: const Icon(Icons.account_balance_wallet_outlined),
              label: Text('Retirer (minimum ${formatFcfa(widget.settings.minWithdrawal)})'),
            ),
            const SizedBox(height: 20),
            Text('Mes retraits', style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
            const SizedBox(height: 8),
            if (withdrawals.isEmpty) Text('Aucun retrait pour le moment.', style: TextStyle(color: colors.textMuted)),
            for (final w in withdrawals)
              ListTile(
                tileColor: colors.surface,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                title: Text('${formatFcfa(w.amount)} · ${w.methodLabel}'),
                subtitle: Text('${w.statusLabel} · net ${formatFcfa(w.netAmount)} (frais ${formatFcfa(w.feeAmount)})'),
              ),
          ]),
        );
      },
    );
  }
}
