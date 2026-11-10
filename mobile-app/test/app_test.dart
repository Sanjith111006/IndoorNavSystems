import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:indoor_nav_mobile/app.dart';

void main() {
  testWidgets('shows the visitor home screen', (tester) async {
    await tester.pumpWidget(const ProviderScope(child: IndoorNavApp()));
    await tester.pumpAndSettle();

    expect(find.text('Indoor navigation'), findsOneWidget);
    expect(find.text('Choose a building'), findsOneWidget);
  });
}
