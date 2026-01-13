package com.pgrdaw.tagfolio;

import com.pgrdaw.tagfolio.model.Image;
import com.pgrdaw.tagfolio.model.User;
import com.pgrdaw.tagfolio.repository.ImageRepository;
import com.pgrdaw.tagfolio.repository.UserRepository;
import com.pgrdaw.tagfolio.service.FileStorageService;
import com.pgrdaw.tagfolio.service.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ImageCrudIntegrationTest {

    @Autowired
    @SuppressWarnings("FieldCanBeLocal")
    private ImageService imageService;

    @Autowired
    @SuppressWarnings("FieldCanBeLocal")
    private FileStorageService fileStorageService;

    @Autowired
    @SuppressWarnings("FieldCanBeLocal")
    private UserRepository userRepository;

    @Autowired
    @SuppressWarnings("FieldCanBeLocal")
    private ImageRepository imageRepository;

    @Autowired
    @SuppressWarnings("FieldCanBeLocal")
    private PasswordEncoder passwordEncoder;

    @SuppressWarnings("FieldCanBeLocal")
    private User testUser;
    @SuppressWarnings("FieldCanBeLocal")
    private MockMultipartFile multipartFile;
    @SuppressWarnings("FieldCanBeLocal")
    private Image testImage;

    @BeforeEach
    void setUp() throws IOException {
        testUser = new User("testuser@tagfolio.com", passwordEncoder.encode("password"));
        userRepository.save(testUser);

        multipartFile = createMockJpegFile("test-image.jpg", "test image content");

        ImageService.UploadResult result = imageService.processAndSaveFile(multipartFile, testUser);
        testImage = result.getImage();
    }

    @SuppressWarnings({"unused", "SameParameterValue"})
    private MockMultipartFile createMockJpegFile(String filename, String content) throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return new MockMultipartFile(
                "file",
                filename,
                "image/jpeg",
                baos.toByteArray()
        );
    }

    @Test
    void testCreateImage() throws IOException {
        assertNotNull(testImage);
        assertNotNull(testImage.getId());

        String originalFileName = testImage.getOriginalFileName();
        String thumbnailFileName = testImage.getThumbnailFileName();

        // Updated to look in user-specific directory
        Path originalPath = fileStorageService.getOriginalsPath()
                .resolve(String.valueOf(testUser.getId()))
                .resolve(originalFileName);
        Path thumbnailPath = fileStorageService.getThumbnailsPath()
                .resolve(String.valueOf(testUser.getId()))
                .resolve(thumbnailFileName);

        assertTrue(Files.exists(originalPath));
        assertTrue(Files.exists(thumbnailPath));
    }

    @Test
    void testReadImage() {
        Image foundImage = imageService.getImageById(testImage.getId());
        assertNotNull(foundImage);
        assertEquals(testImage.getId(), foundImage.getId());
    }

    @Test
    void testUpdateImage() {
        imageService.addTagToImage(testImage.getId(), "new-tag", testUser);
        Image updatedImage = imageService.getImageById(testImage.getId());
        assertTrue(updatedImage.getTags().stream().anyMatch(tag -> tag.getName().equals("new-tag")));
    }

    @Test
    void testDeleteImage() throws IOException {
        String originalFileName = testImage.getOriginalFileName();
        String thumbnailFileName = testImage.getThumbnailFileName();

        imageService.deleteImage(testImage);

        // Updated to look in user-specific directory
        Path originalPath = fileStorageService.getOriginalsPath()
                .resolve(String.valueOf(testUser.getId()))
                .resolve(originalFileName);
        Path thumbnailPath = fileStorageService.getThumbnailsPath()
                .resolve(String.valueOf(testUser.getId()))
                .resolve(thumbnailFileName);
        
        // Updated to look in user-specific deleted directory
        Path deletedDir = fileStorageService.getDeletedPath()
                .resolve(String.valueOf(testUser.getId()));

        // Base name without extension
        int dotIndex = originalFileName.lastIndexOf('.');
        String baseName = (dotIndex == -1)
                ? originalFileName
                : originalFileName.substring(0, dotIndex);

        boolean deletedFileExists = false;
        if (Files.exists(deletedDir)) {
            try (var stream = Files.list(deletedDir)) {
                deletedFileExists = stream.anyMatch(path ->
                        path.getFileName().toString().startsWith(baseName + "_")
                );
            }
        }

        assertFalse(imageRepository.existsById(testImage.getId()));
        assertFalse(Files.exists(originalPath));
        assertFalse(Files.exists(thumbnailPath));
        assertTrue(deletedFileExists);
    }
}
