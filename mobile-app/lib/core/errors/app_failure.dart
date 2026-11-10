sealed class AppFailure implements Exception {
  const AppFailure();

  String get title;
  String get message;
}

class ConnectionFailure extends AppFailure {
  const ConnectionFailure();

  @override
  String get title => 'Couldn\'t connect';

  @override
  String get message => 'Check your connection and try again.';
}

class TimeoutFailure extends AppFailure {
  const TimeoutFailure();

  @override
  String get title => 'This is taking longer than usual';

  @override
  String get message => 'Check your connection and try again.';
}

class ServerFailure extends AppFailure {
  const ServerFailure();

  @override
  String get title => 'Something went wrong';

  @override
  String get message => 'Try again shortly.';
}

class InvalidResponseFailure extends AppFailure {
  const InvalidResponseFailure();

  @override
  String get title => 'Couldn\'t load buildings';

  @override
  String get message => 'Try again.';
}
