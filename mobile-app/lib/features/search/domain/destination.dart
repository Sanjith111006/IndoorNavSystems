class Destination {
  const Destination({
    required this.id,
    required this.name,
    required this.category,
    required this.nodeId,
    required this.floorId,
    required this.floorDisplayName,
  });

  factory Destination.fromJson(Map<String, Object?> json) {
    return Destination(
      id: _requiredInt(json, 'id'),
      name: _requiredString(json, 'name'),
      category: _requiredString(json, 'category'),
      nodeId: _requiredInt(json, 'nodeId'),
      floorId: _requiredInt(json, 'floorId'),
      floorDisplayName: _requiredString(json, 'floorDisplayName'),
    );
  }

  final int id;
  final String name;
  final String category;
  final int nodeId;
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
