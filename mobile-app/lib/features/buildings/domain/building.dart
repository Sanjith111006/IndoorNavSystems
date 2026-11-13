class Building {
  const Building({
    required this.id,
    required this.name,
    required this.description,
    required this.buildingType,
    required this.address,
    required this.floorCount,
  });

  factory Building.fromJson(Map<String, Object?> json) {
    return Building(
      id: _requiredInt(json, 'id'),
      name: _requiredString(json, 'name'),
      description: _optionalString(json, 'description'),
      buildingType: _requiredString(json, 'buildingType'),
      address: _optionalString(json, 'address'),
      floorCount: _requiredInt(json, 'floorCount'),
    );
  }

  final int id;
  final String name;
  final String? description;
  final String buildingType;
  final String? address;
  final int floorCount;

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

  static String? _optionalString(Map<String, Object?> json, String key) {
    final value = json[key];
    if (value == null) {
      return null;
    }
    if (value is! String) {
      throw FormatException('$key must be a string when present.');
    }
    return value;
  }
}

class BuildingDetail extends Building {
  const BuildingDetail({
    required super.id,
    required super.name,
    required super.description,
    required super.buildingType,
    required super.address,
    required super.floorCount,
    required this.defaultFloorId,
  });

  factory BuildingDetail.fromJson(Map<String, Object?> json) {
    return BuildingDetail(
      id: Building._requiredInt(json, 'id'),
      name: Building._requiredString(json, 'name'),
      description: Building._optionalString(json, 'description'),
      buildingType: Building._requiredString(json, 'buildingType'),
      address: Building._optionalString(json, 'address'),
      floorCount: Building._requiredInt(json, 'floorCount'),
      defaultFloorId: Building._requiredInt(json, 'defaultFloorId'),
    );
  }

  final int defaultFloorId;
}
