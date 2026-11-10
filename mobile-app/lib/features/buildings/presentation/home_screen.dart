import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/theme/app_theme.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 600),
            child: Padding(
              padding: const EdgeInsets.all(AppSpacing.x4),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: <Widget>[
                  const Spacer(),
                  Text(
                    'Indoor navigation',
                    style: Theme.of(context).textTheme.headlineLarge,
                  ),
                  const SizedBox(height: AppSpacing.x2),
                  Text(
                    'Choose a building, then select a destination and route.',
                    style: Theme.of(context).textTheme.bodyLarge,
                  ),
                  const SizedBox(height: AppSpacing.x6),
                  Semantics(
                    label: 'Only show step-free routes',
                    child: const SwitchListTile(
                      contentPadding: EdgeInsets.zero,
                      title: Text('Only show step-free routes'),
                      subtitle: Text('Use step-free routes by default.'),
                      value: false,
                      onChanged: null,
                    ),
                  ),
                  const Spacer(),
                  FilledButton(
                    onPressed: () => context.go('/buildings'),
                    child: const Text('Choose a building'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
