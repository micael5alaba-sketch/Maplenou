import 'package:flutter/material.dart';

import '../theme/app_color_scheme.dart';

class _Faq {
  final String question;
  final String answer;
  const _Faq(this.question, this.answer);
}

const _faqs = [
  _Faq(
    'Comment passer une commande ?',
    'Ajoutez des produits au panier, allez dans "Panier", choisissez une adresse de livraison et un '
        'pays/moyen de paiement, puis confirmez. Vous êtes redirigé vers une page de paiement sécurisée.',
  ),
  _Faq(
    'Quels moyens de paiement sont acceptés ?',
    'Mobile Money (T-Money, Flooz) et carte bancaire, via FedaPay. Le pays se choisit à l\'étape paiement.',
  ),
  _Faq(
    'Combien de temps prend la livraison ?',
    'Le délai estimé dépend de la distance jusqu\'à votre adresse et s\'affiche avant de valider la commande. '
        'Le suivi en temps réel est disponible depuis "Mes commandes".',
  ),
  _Faq(
    'Puis-je retourner un produit ?',
    'Oui, sous 30 jours après la livraison, depuis le détail de la commande concernée.',
  ),
  _Faq(
    'Comment devenir vendeur sur Maplenou ?',
    'Depuis "Paramètres du compte" > "Devenir vendeur", créez votre boutique. Une fois votre profil '
        'approuvé par l\'équipe Maplenou, vous pouvez publier vos produits.',
  ),
  _Faq(
    'Ma question n\'est pas ici ?',
    'Utilisez "Nous contacter" dans cette même section pour écrire directement au support.',
  ),
];

/// "Centre d'aide" — reached from [ProfileScreen]'s support section.
class HelpCenterScreen extends StatelessWidget {
  const HelpCenterScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Scaffold(
      backgroundColor: colors.homeBackground,
      body: SafeArea(
        child: Column(
          children: [
            Container(
              color: colors.surface,
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
              child: Row(
                children: [
                  IconButton(icon: Icon(Icons.arrow_back_rounded, color: colors.textDark), onPressed: () => Navigator.of(context).pop()),
                  Expanded(child: Text("Centre d'aide", textAlign: TextAlign.center, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark))),
                  const SizedBox(width: 48),
                ],
              ),
            ),
            Expanded(
              child: ListView.separated(
                padding: const EdgeInsets.all(16),
                itemCount: _faqs.length,
                separatorBuilder: (_, _) => const SizedBox(height: 8),
                itemBuilder: (context, index) {
                  final faq = _faqs[index];
                  return Theme(
                    data: Theme.of(context).copyWith(dividerColor: Colors.transparent),
                    child: Container(
                      decoration: BoxDecoration(
                        color: colors.surface,
                        borderRadius: BorderRadius.circular(14),
                        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 8, offset: const Offset(0, 3))],
                      ),
                      child: ExpansionTile(
                        title: Text(faq.question, style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
                        childrenPadding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
                        expandedCrossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(faq.answer, style: TextStyle(color: colors.textMuted, height: 1.4)),
                        ],
                      ),
                    ),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}
