import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/result.dart';
import '../data/building_repository.dart';
import '../domain/building.dart';

final buildingListProvider =
    AsyncNotifierProvider<BuildingListNotifier, List<Building>>(
      BuildingListNotifier.new,
    );

class BuildingListNotifier extends AsyncNotifier<List<Building>> {
  @override
  Future<List<Building>> build() {
    return _load();
  }

  Future<void> reload() async {
    state = const AsyncLoading<List<Building>>();
    state = await AsyncValue.guard(_load);
  }

  Future<List<Building>> _load() async {
    final result = await ref.read(buildingRepositoryProvider).fetchBuildings();
    return switch (result) {
      Success<List<Building>>(:final value) => value,
      Failure<List<Building>>(:final error) => throw error,
    };
  }
}
