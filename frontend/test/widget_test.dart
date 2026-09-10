import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:frontend/main.dart';
import 'package:frontend/screens/categories_screen.dart';
import 'package:frontend/screens/home_screen.dart';
import 'package:frontend/screens/profile_selection_screen.dart';
import 'package:frontend/theme/app_colors.dart';
import 'package:frontend/widgets/app_logo.dart';
import 'package:frontend/widgets/custom_bottom_navigation.dart';
import 'package:frontend/widgets/product_card.dart';
import 'package:frontend/widgets/role_card.dart';

void main() {
  setUp(() {
    // No profile saved by default — each test opts into a saved one if needed.
    SharedPreferences.setMockInitialValues({});
  });

  testWidgets('SplashScreen sends a first-time visitor to ProfileSelectionScreen',
      (WidgetTester tester) async {
    // Tall viewport since ProfileSelectionScreen will render after the delay.
    tester.view.physicalSize = const Size(1080, 2400);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(const MaplenouApp());

    final scaffold = tester.widget<Scaffold>(find.byType(Scaffold));
    expect(scaffold.backgroundColor, AppColors.primary);
    expect(find.byType(AppLogo), findsOneWidget);
    expect(find.byType(ProfileSelectionScreen), findsNothing);

    await tester.pump(const Duration(seconds: 10));
    await tester.pumpAndSettle();

    expect(find.byType(ProfileSelectionScreen), findsOneWidget);
  });

  testWidgets('SplashScreen sends a returning "Acheteur" straight to HomeScreen',
      (WidgetTester tester) async {
    SharedPreferences.setMockInitialValues({'selected_role_id': 'buyer'});

    // Tall viewport since HomeScreen will render after the delay.
    tester.view.physicalSize = const Size(1080, 2400);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(const MaplenouApp());
    await tester.pump(const Duration(seconds: 10));
    await tester.pumpAndSettle();

    expect(find.byType(HomeScreen), findsOneWidget);
  });

  testWidgets('ProfileSelectionScreen enables "Suivant" only once a role card is tapped',
      (WidgetTester tester) async {
    // Tall viewport so all three role cards render without needing a scroll.
    tester.view.physicalSize = const Size(1080, 2400);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(const MaterialApp(home: ProfileSelectionScreen()));

    expect(find.byType(RoleCard), findsNWidgets(3));

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

    await tester.pumpWidget(const MaterialApp(home: ProfileSelectionScreen()));

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
    // "Catégories" appears both as the section title and the nav bar label.
    expect(find.text('Catégories'), findsNWidgets(2));
    expect(find.text('Produits populaires'), findsOneWidget);
    expect(find.byType(ProductCard), findsNWidgets(4));
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
    expect(find.text('124 Résultats'), findsOneWidget);
    expect(find.byType(ProductCard), findsNWidgets(4));
  });
}
