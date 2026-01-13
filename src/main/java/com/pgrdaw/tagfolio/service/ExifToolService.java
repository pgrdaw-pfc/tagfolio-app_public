package com.pgrdaw.tagfolio.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A service for interacting with the ExifTool command-line utility.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Service
public class ExifToolService {

    private static final Logger logger = LoggerFactory.getLogger(ExifToolService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Path tempConfigPath;

    public static class ExifToolException extends RuntimeException {
        public ExifToolException(String message) {
            super(message);
        }
        public ExifToolException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Initializes the ExifToolService by creating a temporary config file.
     *
     * @throws ExifToolException if the temporary config file cannot be created.
     */
    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("exiftool_config");
            try (InputStream inputStream = resource.getInputStream()) {
                this.tempConfigPath = Files.createTempFile("exiftool_config", ".conf");
                Files.copy(inputStream, tempConfigPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            logger.error("Could not create temporary ExifTool config file", e);
            this.tempConfigPath = null;
            throw new ExifToolException("Failed to initialize ExifTool service: could not create temporary config file.", e);
        }
    }

    /**
     * Cleans up the temporary ExifTool config file.
     */
    @PreDestroy
    public void cleanup() {
        if (this.tempConfigPath != null) {
            try {
                Files.deleteIfExists(this.tempConfigPath);
                logger.info("Deleted temporary ExifTool config file: {}", this.tempConfigPath);
            } catch (IOException e) {
                logger.error("Could not delete temporary ExifTool config file", e);
            }
        }
    }

    /**
     * Executes an ExifTool command and returns its standard output.
     * Throws ExifToolException if the command fails or times out.
     *
     * @param command The list of command line arguments for ExifTool.
     * @param timeoutSeconds The maximum time to wait for the ExifTool process to complete.
     * @return The standard output of the ExifTool process.
     * @throws ExifToolException if the ExifTool process encounters an error, times out, or is interrupted.
     */
    private String executeExifToolCommand(List<String> command, long timeoutSeconds) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new ExifToolException("ExifTool process timed out after " + timeoutSeconds + " seconds. Command: " + String.join(" ", command));
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                logger.error("ExifTool command failed with exit code {}. Command: {}. Output: {}", exitCode, String.join(" ", command), output.toString().trim());
                throw new ExifToolException("ExifTool command failed with exit code " + exitCode + ". Output: " + output.toString().trim());
            }

            return output.toString();

        } catch (IOException e) {
            logger.error("IOException during ExifTool command execution. Command: {}", String.join(" ", command), e);
            throw new ExifToolException("Failed to execute ExifTool command due to I/O error.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("ExifTool command execution was interrupted. Command: {}", String.join(" ", command), e);
            throw new ExifToolException("ExifTool command execution was interrupted.", e);
        }
    }

    /**
     * Reads metadata from an image file.
     *
     * @param path The path to the image file.
     * @return A map of metadata keys and values.
     */
    public Map<String, Object> read(Path path) {
        if (path == null || !Files.exists(path)) {
            logger.warn("Attempted to read EXIF data from a null or non-existent path: {}", path);
            return null;
        }

        List<String> command = new ArrayList<>();
        command.add("exiftool");
        command.add("-json");
        command.add("-G1");
        command.add("-struct");
        command.add(path.toString());

        try {
            String output = executeExifToolCommand(command, 30);
            List<Map<String, Object>> metadataList = objectMapper.readValue(output, new TypeReference<>() {});

            if (metadataList != null && !metadataList.isEmpty()) {
                return metadataList.get(0);
            }
        } catch (ExifToolException e) {
            logger.error("Failed to read EXIF data from file: {}", path, e);
            return null;
        } catch (IOException e) {
            logger.error("Failed to parse JSON output from ExifTool for file: {}", path, e);
            return null;
        }
        return null;
    }

    /**
     * Writes metadata to an image file.
     *
     * @param path The path to the image file.
     * @param json The JSON string of metadata to write.
     * @throws IllegalArgumentException if the path or JSON is invalid.
     */
    public void write(Path path, String json) {
        if (path == null || !Files.exists(path) || json == null || json.isEmpty()) {
            logger.warn("Invalid input for writing EXIF data. Path: {}, JSON empty: {}", path, json == null || json.isEmpty());
            throw new IllegalArgumentException("Path must exist and JSON must not be empty for writing EXIF data.");
        }

        try {
            Map<String, Object> metadataToWrite = objectMapper.readValue(json, new TypeReference<>() {});

            List<String> command = new ArrayList<>();
            command.add("exiftool");
            if (tempConfigPath != null) {
                command.add("-config");
                command.add(tempConfigPath.toString());
            }

            for (Map.Entry<String, Object> entry : metadataToWrite.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof List) {
                    for (Object item : (List<?>) value) {
                        command.add("-" + key + "=" + String.valueOf(item));
                    }
                } else {
                    command.add("-" + key + "=" + String.valueOf(value));
                }
            }

            command.add("-overwrite_original");
            command.add(path.toString());

            String output = executeExifToolCommand(command, 60);
            logger.info("Successfully wrote EXIF data to file: {}. Output: {}", path, output.trim());

        } catch (ExifToolException e) {
            logger.error("Failed to write EXIF data to file: {}", path, e);
        } catch (IOException e) {
            logger.error("Failed to parse JSON input for writing EXIF data to file: {}", path, e);
        }
    }
}
