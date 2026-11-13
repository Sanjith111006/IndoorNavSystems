import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/app_failure.dart';
import '../../../core/errors/result.dart';
import '../../../core/network/api_client.dart';
import '../domain/building.dart';

final buildingRepositoryProvider = Provider<BuildingRepository>((ref) {
  return BuildingRepository(ref.watch(apiClientProvider));
});

class BuildingRepository {
  BuildingRepository(this._apiClient);

  final ApiClient _apiClient;

  Future<Result<List<Building>>> fetchBuildings() async {
    try {
      final data = await _apiClient.getObjectList('/api/buildings');
      return Success(data.map(Building.fromJson).toList(growable: false));
    } on AppFailure catch (failure) {
      return Failure(failure);
    } on FormatException {
      return const Failure(InvalidResponseFailure());
    }
  }

  Future<Result<BuildingDetail>> fetchBuilding(int buildingId) async {
    try {
      final data = await _apiClient.getObject('/api/buildings/$buildingId');
      return Success(BuildingDetail.fromJson(data));
    } on AppFailure catch (failure) {
      return Failure(failure);
    } on FormatException {
      return const Failure(InvalidResponseFailure());
    }
  }
}
