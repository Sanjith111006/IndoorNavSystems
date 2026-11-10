import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/app_failure.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/full_state_view.dart';
import '../application/building_list_notifier.dart';
import '../domain/building.dart';

class BuildingSelectionScreen extends ConsumerWidget {
  const BuildingSelectionScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final buildings = ref.watch(buildingListProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Choose a building')),
      body: buildings.when(
        loading: () => const _BuildingListSkeleton(),
        error: (error, stackTrace) => FullStateView(
          icon: Icons.cloud_off_outlined,
          title: error is AppFailure ? error.title : 'Couldn\'t load buildings',
          message: error is AppFailure
              ? error.message
              : 'Check your connection and try again.',
          primaryAction: () => ref.read(buildingListProvider.notifier).reload(),
          primaryActionLabel: 'Retry',
        ),
        data: (items) {
          if (items.isEmpty) {
            return const FullStateView(
              icon: Icons.location_city_outlined,
              title: 'No buildings available',
              message: 'There are no buildings to choose from right now.',
            );
          }
          return _BuildingList(items: items);
        },
      ),
    );
  }
}

class _BuildingList extends StatelessWidget {
  const _BuildingList({required this.items});

  final List<Building> items;

  @override
  Widget build(BuildContext context) {
    return ListView.separated(
      itemCount: items.length,
      separatorBuilder: (context, index) =>
          const Divider(height: 1, indent: AppSpacing.x4),
      itemBuilder: (context, index) {
        final building = items[index];
        final details = <String>[
          '${building.floorCount} floors',
          if (building.description case final description?) description,
        ];
        return Semantics(
          button: true,
          label: '${building.name}, ${building.floorCount} floors',
          child: ListTile(
            minVerticalPadding: AppSpacing.x2,
            leading: const Icon(Icons.location_city_outlined),
            title: Text(building.name),
            subtitle: Text(details.join(' · ')),
            trailing: const Icon(Icons.chevron_right),
            onTap: () {},
          ),
        );
      },
    );
  }
}

class _BuildingListSkeleton extends StatelessWidget {
  const _BuildingListSkeleton();

  @override
  Widget build(BuildContext context) {
    return ListView.builder(
      itemCount: 4,
      itemBuilder: (context, index) {
        return const ListTile(
          leading: CircleAvatar(backgroundColor: AppColors.surfaceVariant),
          title: _SkeletonLine(width: 180),
          subtitle: Padding(
            padding: EdgeInsets.only(top: AppSpacing.x2),
            child: _SkeletonLine(width: 120),
          ),
        );
      },
    );
  }
}

class _SkeletonLine extends StatelessWidget {
  const _SkeletonLine({required this.width});

  final double width;

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.centerLeft,
      child: Container(
        width: width,
        height: AppSpacing.x4,
        decoration: BoxDecoration(
          color: AppColors.surfaceVariant,
          borderRadius: BorderRadius.circular(AppRadius.small),
        ),
      ),
    );
  }
}
