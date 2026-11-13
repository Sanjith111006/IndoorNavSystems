import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/app_failure.dart';
import '../../../core/errors/result.dart';
import '../data/search_repository.dart';
import '../domain/start_location.dart';

final startLocationProvider =
    AsyncNotifierProvider.family<
      StartLocationNotifier,
      List<StartLocation>,
      int
    >(StartLocationNotifier.new);

class StartLocationNotifier extends AsyncNotifier<List<StartLocation>> {
  StartLocationNotifier(this._buildingId);

  final int _buildingId;

  @override
  Future<List<StartLocation>> build() {
    return _load();
  }

  Future<void> search(String query) async {
    state = const AsyncLoading<List<StartLocation>>();
    state = await AsyncValue.guard(() => _load(query));
  }

  Future<List<StartLocation>> _load([String? query]) async {
    final result = await ref
        .read(searchRepositoryProvider)
        .searchStartLocations(buildingId: _buildingId, query: query?.trim());
    return switch (result) {
      Success<List<StartLocation>>(value: final starts) => starts,
      Failure<List<StartLocation>>(error: final error) => throw _asFailure(
        error,
      ),
    };
  }

  AppFailure _asFailure(Exception error) {
    return error is AppFailure ? error : const InvalidResponseFailure();
  }
}
