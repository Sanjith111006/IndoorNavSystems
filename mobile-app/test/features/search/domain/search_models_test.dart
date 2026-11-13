import 'package:flutter_test/flutter_test.dart';
import 'package:indoor_nav_mobile/features/search/domain/destination.dart';
import 'package:indoor_nav_mobile/features/search/domain/start_location.dart';

void main() {
  test('parses a destination returned by search', () {
    final destination = Destination.fromJson(const <String, Object?>{
      'id': 12,
      'name': 'Information desk',
      'category': 'information',
      'nodeId': 55,
      'floorId': 2,
      'floorDisplayName': 'G',
    });

    expect(destination.nodeId, 55);
    expect(destination.floorDisplayName, 'G');
  });

  test('parses a manually selectable start location', () {
    final start = StartLocation.fromJson(const <String, Object?>{
      'id': 55,
      'label': 'North entrance',
      'type': 'ENTRANCE',
      'floorId': 2,
      'floorDisplayName': 'G',
      'x': 30.0,
      'y': 20.0,
    });

    expect(start.id, 55);
    expect(start.label, 'North entrance');
  });
}
