import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:frontend/main.dart';
import 'package:frontend/models/user_model.dart';
import 'package:frontend/screens/categories_screen.dart';
import 'package:frontend/screens/home_screen.dart';
import 'package:frontend/screens/login_screen.dart';
import 'package:frontend/screens/profile_selection_screen.dart';
import 'package:frontend/theme/app_colors.dart';
import 'package:frontend/widgets/app_logo.dart';
import 'package:frontend/widgets/custom_bottom_navigation.dart';
import 'package:frontend/widgets/role_card.dart';

/// Compte fictif avec profil vendeur approuvé, pour exercer les deux cartes
/// (Acheteur + Vendeur) de [ProfileSelectionScreen] sans appel réseau.
const _approvedSellerUser = UserModel(
  id: 'u1',
  fullName: 'Ama Konan',
  phoneNumber: '+22890000000',
  phoneVerified: true,
  sellerStatus: SellerAccountStatus.approved,
);

void main() {
  setUp(() {
    // Aucun token stocké par défaut — chaque test injecte le sien si besoin.
    SharedPreferences.setMockInitialValues({});
  });

  testWidgets('SplashScreen sends a visitor with no stored session to LoginScreen',
      (WidgetTester tester) async {
    // Tall viewport since LoginScreen will render after the delay.
    tester.view.physicalSize = const Size(1080, 2400);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(const MaplenouApp());

    final scaffold = tester.widget<Scaffold>(find.byType(Scaffold));
    expect(scaffold.backgroundColor, AppColors.primary);
    expect(find.byType(AppLogo), findsOneWidget);
    expect(find.byType(LoginScreen), findsNothing);

    await tester.pump(const Duration(seconds: 10));
    await tester.pumpAndSettle();

    expect(find.byType(LoginScreen), findsOneWidget);
  });

  // NOTE : le cas "session valide -> ouvre directement l'interface du
  // compte" (AppRouter.resolveHomeScreen après un vrai GET /api/users/me)
  // n'est pas couvert ici : il faudrait une injection de dépendance pour
  // simuler UserService/ApiClient sans réseau, ce qui n'existe pas encore
  // dans ce projet de tests. À ajouter quand ce besoin se généralise.

  testWidgets('ProfileSelectionScreen enables "Suivant" only once a role card is tapped',
      (WidgetTester tester) async {
    // Tall viewport so both role cards render without needing a scroll.
    tester.view.physicalSize = const Size(1080, 2400);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(const MaterialApp(home: ProfileSelectionScreen(user: _approvedSellerUser)));

    // Livreur n'apparaît jamais ici (compte dédié) ; Vendeur seulement si approuvé.
    expect(find.byType(RoleCard), findsNWidgets(2));

    ElevatedButton nextButton() =>
        tester.widget<ElevatedButton>(find.widgetWithText(ElevatedButton, 'Suivant'));

    expect(nextButton().onPressed, isNull);

    await tester.tap(find.text('Acheteur'));
    await tester.pumpAndSettle();

    expect(nextButton().onPressed, isNotNull);
  });

  testWidgets('Selecting "Acheteur" and tapping "Suivant" persists the choice and opens HomeScreen',
      (WidgetTester tester) async {
    tester.view.physicalSize = const Size(1080, 2400);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(const MaterialApp(home: ProfileSelectionScreen(user: _approvedSellerUser)));

    await tester.tap(find.text('Acheteur'));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(ElevatedButton, 'Suivant'));
    await tester.pumpAndSettle();

    expect(find.byType(HomeScreen), findsOneWidget);

    final prefs = await SharedPreferences.getInstance();
    expect(prefs.getString('selected_role_id'), 'buyer');
  });

  testWidgets('HomeScreen renders the header, banner, categories and product grid without errors',
      (WidgetTester tester) async {
    // A common, narrow phone width (360dp) — an oversized viewport here
    // previously hid real overflow bugs in the product grid.
    tester.view.physicalSize = const Size(360, 800);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(const MaterialApp(home: HomeScreen()));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    // The brand name is now baked into the logo SVG, not a separate Text.
    expect(find.byType(AppLogo), findsOneWidget);
    // "Catégories" (le libellé de la barre de navigation, toujours statique)
    // apparaît au moins une fois — le titre de la section catégories, lui,
    // vient de CategoryService et ne s'affiche pas sans backend en test.
    expect(find.text('Catégories'), findsAtLeastNWidgets(1));
    // Pas d'assertion sur "Produits populaires"/le nombre de ProductCard :
    // HomeScreen appelle le vrai ProductService (voir CLAUDE.md), donc sans
    // backend accessible en test, la section reste vide plutôt que remplie
    // de données mockées — seule l'absence de crash est vérifiée ici.
  });

  testWidgets('Tapping "Catégories" in the bottom nav opens CategoriesScreen',
      (WidgetTester tester) async {
    tester.view.physicalSize = const Size(360, 800);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(const MaterialApp(home: HomeScreen()));
    await tester.tap(find.descendant(
      of: find.byType(CustomBottomNavigation),
      matching: find.text('Catégories'),
    ));
    await tester.pumpAndSettle();

    expect(find.byType(CategoriesScreen), findsOneWidget);
  });

  testWidgets('CategoriesScreen renders filters, results bar and the results grid without errors',
      (WidgetTester tester) async {
    tester.view.physicalSize = const Size(360, 800);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(const MaterialApp(home: CategoriesScreen()));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.text('Tout voir'), findsOneWidget);
    // Idem HomeScreen : pas d'assertion sur le nombre de résultats/ProductCard,
    // qui dépendent du vrai ProductService (pas de backend en test).
  });
}
