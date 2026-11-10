import 'package:flutter_test/flutter_test.dart';
import 'package:indoor_nav_mobile/features/buildings/domain/building.dart';

void main() {
  test('parses a building returned by the API', () {
    final building = Building.fromJson(const <String, Object?>{
      'id': 42,
      'name': 'Central library',
      'description': 'Study spaces and services',
      'buildingType': 'library',
      'address': null,
      'floorCount': 3,
    });

    expect(building.id, 42);
    expect(building.name, 'Central library');
    expect(building.floorCount, 3);
    expect(building.address, isNull);
  });

  test('rejects a building with an invalid identifier', () {
    expect(
      () => Building.fromJson(const <String, Object?>{
        'id': '42',
        'name': 'Central library',
        'buildingType': 'library',
        'floorCount': 3,
      }),
      throwsFormatException,
    );
  });
}
