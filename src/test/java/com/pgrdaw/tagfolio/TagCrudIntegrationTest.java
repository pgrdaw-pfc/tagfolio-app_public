package com.pgrdaw.tagfolio;

import com.pgrdaw.tagfolio.model.Image;
import com.pgrdaw.tagfolio.model.Tag;
import com.pgrdaw.tagfolio.model.User;
import com.pgrdaw.tagfolio.repository.ImageRepository;
import com.pgrdaw.tagfolio.repository.TagRepository;
import com.pgrdaw.tagfolio.repository.UserRepository;
import com.pgrdaw.tagfolio.service.FileStorageService;
import com.pgrdaw.tagfolio.service.ImageService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TagCrudIntegrationTest {

    @Autowired
    private ImageService imageService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    // Store IDs, not entities, to survive entityManager.clear()
    private Long testUserId;
    private Long testImageId;

    @BeforeAll
    void setupStorageDirectories() throws IOException {
        Files.createDirectories(fileStorageService.getOriginalsPath());
        Files.createDirectories(fileStorageService.getThumbnailsPath());
        Files.createDirectories(fileStorageService.getDeletedPath());
    }

    @BeforeEach
    void setUp() throws IOException {
        User user = new User("testuser@tagfolio.com", passwordEncoder.encode("password"));
        userRepository.saveAndFlush(user); // Save and flush to get ID immediately

        MockMultipartFile multipartFile = createMockJpegFile("test-image.jpg", "test image content");
        ImageService.UploadResult result = imageService.processAndSaveFile(multipartFile, user);
        Image image = result.getImage();

        assertNotNull(image);
        assertNotNull(image.getId());

        this.testUserId = user.getId();
        this.testImageId = image.getId();

        // Clear context to ensure subsequent tests start fresh
        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() throws IOException {
        cleanDirectoryContents(fileStorageService.getOriginalsPath());
        cleanDirectoryContents(fileStorageService.getThumbnailsPath());
        cleanDirectoryContents(fileStorageService.getDeletedPath());
    }

    private void cleanDirectoryContents(Path directory) throws IOException {
        if (Files.exists(directory) && Files.isDirectory(directory)) {
            try (var stream = Files.walk(directory)) {
                stream.filter(path -> !path.equals(directory))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(file -> {
                            try {
                                Files.delete(file.toPath());
                            } catch (IOException e) {
                                System.err.println("Failed to delete file/directory during cleanup: " + file.toPath() + " - " + e.getMessage());
                            }
                        });
            }
        }
    }

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
    void testAddTagsToImage() {
        User user = userRepository.findById(testUserId).orElseThrow();
        imageService.addTagToImage(testImageId, "tag1, tag2, new-tag", user);

        entityManager.flush();

        Image updatedImage = imageRepository.findById(testImageId).orElseThrow();
        Set<String> imageTagNames = updatedImage.getTags().stream().map(Tag::getName).collect(Collectors.toSet());

        assertTrue(imageTagNames.contains("tag1"));
        assertTrue(imageTagNames.contains("tag2"));
        assertTrue(imageTagNames.contains("new-tag"));
    }

    @Test
    void testRemoveTagsFromImage() {
        // Arrange
        User user = userRepository.findById(testUserId).orElseThrow();
        imageService.addTagToImage(testImageId, "tagA, tagB, tagC", user);
        entityManager.flush();
        entityManager.clear();

        // Act
        User currentUser = userRepository.findById(testUserId).orElseThrow();
        List<String> tagsToRemove = Arrays.asList("tagA", "tagC");
        imageService.removeTagFromImage(testImageId, tagsToRemove, currentUser);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Image updatedImage = imageRepository.findById(testImageId).orElseThrow();
        Set<String> imageTagNames = updatedImage.getTags().stream().map(Tag::getName).collect(Collectors.toSet());

        assertFalse(imageTagNames.contains("taga"));
        assertTrue(imageTagNames.contains("tagb"), "tagB should still be present on the image.");
        assertFalse(imageTagNames.contains("tagc"));
        assertEquals(1, imageTagNames.size());

        assertFalse(tagRepository.findByName("taga").isPresent(), "tagA should be deleted.");
        assertTrue(tagRepository.findByName("tagb").isPresent(), "tagB should still exist.");
        assertFalse(tagRepository.findByName("tagc").isPresent(), "tagC should be deleted.");
    }

    @Test
    void testDeleteTagsGlobally() throws IOException {
        // Arrange
        User user = userRepository.findById(testUserId).orElseThrow();
        imageService.addTagToImage(testImageId, "global-tag1, global-tag2, unique-tag-for-img1", user);

        MockMultipartFile multipartFile2 = createMockJpegFile("test-image2.jpg", "second image content");
        ImageService.UploadResult result2 = imageService.processAndSaveFile(multipartFile2, user);
        Long testImage2Id = result2.getImage().getId();
        imageService.addTagToImage(testImage2Id, "global-tag1, global-tag3, unique-tag-for-img2", user);
        entityManager.flush();
        entityManager.clear();

        // Act & Assert for unique-tag-for-img1
        User currentUser = userRepository.findById(testUserId).orElseThrow();
        imageService.deleteTagsGlobally(List.of("unique-tag-for-img1"), currentUser);
        entityManager.flush();
        entityManager.clear();
        assertFalse(tagRepository.findByName("unique-tag-for-img1").isPresent());

        // Act & Assert for global-tag1
        currentUser = userRepository.findById(testUserId).orElseThrow();
        imageService.deleteTagsGlobally(List.of("global-tag1"), currentUser);
        entityManager.flush();
        entityManager.clear();
        assertFalse(tagRepository.findByName("global-tag1").isPresent());

        // Act & Assert for unique-tag-for-img2
        currentUser = userRepository.findById(testUserId).orElseThrow();
        imageService.deleteTagsGlobally(List.of("unique-tag-for-img2"), currentUser);
        entityManager.flush();
        entityManager.clear();
        assertFalse(tagRepository.findByName("unique-tag-for-img2").isPresent());

        // Cleanup
        Image imageToDelete = imageRepository.findById(testImage2Id).orElseThrow();
        imageService.deleteImage(imageToDelete);
    }

    @Test
    void testTagsDeletedWithImage() throws IOException {
        User user = userRepository.findById(testUserId).orElseThrow();
        imageService.addTagToImage(testImageId, "image-specific-tag", user);
        entityManager.flush();
        entityManager.clear();

        assertTrue(tagRepository.findByName("image-specific-tag").isPresent());

        Image imageToDelete = imageRepository.findById(testImageId).orElseThrow();
        imageService.deleteImage(imageToDelete);
        entityManager.flush();

        assertFalse(imageRepository.existsById(testImageId));
        assertFalse(tagRepository.findByName("image-specific-tag").isPresent());
    }

    @Test
    void testTagsRemainIfUsedByOtherImagesWhenImageDeleted() throws IOException {
        User user = userRepository.findById(testUserId).orElseThrow();
        imageService.addTagToImage(testImageId, "common-tag", user);

        MockMultipartFile multipartFile2 = createMockJpegFile("test-image2.jpg", "second image content");
        ImageService.UploadResult result2 = imageService.processAndSaveFile(multipartFile2, user);
        Long testImage2Id = result2.getImage().getId();
        imageService.addTagToImage(testImage2Id, "common-tag", user);
        entityManager.flush();
        entityManager.clear();

        Image image1ToDelete = imageRepository.findById(testImageId).orElseThrow();
        imageService.deleteImage(image1ToDelete);
        entityManager.flush();
        entityManager.clear();

        assertFalse(imageRepository.existsById(testImageId));
        assertTrue(tagRepository.findByName("common-tag").isPresent());

        Image image2ToDelete = imageRepository.findById(testImage2Id).orElseThrow();
        imageService.deleteImage(image2ToDelete);
        entityManager.flush();

        assertFalse(tagRepository.findByName("common-tag").isPresent());
    }
}
