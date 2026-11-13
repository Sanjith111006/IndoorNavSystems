import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/app_failure.dart';
import '../../../core/errors/result.dart';
import '../../../core/network/api_client.dart';
import '../domain/floor.dart';

final floorRepositoryProvider = Provider<FloorRepository>((ref) {
  return FloorRepository(ref.watch(apiClientProvider));
});

class FloorRepository {
  FloorRepository(this._apiClient);

  final ApiClient _apiClient;

  Future<Result<List<Floor>>> fetchFloors(int buildingId) async {
    try {
      final data = await _apiClient.getObjectList(
        '/api/buildings/$buildingId/floors',
      );
      return Success(data.map(Floor.fromJson).toList(growable: false));
    } on AppFailure catch (failure) {
      return Failure(failure);
    } on FormatException {
      return const Failure(InvalidResponseFailure());
    }
  }
}
