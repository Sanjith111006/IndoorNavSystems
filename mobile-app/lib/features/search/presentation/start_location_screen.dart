import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/errors/app_failure.dart';
import '../../../core/widgets/full_state_view.dart';
import '../../map/application/map_session_notifier.dart';
import '../application/start_location_notifier.dart';
import '../domain/start_location.dart';

class StartLocationScreen extends ConsumerStatefulWidget {
  const StartLocationScreen({required this.buildingId, super.key});

  final int buildingId;

  @override
  ConsumerState<StartLocationScreen> createState() =>
      _StartLocationScreenState();
}

class _StartLocationScreenState extends ConsumerState<StartLocationScreen> {
  @override
  Widget build(BuildContext context) {
    final starts = ref.watch(startLocationProvider(widget.buildingId));
    return Scaffold(
      appBar: AppBar(title: const Text('Choose a start location')),
      body: starts.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, stackTrace) => FullStateView(
          icon: Icons.search_off_outlined,
          title: error is AppFailure ? error.title : 'Couldn\'t load starts',
          message: error is AppFailure
              ? error.message
              : 'Check your connection and try again.',
          primaryAction: () =>
              ref.invalidate(startLocationProvider(widget.buildingId)),
          primaryActionLabel: 'Retry',
        ),
        data: (items) => Column(
          children: <Widget>[
            Padding(
              padding: const EdgeInsets.all(16),
              child: TextField(
                decoration: const InputDecoration(
                  labelText: 'Search start locations',
                  prefixIcon: Icon(Icons.search),
                  border: OutlineInputBorder(),
                ),
                onChanged: (query) => ref
                    .read(startLocationProvider(widget.buildingId).notifier)
                    .search(query),
              ),
            ),
            Expanded(
              child: items.isEmpty
                  ? const FullStateView(
                      icon: Icons.location_off_outlined,
                      title: 'No start locations found',
                      message: 'Try another search term.',
                    )
                  : ListView.separated(
                      itemCount: items.length,
                      separatorBuilder: (context, index) => const Divider(),
                      itemBuilder: (context, index) => _StartTile(
                        start: items[index],
                        onSelected: _selectStart,
                      ),
                    ),
            ),
          ],
        ),
      ),
    );
  }

  void _selectStart(StartLocation start) {
    ref.read(mapSessionProvider.notifier).setStart(start);
    context.pop();
  }
}

class _StartTile extends StatelessWidget {
  const _StartTile({required this.start, required this.onSelected});

  final StartLocation start;
  final ValueChanged<StartLocation> onSelected;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: const Icon(Icons.my_location_outlined),
      title: Text(start.label),
      subtitle: Text(start.floorDisplayName),
      trailing: const Icon(Icons.chevron_right),
      onTap: () => onSelected(start),
    );
  }
}
