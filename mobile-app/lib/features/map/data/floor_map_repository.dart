import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/app_failure.dart';
import '../../../core/errors/result.dart';
import '../../../core/network/api_client.dart';
import '../domain/floor_map.dart';

final floorMapRepositoryProvider = Provider<FloorMapRepository>((ref) {
  return FloorMapRepository(ref.watch(apiClientProvider));
});

class FloorMapRepository {
  FloorMapRepository(this._apiClient);

  final ApiClient _apiClient;

  Future<Result<FloorMap>> fetchFloorMap(int floorId) async {
    try {
      final data = await _apiClient.getObject('/api/floors/$floorId/map');
      final plan = data['plan'];
      if (plan is! Map<Object?, Object?>) {
        return const Failure(InvalidResponseFailure());
      }
      final planUrl = plan['url'];
      if (planUrl is! String) {
        return const Failure(InvalidResponseFailure());
      }
      final svgContent = await _apiClient.getText(planUrl);
      return Success(FloorMap.fromJson(json: data, svgContent: svgContent));
    } on AppFailure catch (failure) {
      return Failure(failure);
    } on FormatException {
      return const Failure(InvalidResponseFailure());
    }
  }
}
