import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';

/// Which of the 5 Maplenou logo assets to render.
///
/// - `stacked*`: pictogram above the "Maplenou" wordmark (splash screen).
/// - `horizontal*`: pictogram beside the wordmark (app bars/headers).
/// - `iconColor`: pictogram alone, no wordmark (compact spaces).
///
/// White variants belong on dark or photo backgrounds; color variants on
/// light backgrounds — using the wrong one makes the logo unreadable.
enum AppLogoVariant {
  stackedWhite('assets/images/logo_stacked_white.svg'),
  stackedColor('assets/images/logo_stacked_color.svg'),
  horizontalWhite('assets/images/logo_horizontal_white.svg'),
  horizontalColor('assets/images/logo_horizontal_color.svg'),
  iconColor('assets/images/logo_icon_color.svg');

  final String assetPath;

  const AppLogoVariant(this.assetPath);
}

/// Maplenou brand logo. Pick the [AppLogoVariant] that matches both the
/// layout you need (stacked / horizontal / icon-only) and the background
/// it sits on (white on dark, color on light).
///
/// [size] sets the logo's height; width scales automatically to match each
/// asset's own aspect ratio (the horizontal lockups are notably wider than
/// tall), so bumping [size] up never forces extra, unwanted vertical space.
class AppLogo extends StatelessWidget {
  final AppLogoVariant variant;
  final double size;

  const AppLogo({
    super.key,
    this.variant = AppLogoVariant.stackedColor,
    this.size = 110,
  });

  @override
  Widget build(BuildContext context) {
    return SvgPicture.asset(
      variant.assetPath,
      height: size,
    );
  }
}
