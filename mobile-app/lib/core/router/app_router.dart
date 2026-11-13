import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/buildings/presentation/building_selection_screen.dart';
import '../../features/buildings/presentation/home_screen.dart';
import '../../features/map/presentation/indoor_map_screen.dart';

final appRouterProvider = Provider<GoRouter>((ref) {
  return GoRouter(
    initialLocation: '/',
    routes: <RouteBase>[
      GoRoute(path: '/', builder: (context, state) => const HomeScreen()),
      GoRoute(
        path: '/buildings',
        builder: (context, state) => const BuildingSelectionScreen(),
      ),
      GoRoute(
        path: '/buildings/:buildingId/map',
        builder: (context, state) {
          final buildingId = int.tryParse(
            state.pathParameters['buildingId'] ?? '',
          );
          if (buildingId == null || buildingId < 1) {
            throw const FormatException('A valid building ID is required.');
          }
          return IndoorMapScreen(buildingId: buildingId);
        },
      ),
    ],
  );
});
