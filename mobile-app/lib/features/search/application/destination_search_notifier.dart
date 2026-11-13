import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/app_failure.dart';
import '../../../core/errors/result.dart';
import '../data/search_repository.dart';
import '../domain/destination.dart';

final destinationSearchProvider =
    AsyncNotifierProvider.family<
      DestinationSearchNotifier,
      DestinationSearchState,
      int
    >(DestinationSearchNotifier.new);

class DestinationSearchNotifier extends AsyncNotifier<DestinationSearchState> {
  DestinationSearchNotifier(this._buildingId);

  final int _buildingId;

  @override
  Future<DestinationSearchState> build() async {
    final repository = ref.read(searchRepositoryProvider);
    final categories = await repository.fetchCategories(_buildingId);
    final destinations = await repository.searchDestinations(
      buildingId: _buildingId,
    );
    return _stateFrom(categories, destinations, query: '', category: null);
  }

  Future<void> search({required String query, String? category}) async {
    final current = switch (state) {
      AsyncData<DestinationSearchState>(value: final value) => value,
      _ => null,
    };
    if (current == null) {
      return;
    }
    state = const AsyncLoading<DestinationSearchState>();
    state = await AsyncValue.guard(() async {
      final destinations = await ref
          .read(searchRepositoryProvider)
          .searchDestinations(
            buildingId: _buildingId,
            query: query.trim(),
            category: category,
          );
      return _stateFrom(
        Success(current.categories),
        destinations,
        query: query,
        category: category,
      );
    });
  }

  DestinationSearchState _stateFrom(
    Result<List<String>> categories,
    Result<List<Destination>> destinations, {
    required String query,
    required String? category,
  }) {
    return switch ((categories, destinations)) {
      (
        Success<List<String>>(value: final categoryValues),
        Success<List<Destination>>(value: final destinationValues),
      ) =>
        DestinationSearchState(
          categories: categoryValues,
          destinations: destinationValues,
          query: query,
          selectedCategory: category,
        ),
      (Failure<List<String>>(error: final error), _) => throw _asFailure(error),
      (_, Failure<List<Destination>>(error: final error)) => throw _asFailure(
        error,
      ),
    };
  }

  AppFailure _asFailure(Exception error) {
    return error is AppFailure ? error : const InvalidResponseFailure();
  }
}

class DestinationSearchState {
  const DestinationSearchState({
    required this.categories,
    required this.destinations,
    required this.query,
    required this.selectedCategory,
  });

  final List<String> categories;
  final List<Destination> destinations;
  final String query;
  final String? selectedCategory;
}
