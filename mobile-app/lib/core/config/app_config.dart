class AppConfig {
  const AppConfig({required this.apiBaseUrl});

  factory AppConfig.fromEnvironment() {
    const apiBaseUrl = String.fromEnvironment('API_BASE_URL');
    if (apiBaseUrl.isEmpty) {
      throw const FormatException(
        'API_BASE_URL must be supplied at build time.',
      );
    }

    return const AppConfig(apiBaseUrl: apiBaseUrl);
  }

  final String apiBaseUrl;
}
