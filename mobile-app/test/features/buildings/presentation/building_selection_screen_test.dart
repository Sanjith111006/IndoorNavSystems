import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:indoor_nav_mobile/core/network/api_client.dart';
import 'package:indoor_nav_mobile/core/theme/app_theme.dart';
import 'package:indoor_nav_mobile/features/buildings/data/building_repository.dart';
import 'package:indoor_nav_mobile/features/buildings/presentation/building_selection_screen.dart';

void main() {
  testWidgets('shows buildings returned by the repository', (tester) async {
    final apiClient = _FakeApiClient(const <Map<String, Object?>>[
      <String, Object?>{
        'id': 7,
        'name': 'Central library',
        'description': 'Study spaces and services',
        'buildingType': 'library',
        'address': null,
        'floorCount': 3,
      },
    ]);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          buildingRepositoryProvider.overrideWith(
            (ref) => BuildingRepository(apiClient),
          ),
        ],
        child: MaterialApp(
          theme: AppTheme.light(),
          home: const BuildingSelectionScreen(),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Central library'), findsOneWidget);
    expect(find.text('3 floors · Study spaces and services'), findsOneWidget);
  });

  testWidgets('shows an empty state when no buildings are returned', (
    tester,
  ) async {
    final apiClient = _FakeApiClient(const <Map<String, Object?>>[]);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          buildingRepositoryProvider.overrideWith(
            (ref) => BuildingRepository(apiClient),
          ),
        ],
        child: MaterialApp(
          theme: AppTheme.light(),
          home: const BuildingSelectionScreen(),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('No buildings available'), findsOneWidget);
  });
}

class _FakeApiClient extends ApiClient {
  _FakeApiClient(this._response) : super(Dio());

  final List<Map<String, Object?>> _response;

  @override
  Future<List<Map<String, Object?>>> getObjectList(
    String path, {
    Map<String, Object?>? queryParameters,
  }) async {
    return _response;
  }
}
