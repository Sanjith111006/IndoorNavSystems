package com.indoornav.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Boots the whole application over real HTTP with the database deliberately unreachable.
 *
 * <p>Startup alone proves the beans wire together, the entities map and every repository query
 * parses. Requests then prove filter ordering, the security rules and the database-down path.
 * Checks against a real PostgreSQL schema belong in Testcontainers tests.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.datasource.url=jdbc:postgresql://127.0.0.1:1/unreachable",
            "spring.datasource.username=test",
            "spring.datasource.password=fake-test-password",
            "spring.datasource.hikari.initialization-fail-timeout=-1",
            "spring.datasource.hikari.connection-timeout=250",
            "spring.flyway.enabled=false",
            "spring.jpa.hibernate.ddl-auto=none",
            "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
            "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
            "springdoc.api-docs.enabled=true"
        })
class BackendApplicationTest {

    private static final String ADMIN = "test-admin";
    private static final String ADMIN_PASSWORD = "fake-" + UUID.randomUUID();
    private static final int OVER_BODY_LIMIT_BYTES = 16 * 1024 + 1;

    private final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    private final JsonMapper json = JsonMapper.builder().build();

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void adminCredentials(DynamicPropertyRegistry registry) {
        registry.add("app.admin.username", () -> ADMIN);
        registry.add("app.admin.password-hash", () -> new BCryptPasswordEncoder(4).encode(ADMIN_PASSWORD));
    }

    @Test
    void healthReportsDownWhenTheDatabaseIsUnreachable() throws Exception {
        HttpResponse<String> response = send(get("/api/health"));

        assertThat(response.statusCode()).isEqualTo(503);
        JsonNode body = json.readTree(response.body());
        assertThat(body.get("status").asString()).isEqualTo("DOWN");
        assertThat(body.get("database").asString()).isEqualTo("DOWN");
        assertThat(body.get("requestId").asString())
                .isEqualTo(response.headers().firstValue("X-Request-Id").orElseThrow());
    }

    @Test
    void databaseFailureOnAReadReturns503WithRetryAfterAndNoInternals() throws Exception {
        HttpResponse<String> response = send(get("/api/buildings"));

        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.headers().firstValue("Retry-After")).contains("5");
        assertThat(json.readTree(response.body()).get("code").asString()).isEqualTo("SERVICE_UNAVAILABLE");
        assertThat(response.body()).doesNotContain("jdbc", "postgresql", "Exception", "127.0.0.1");
    }

    @Test
    void unauthorizedResponseStillCarriesARequestId() throws Exception {
        HttpResponse<String> response = send(get("/api/obstacles?buildingId=1"));

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("WWW-Authenticate")).contains("Bearer");
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        assertThat(json.readTree(response.body()).get("requestId").asString())
                .isEqualTo(response.headers().firstValue("X-Request-Id").orElseThrow());
    }

    @Test
    void signInThenAdminCallPassesAuthorization() throws Exception {
        HttpResponse<String> login = send(login(ADMIN, ADMIN_PASSWORD));
        assertThat(login.statusCode()).isEqualTo(200);
        JsonNode token = json.readTree(login.body());
        assertThat(token.get("tokenType").asString()).isEqualTo("Bearer");
        assertThat(token.get("expiresInSeconds").asLong()).isEqualTo(3600);

        HttpResponse<String> obstacles = send(HttpRequest.newBuilder(uri("/api/obstacles?buildingId=1"))
                .header("Authorization", "Bearer " + token.get("accessToken").asString())
                .GET()
                .build());

        // 503 rather than 401: the token was accepted and the request reached the database layer.
        assertThat(obstacles.statusCode()).isEqualTo(503);
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        HttpResponse<String> response = send(login(ADMIN, "fake-wrong-password"));

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(json.readTree(response.body()).get("code").asString()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    void badTokenDoesNotBreakAPublicEndpoint() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/api/health"))
                .header("Authorization", "Bearer expired-or-garbage")
                .GET()
                .build());

        assertThat(response.statusCode()).isNotEqualTo(401);
    }

    @Test
    void oversizedBodyIsRejectedWhenTheLengthIsDeclared() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("x".repeat(OVER_BODY_LIMIT_BYTES)))
                .build());

        assertThat(response.statusCode()).isEqualTo(413);
        assertThat(json.readTree(response.body()).get("code").asString()).isEqualTo("PAYLOAD_TOO_LARGE");
    }

    @Test
    void oversizedChunkedBodyIsRejected() throws Exception {
        byte[] body = new byte[OVER_BODY_LIMIT_BYTES];
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(body)))
                .build());

        assertThat(response.statusCode()).isEqualTo(413);
    }

    @Test
    void floorPlanTraversalLooksLikeAMissingFile() throws Exception {
        HttpResponse<String> response = send(get("/assets/floorplans/%2e%2e/%2e%2e/etc/passwd.svg"));

        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void openApiDocumentDescribesTheApi() throws Exception {
        HttpResponse<String> response = send(get("/v3/api-docs"));

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode paths = json.readTree(response.body()).get("paths");
        assertThat(paths.has("/api/buildings")).isTrue();
        assertThat(paths.has("/api/floors/{floorId}/map")).isTrue();
        assertThat(paths.has("/api/obstacles/{obstacleId}")).isTrue();
        assertThat(paths.has("/api/auth/login")).isTrue();
    }

    private HttpRequest get(String path) {
        return HttpRequest.newBuilder(uri(path)).GET().build();
    }

    private HttpRequest login(String username, String password) {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        return HttpRequest.newBuilder(uri("/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
