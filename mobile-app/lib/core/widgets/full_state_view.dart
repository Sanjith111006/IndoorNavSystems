import 'package:flutter/material.dart';

import '../theme/app_theme.dart';

class FullStateView extends StatelessWidget {
  const FullStateView({
    required this.icon,
    required this.title,
    required this.message,
    this.primaryAction,
    this.primaryActionLabel,
    super.key,
  });

  final IconData icon;
  final String title;
  final String message;
  final VoidCallback? primaryAction;
  final String? primaryActionLabel;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 600),
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.x6),
          child: Semantics(
            liveRegion: true,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: <Widget>[
                Icon(icon, size: 48, color: AppColors.onSurfaceSecondary),
                const SizedBox(height: AppSpacing.x4),
                Text(
                  title,
                  style: Theme.of(context).textTheme.headlineSmall,
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: AppSpacing.x2),
                Text(message, textAlign: TextAlign.center),
                if (primaryAction != null &&
                    primaryActionLabel != null) ...<Widget>[
                  const SizedBox(height: AppSpacing.x4),
                  FilledButton(
                    onPressed: primaryAction,
                    child: Text(primaryActionLabel!),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}
