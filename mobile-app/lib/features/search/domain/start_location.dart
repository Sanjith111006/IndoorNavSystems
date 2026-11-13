class StartLocation {
  const StartLocation({
    required this.id,
    required this.label,
    required this.floorId,
    required this.floorDisplayName,
  });

  factory StartLocation.fromJson(Map<String, Object?> json) {
    return StartLocation(
      id: _requiredInt(json, 'id'),
      label: _requiredString(json, 'label'),
      floorId: _requiredInt(json, 'floorId'),
      floorDisplayName: _requiredString(json, 'floorDisplayName'),
    );
  }

  final int id;
  final String label;
  final int floorId;
  final String floorDisplayName;
}

int _requiredInt(Map<String, Object?> json, String key) {
  final value = json[key];
  if (value is! int) {
    throw FormatException('$key must be an integer.');
  }
  return value;
}

String _requiredString(Map<String, Object?> json, String key) {
  final value = json[key];
  if (value is! String || value.isEmpty) {
    throw FormatException('$key must be a non-empty string.');
  }
  return value;
}
