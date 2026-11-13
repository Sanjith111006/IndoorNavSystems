import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/errors/app_failure.dart';
import '../../../core/widgets/full_state_view.dart';
import '../../map/application/map_session_notifier.dart';
import '../application/destination_search_notifier.dart';
import '../domain/destination.dart';

class DestinationSearchScreen extends ConsumerStatefulWidget {
  const DestinationSearchScreen({required this.buildingId, super.key});

  final int buildingId;

  @override
  ConsumerState<DestinationSearchScreen> createState() =>
      _DestinationSearchScreenState();
}

class _DestinationSearchScreenState
    extends ConsumerState<DestinationSearchScreen> {
  final _queryController = TextEditingController();

  @override
  void dispose() {
    _queryController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final results = ref.watch(destinationSearchProvider(widget.buildingId));
    return Scaffold(
      appBar: AppBar(title: const Text('Choose a destination')),
      body: results.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, stackTrace) => FullStateView(
          icon: Icons.search_off_outlined,
          title: error is AppFailure ? error.title : 'Couldn\'t search',
          message: error is AppFailure
              ? error.message
              : 'Check your connection and try again.',
          primaryAction: () =>
              ref.invalidate(destinationSearchProvider(widget.buildingId)),
          primaryActionLabel: 'Retry',
        ),
        data: (data) => Column(
          children: <Widget>[
            Padding(
              padding: const EdgeInsets.all(16),
              child: TextField(
                controller: _queryController,
                autofocus: true,
                decoration: const InputDecoration(
                  labelText: 'Search destinations',
                  prefixIcon: Icon(Icons.search),
                  border: OutlineInputBorder(),
                ),
                onChanged: (query) => ref
                    .read(destinationSearchProvider(widget.buildingId).notifier)
                    .search(query: query, category: data.selectedCategory),
              ),
            ),
            SizedBox(
              height: 48,
              child: ListView(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: 16),
                children: <Widget>[
                  ChoiceChip(
                    label: const Text('All'),
                    selected: data.selectedCategory == null,
                    onSelected: (_) => _search(data.query, null),
                  ),
                  const SizedBox(width: 8),
                  ...data.categories.expand(
                    (category) => <Widget>[
                      ChoiceChip(
                        label: Text(category),
                        selected: data.selectedCategory == category,
                        onSelected: (_) => _search(
                          data.query,
                          data.selectedCategory == category ? null : category,
                        ),
                      ),
                      const SizedBox(width: 8),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 8),
            Expanded(
              child: data.destinations.isEmpty
                  ? const FullStateView(
                      icon: Icons.search_off_outlined,
                      title: 'No destinations found',
                      message: 'Check the spelling or browse categories.',
                    )
                  : ListView.separated(
                      itemCount: data.destinations.length,
                      separatorBuilder: (context, index) => const Divider(),
                      itemBuilder: (context, index) => _DestinationTile(
                        destination: data.destinations[index],
                        onSelected: _selectDestination,
                      ),
                    ),
            ),
          ],
        ),
      ),
    );
  }

  void _search(String query, String? category) {
    ref
        .read(destinationSearchProvider(widget.buildingId).notifier)
        .search(query: query, category: category);
  }

  void _selectDestination(Destination destination) {
    ref.read(mapSessionProvider.notifier).setDestination(destination);
    context.pop();
  }
}

class _DestinationTile extends StatelessWidget {
  const _DestinationTile({required this.destination, required this.onSelected});

  final Destination destination;
  final ValueChanged<Destination> onSelected;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: const Icon(Icons.place_outlined),
      title: Text(destination.name),
      subtitle: Text(
        '${destination.floorDisplayName} · ${destination.category}',
      ),
      trailing: const Icon(Icons.chevron_right),
      onTap: () => onSelected(destination),
    );
  }
}
