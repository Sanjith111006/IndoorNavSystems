import 'package:flutter/material.dart';

abstract final class AppColors {
  static const primary = Color(0xFF0B5D6B);
  static const onPrimary = Color(0xFFFFFFFF);
  static const primaryContainer = Color(0xFFCFE8EC);
  static const onPrimaryContainer = Color(0xFF052E35);
  static const background = Color(0xFFF7F5F0);
  static const surface = Color(0xFFFFFFFF);
  static const surfaceVariant = Color(0xFFECE8DF);
  static const onSurface = Color(0xFF1C1B17);
  static const onSurfaceSecondary = Color(0xFF55524A);
  static const outline = Color(0xFF7A7568);
  static const outlineSubtle = Color(0xFFD9D4C8);
  static const error = Color(0xFFB3261E);
}

abstract final class AppSpacing {
  static const x2 = 8.0;
  static const x4 = 16.0;
  static const x6 = 24.0;
}

abstract final class AppRadius {
  static const small = 8.0;
  static const medium = 12.0;
  static const full = 100.0;
}

abstract final class AppTheme {
  static ThemeData light() {
    const colorScheme = ColorScheme.light(
      primary: AppColors.primary,
      onPrimary: AppColors.onPrimary,
      primaryContainer: AppColors.primaryContainer,
      onPrimaryContainer: AppColors.onPrimaryContainer,
      surface: AppColors.surface,
      onSurface: AppColors.onSurface,
      error: AppColors.error,
    );

    return ThemeData(
      useMaterial3: true,
      colorScheme: colorScheme,
      scaffoldBackgroundColor: AppColors.background,
      appBarTheme: const AppBarTheme(
        backgroundColor: AppColors.surface,
        foregroundColor: AppColors.onSurface,
        surfaceTintColor: Colors.transparent,
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          minimumSize: const Size.fromHeight(48),
          shape: const StadiumBorder(),
        ),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          minimumSize: const Size.fromHeight(48),
          shape: const StadiumBorder(),
        ),
      ),
      listTileTheme: const ListTileThemeData(
        minVerticalPadding: AppSpacing.x2,
        contentPadding: EdgeInsets.symmetric(horizontal: AppSpacing.x4),
      ),
      dividerColor: AppColors.outlineSubtle,
    );
  }
}
