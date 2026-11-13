import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/app_failure.dart';
import '../../../core/errors/result.dart';
import '../../../core/network/api_client.dart';
import '../domain/destination.dart';
import '../domain/start_location.dart';

final searchRepositoryProvider = Provider<SearchRepository>((ref) {
  return SearchRepository(ref.watch(apiClientProvider));
});

class SearchRepository {
  SearchRepository(this._apiClient);

  final ApiClient _apiClient;

  Future<Result<List<Destination>>> searchDestinations({
    required int buildingId,
    String? query,
    String? category,
  }) async {
    try {
      final data = await _apiClient.getObjectList(
        '/api/points-of-interest',
        queryParameters: <String, Object?>{
          'buildingId': buildingId,
          'limit': 50,
          if (query != null && query.isNotEmpty) 'query': query,
          if (category != null) 'category': category,
        },
      );
      return Success(data.map(Destination.fromJson).toList(growable: false));
    } on AppFailure catch (failure) {
      return Failure(failure);
    } on FormatException {
      return const Failure(InvalidResponseFailure());
    }
  }

  Future<Result<List<String>>> fetchCategories(int buildingId) async {
    try {
      final data = await _apiClient.getStringList(
        '/api/points-of-interest/categories',
        queryParameters: <String, Object?>{'buildingId': buildingId},
      );
      return Success(data);
    } on AppFailure catch (failure) {
      return Failure(failure);
    } on FormatException {
      return const Failure(InvalidResponseFailure());
    }
  }

  Future<Result<List<StartLocation>>> searchStartLocations({
    required int buildingId,
    String? query,
  }) async {
    try {
      final data = await _apiClient.getObjectList(
        '/api/buildings/$buildingId/nodes',
        queryParameters: <String, Object?>{
          'limit': 100,
          if (query != null && query.isNotEmpty) 'query': query,
        },
      );
      return Success(data.map(StartLocation.fromJson).toList(growable: false));
    } on AppFailure catch (failure) {
      return Failure(failure);
    } on FormatException {
      return const Failure(InvalidResponseFailure());
    }
  }
}
