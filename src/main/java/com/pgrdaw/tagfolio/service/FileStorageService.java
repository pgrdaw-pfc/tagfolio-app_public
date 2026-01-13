package com.pgrdaw.tagfolio.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A service for managing file storage.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Getter
@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    private final Path originalsPath;
    private final Path thumbnailsPath;
    private final Path deletedPath;

    /**
     * Constructs a new FileStorageService.
     *
     * @param originalsPath      The path to the originals directory.
     * @param thumbnailsPath     The path to the thumbnails directory.
     * @param deletedStoragePath The path to the deleted files directory.
     */
    public FileStorageService(@Value("${storage.originals-path}") String originalsPath,
                              @Value("${storage.thumbnails-path}") String thumbnailsPath,
                              @Value("${file.storage-deleted-path}") String deletedStoragePath) {
        this.originalsPath = Paths.get(originalsPath);
        this.thumbnailsPath = Paths.get(thumbnailsPath);
        this.deletedPath = Paths.get(deletedStoragePath);
    }

    /**
     * Initializes the storage directories.
     *
     * @throws RuntimeException if the storage directories cannot be initialized.
     */
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(originalsPath);
            Files.createDirectories(thumbnailsPath);
            Files.createDirectories(deletedPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directories!", e);
        }
    }

    /**
     * Saves an original file.
     *
     * @param fileBytes The bytes of the file.
     * @param fileName  The name of the file.
     * @param userId    The ID of the user who owns the file.
     * @return The path to the saved file.
     * @throws IOException if an I/O error occurs.
     */
    public Path saveOriginalFile(byte[] fileBytes, String fileName, Long userId) throws IOException {
        Path userDir = originalsPath.resolve(String.valueOf(userId));
        if (!Files.exists(userDir)) {
            Files.createDirectories(userDir);
        }
        Path filePath = userDir.resolve(fileName);
        Files.write(filePath, fileBytes);
        return filePath;
    }

    /**
     * Saves a thumbnail file.
     *
     * @param fileBytes The bytes of the file.
     * @param fileName  The name of the file.
     * @param userId    The ID of the user who owns the file.
     * @throws IOException if an I/O error occurs.
     */
    public void saveThumbnailFile(byte[] fileBytes, String fileName, Long userId) throws IOException {
        Path userDir = thumbnailsPath.resolve(String.valueOf(userId));
        if (!Files.exists(userDir)) {
            Files.createDirectories(userDir);
        }
        Path filePath = userDir.resolve(fileName);
        Files.write(filePath, fileBytes);
    }

    /**
     * Loads an original file as a resource.
     *
     * @param fileName The name of the file.
     * @param userId   The ID of the user who owns the file.
     * @return The file resource.
     * @throws MalformedURLException if the file URL is malformed.
     */
    public Resource loadOriginalAsResource(String fileName, Long userId) throws MalformedURLException {
        Path filePath = this.originalsPath.resolve(String.valueOf(userId)).resolve(fileName);
        return new UrlResource(filePath.toUri());
    }

    /**
     * Loads a thumbnail file as a resource.
     *
     * @param fileName The name of the file.
     * @param userId   The ID of the user who owns the file.
     * @return The file resource.
     * @throws MalformedURLException if the file URL is malformed.
     */
    public Resource loadThumbnailAsResource(String fileName, Long userId) throws MalformedURLException {
        Path filePath = this.thumbnailsPath.resolve(String.valueOf(userId)).resolve(fileName);
        return new UrlResource(filePath.toUri());
    }

    /**
     * Moves an original file to the deleted directory.
     *
     * @param fileName The name of the file.
     * @param userId   The ID of the user who owns the file.
     * @throws IOException if an I/O error occurs.
     */
    public void moveOriginalToDeleted(String fileName, Long userId) throws IOException {
        Path sourcePath = originalsPath.resolve(String.valueOf(userId)).resolve(fileName);

        if (Files.exists(sourcePath)) {
            int dotIndex = fileName.lastIndexOf('.');
            String baseName = (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
            String extension = (dotIndex == -1) ? "" : fileName.substring(dotIndex);

            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

            String newFileName = baseName + "_" + timestamp + extension;

            Path userDeletedDir = deletedPath.resolve(String.valueOf(userId));
            if (!Files.exists(userDeletedDir)) {
                Files.createDirectories(userDeletedDir);
            }
            Path destinationPath = userDeletedDir.resolve(newFileName);

            Files.move(sourcePath, destinationPath);
        }
    }

    /**
     * Deletes a thumbnail file.
     *
     * @param fileName The name of the file.
     * @param userId   The ID of the user who owns the file.
     * @throws IOException if an I/O error occurs.
     */
    public void deleteThumbnailFile(String fileName, Long userId) throws IOException {
        Path filePath = thumbnailsPath.resolve(String.valueOf(userId)).resolve(fileName);
        Files.deleteIfExists(filePath);
    }

    /**
     * Zips a map of files into the provided ZipOutputStream.
     *
     * @param filesToZip      A map where the key is the desired entry name in the zip file
     *                        and the value is the Path to the file on disk.
     * @param zipOutputStream The ZipOutputStream to write to.
     * @throws IOException If an I/O error occurs.
     */
    public void zipFiles(Map<String, Path> filesToZip, ZipOutputStream zipOutputStream) throws IOException {
        Path tempDir = Files.createTempDirectory("tagfolio-export-");
        try {
            for (Map.Entry<String, Path> entry : filesToZip.entrySet()) {
                String zipEntryName = entry.getKey();
                Path filePathOnDisk = entry.getValue();

                if (Files.exists(filePathOnDisk)) {
                    zipOutputStream.putNextEntry(new ZipEntry(zipEntryName));
                    Files.copy(filePathOnDisk, zipOutputStream);
                    zipOutputStream.closeEntry();
                } else {
                    logger.warn("File not found on disk for zipping: {}", filePathOnDisk);
                }
            }
        } finally {
            try (Stream<Path> pathStream = Files.walk(tempDir)) {
                pathStream
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(file -> {
                            if (!file.delete()) {
                                logger.warn("Failed to delete temporary file: {}", file);
                            }
                        });
            }
        }
    }

}
