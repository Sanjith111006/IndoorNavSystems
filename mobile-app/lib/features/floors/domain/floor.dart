class Floor {
  const Floor({
    required this.id,
    required this.buildingId,
    required this.level,
    required this.displayName,
  });

  factory Floor.fromJson(Map<String, Object?> json) {
    return Floor(
      id: _requiredInt(json, 'id'),
      buildingId: _requiredInt(json, 'buildingId'),
      level: _requiredInt(json, 'level'),
      displayName: _requiredString(json, 'displayName'),
    );
  }

  final int id;
  final int buildingId;
  final int level;
  final String displayName;

  static int _requiredInt(Map<String, Object?> json, String key) {
    final value = json[key];
    if (value is! int) {
      throw FormatException('$key must be an integer.');
    }
    return value;
  }

  static String _requiredString(Map<String, Object?> json, String key) {
    final value = json[key];
    if (value is! String || value.isEmpty) {
      throw FormatException('$key must be a non-empty string.');
    }
    return value;
  }
}
