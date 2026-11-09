package com.indoornav.backend.service;

import com.indoornav.backend.config.AppProperties;
import com.indoornav.backend.exception.ResourceNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Loads floor-plan SVGs for the public asset route (API.md 8.1).
 *
 * <p>A path is served only if it matches the stored pattern and, on disk, resolves inside the
 * configured directory after following symbolic links. Every failure looks like a missing file.
 */
@Service
public class FloorPlanService {

    public record FloorPlan(byte[] content, String entityTag) {
    }

    private static final Pattern ALLOWED_PATH = Pattern.compile("[a-z0-9_-]+(/[a-z0-9_-]+)*\\.svg");
    private static final String PACKAGED_LOCATION = "floorplans/";
    private static final String RESOURCE_NAME = "Floor plan";

    private final String directory;

    public FloorPlanService(AppProperties properties) {
        this.directory = properties.floorPlans().directory();
    }

    public FloorPlan load(String relativePath) {
        if (!ALLOWED_PATH.matcher(relativePath).matches()) {
            throw new ResourceNotFoundException(RESOURCE_NAME);
        }
        try {
            byte[] content = directory == null || directory.isBlank()
                    ? readPackaged(relativePath)
                    : readFromDirectory(relativePath);
            return new FloorPlan(content, entityTagOf(content));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static byte[] readPackaged(String relativePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(PACKAGED_LOCATION + relativePath);
        if (!resource.isReadable()) {
            throw new ResourceNotFoundException(RESOURCE_NAME);
        }
        return resource.getContentAsByteArray();
    }

    private byte[] readFromDirectory(String relativePath) throws IOException {
        Path file = Path.of(directory).resolve(relativePath);
        if (!Files.isRegularFile(file)) {
            throw new ResourceNotFoundException(RESOURCE_NAME);
        }
        if (!file.toRealPath().startsWith(Path.of(directory).toRealPath())) {
            throw new ResourceNotFoundException(RESOURCE_NAME);
        }
        return Files.readAllBytes(file);
    }

    private static String entityTagOf(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            return '"' + HexFormat.of().formatHex(digest) + '"';
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required on every Java platform", exception);
        }
    }
}
