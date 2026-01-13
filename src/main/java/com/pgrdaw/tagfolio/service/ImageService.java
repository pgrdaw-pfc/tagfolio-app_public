package com.pgrdaw.tagfolio.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgrdaw.tagfolio.model.Image;
import com.pgrdaw.tagfolio.model.Tag;
import com.pgrdaw.tagfolio.model.User;
import com.pgrdaw.tagfolio.repository.ImageRepository;
import com.pgrdaw.tagfolio.repository.TagRepository;
import com.pgrdaw.tagfolio.service.util.MetadataService;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

/**
 * A service for managing images.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Service
public class ImageService {

    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);

    private final int imageMaxDimension;
    private final long imageMaxFileSizeKB;
    private final int thumbnailMaxDimension;

    private final ImageRepository imageRepository;
    private final TagRepository tagRepository;
    private final ExifToolService exifToolService;
    private final ObjectMapper objectMapper;
    private final ImageSecurityService imageSecurityService;
    private final FileStorageService fileStorageService;

    private final Map<String, String> tagSourceKeys;
    private final Map<String, String> sortableFields;

    private static final Set<String> INTERNAL_IGNORED_METADATA_KEYS = Set.of(
            "SourceFile", "FileName", "Directory", "FileModifyDate", "CreateDate",
            "FileAccessDate", "FileInodeChangeDate", "FilePermissions"
    );

    @Getter
    @Setter
    public static class UploadResult {
        private Image image;
        private String status;
        private FileConflict conflict;
    }

    @Getter
    @Setter
    public static class FileConflict {
        private String fileName;
        private Map<String, Object> originalMetadata;
        private Map<String, Object> newMetadata;
    }

    /**
     * Constructs a new ImageService.
     *
     * @param imageRepository      The image repository.
     * @param tagRepository        The tag repository.
     * @param exifToolService      The ExifTool service.
     * @param imageSecurityService The image security service.
     * @param fileStorageService   The file storage service.
     * @param metadataService      The metadata service.
     * @param tagSourceKeys        A map of tag source keys.
     * @param sortableFields       A map of sortable fields.
     * @param imageMaxDimension    The maximum dimension for image resizing.
     * @param imageMaxFileSizeKB   The maximum file size in kilobytes for image resizing.
     * @param thumbnailMaxDimension The maximum dimension for thumbnail resizing.
     */
    @Autowired
    public ImageService(ImageRepository imageRepository,
                        TagRepository tagRepository,
                        ExifToolService exifToolService,
                        ImageSecurityService imageSecurityService,
                        FileStorageService fileStorageService,
                        MetadataService metadataService,
                        @Value("#{${image.exiftool.tag-source-keys}}") Map<String, String> tagSourceKeys,
                        @Value("#{${app.sortable-fields}}") Map<String, String> sortableFields,
                        @Value("${image.max-dimension}") int imageMaxDimension,
                        @Value("${image.max-file-size-kb}") long imageMaxFileSizeKB,
                        @Value("${thumbnail.max-dimension}") int thumbnailMaxDimension) {
        this.imageRepository = imageRepository;
        this.tagRepository = tagRepository;
        this.exifToolService = exifToolService;
        this.imageSecurityService = imageSecurityService;
        this.fileStorageService = fileStorageService;
        this.imageMaxDimension = imageMaxDimension;
        this.imageMaxFileSizeKB = imageMaxFileSizeKB;
        this.thumbnailMaxDimension = thumbnailMaxDimension;
        this.objectMapper = new ObjectMapper();
        this.sortableFields = sortableFields;
        this.tagSourceKeys = tagSourceKeys;
    }

    /**
     * Processes and saves an uploaded file.
     *
     * @param file The multipart file to process.
     * @param user The user who uploaded the file.
     * @return An {@link UploadResult} indicating the status of the upload.
     * @throws IOException if an I/O error occurs.
     */
    @Transactional
    public UploadResult processAndSaveFile(MultipartFile file, User user) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }

        String originalFilename = file.getOriginalFilename();
        Optional<Image> existingImageOpt = imageRepository.findByOriginalFileNameAndUser(originalFilename, user);

        if (existingImageOpt.isPresent()) {
            Image existingImage = existingImageOpt.get();
            byte[] newFileBytes = file.getBytes();

            Path tempFile = Files.createTempFile("upload-", ".tmp");
            Map<String, Object> newMetadata;
            try {
                Files.write(tempFile, newFileBytes);
                newMetadata = exifToolService.read(tempFile);
            } finally {
                Files.deleteIfExists(tempFile);
            }

            if (isMetadataEqual(existingImage.getExiftool(), newMetadata)) {
                UploadResult result = new UploadResult();
                result.setStatus("SKIPPED");
                result.setImage(existingImage);
                return result;
            } else {
                UploadResult result = new UploadResult();
                result.setStatus("CONFLICT");
                FileConflict conflict = new FileConflict();
                conflict.setFileName(originalFilename);
                conflict.setOriginalMetadata(objectMapper.readValue(existingImage.getExiftool(), new TypeReference<>() {
                }));
                conflict.setNewMetadata(newMetadata);
                result.setConflict(conflict);
                return result;
            }
        }

        Image newImage = createImageFromFile(file, user);
        UploadResult result = new UploadResult();
        result.setStatus("UPLOADED");
        result.setImage(newImage);
        return result;
    }

    /**
     * Synchronizes images for a user from their storage directory.
     *
     * @param user The user to synchronize images for.
     * @return A list of {@link UploadResult}s indicating the status of each synchronized file.
     * @throws IOException if an I/O error occurs.
     */
    @Transactional
    public List<UploadResult> syncUserImages(User user) throws IOException {
        Path userOriginalsDir = fileStorageService.getOriginalsPath().resolve(String.valueOf(user.getId()));
        if (!Files.exists(userOriginalsDir)) {
            throw new IOException("Directory not found: " + userOriginalsDir.toAbsolutePath());
        }

        List<UploadResult> results = new ArrayList<>();
        try (java.util.stream.Stream<Path> stream = Files.list(userOriginalsDir)) {
            List<Path> files = stream.filter(Files::isRegularFile).collect(Collectors.toList());
            if (files.isEmpty()) {
                logger.warn("No files found in {}", userOriginalsDir);
            }
            for (Path file : files) {
                try {
                    UploadResult result = processExistingFile(file, user);
                    if (result != null) {
                        results.add(result);
                    }
                } catch (Exception e) {
                    logger.error("Error syncing file {}: {}", file, e.getMessage());
                }
            }
        }
        return results;
    }

    private UploadResult processExistingFile(Path filePath, User user) throws IOException {
        String originalFilename = filePath.getFileName().toString();
        List<Image> existingImages = imageRepository.findAllByOriginalFileNameAndUser(originalFilename, user);

        if (existingImages.size() > 1) {
            logger.warn("Found {} duplicate records for file {}. Deleting all and re-importing.", existingImages.size(), originalFilename);
            for (Image img : existingImages) {
                try {
                    fileStorageService.deleteThumbnailFile(img.getThumbnailFileName(), user.getId());
                } catch (Exception e) {
                    logger.warn("Failed to delete thumbnail for duplicate image {}: {}", img.getId(), e.getMessage());
                }
                img.getTags().clear();
                imageRepository.save(img);
                imageRepository.delete(img);
            }
            // Fall through to create new
        } else if (existingImages.size() == 1) {
            Image existingImage = existingImages.get(0);
            Map<String, Object> newMetadata = exifToolService.read(filePath);

            if (isMetadataEqual(existingImage.getExiftool(), newMetadata) && !hasInvalidMetadata(existingImage.getExiftool())) {
                logger.info("Skipping image {}: Metadata unchanged.", originalFilename);
                UploadResult result = new UploadResult();
                result.setStatus("SKIPPED");
                result.setImage(existingImage);
                return result;
            } else {
                logger.info("Updating metadata for image {}", originalFilename);
                
                existingImage.setExiftool(objectMapper.writeValueAsString(newMetadata));
                updateImageFieldsFromMetadata(existingImage, newMetadata);
                syncTagsFromMetadata(newMetadata, existingImage);
                
                // Regenerate thumbnail as well, since file might have changed
                try {
                    BufferedImage originalImage = ImageIO.read(filePath.toFile());
                    if (originalImage != null) {
                        BufferedImage thumbnailImage = resizeImageByDimension(originalImage, thumbnailMaxDimension);
                        saveThumbnailToStorage(thumbnailImage, existingImage.getThumbnailFileName(), user.getId());
                        logger.info("Regenerated thumbnail for updated image: {}", originalFilename);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to regenerate thumbnail during sync update for {}", originalFilename, e);
                }

                imageRepository.save(existingImage);
                
                UploadResult result = new UploadResult();
                result.setStatus("UPLOADED");
                result.setImage(existingImage);
                return result;
            }
        }

        byte[] fileBytes = Files.readAllBytes(filePath);
        BufferedImage originalBufferedImage = ImageIO.read(new ByteArrayInputStream(fileBytes));
        if (originalBufferedImage == null) {
             return null;
        }
        
        Image newImage = createImageEntityAndProcess(originalBufferedImage, originalFilename, user, fileBytes);
        logger.info("Imported new image: {}", originalFilename);

        UploadResult result = new UploadResult();
        result.setStatus("UPLOADED");
        result.setImage(newImage);
        return result;
    }

    private boolean hasInvalidMetadata(String metadataJson) {
        if (metadataJson == null) return false;
        try {
            Map<String, Object> metadata = objectMapper.readValue(metadataJson, new TypeReference<>() {});
            String sourceFile = (String) metadata.get("SourceFile");
            if (sourceFile != null && (sourceFile.startsWith("/tmp/") || sourceFile.contains(".tmp"))) {
                return true;
            }
            String systemFileName = (String) metadata.get("System:FileName");
            if (systemFileName != null && systemFileName.startsWith("upload-") && systemFileName.endsWith(".tmp")) {
                return true;
            }
        } catch (Exception e) {
            logger.warn("Failed to parse metadata JSON for validation", e);
        }
        return false;
    }

    private boolean isMetadataEqual(String existingMetadataJson, Map<String, Object> newMetadata) throws IOException {
        if (existingMetadataJson == null || newMetadata == null) {
            return existingMetadataJson == null && newMetadata == null;
        }
        Map<String, Object> existingMetadata = objectMapper.readValue(existingMetadataJson, new TypeReference<>() {
        });

        for (Map.Entry<String, String> entry : tagSourceKeys.entrySet()) {
            String configuredKeys = entry.getValue();
            List<String> individualKeys = Arrays.stream(configuredKeys.split(","))
                                                .map(String::trim)
                                                .toList();

            for (String key : individualKeys) {
                if (INTERNAL_IGNORED_METADATA_KEYS.contains(key)) {
                    continue;
                }

                Set<String> existingValues = normalizeMetadataValue(existingMetadata.get(key));
                Set<String> newValues = normalizeMetadataValue(newMetadata.get(key));

                if (!existingValues.equals(newValues)) {
                    return false;
                }
            }
        }

        return true;
    }

    private Set<String> normalizeMetadataValue(Object value) {
        if (value == null) {
            return Collections.emptySet();
        }
        if (value instanceof String) {
            return Collections.singleton((String) value);
        }
        if (value instanceof Collection) {
            return ((Collection<?>) value).stream()
                    .map(Object::toString)
                    .collect(Collectors.toSet());
        }
        return Collections.singleton(value.toString());
    }

    /**
     * Overwrites an existing image with a new file.
     *
     * @param file The new multipart file.
     * @param user The user who owns the image.
     * @return The newly created image.
     * @throws IOException if an I/O error occurs.
     */
    @Transactional
    public Image overwriteImage(MultipartFile file, User user) throws IOException {
        String originalFilename = file.getOriginalFilename();
        Image existingImage = imageRepository.findByOriginalFileNameAndUser(originalFilename, user)
                .orElseThrow(() -> new IOException("No existing image to overwrite."));

        fileStorageService.moveOriginalToDeleted(existingImage.getOriginalFileName(), user.getId());
        fileStorageService.deleteThumbnailFile(existingImage.getThumbnailFileName(), user.getId());
        imageRepository.delete(existingImage);

        return createImageFromFile(file, user);
    }

    /**
     * Saves an image entity.
     *
     * @param image The image to save.
     * @return The saved image.
     */
    @Transactional
    public Image saveImage(Image image) {
        return imageRepository.save(image);
    }

    /**
     * Creates an image entity from a multipart file.
     *
     * @param file The multipart file.
     * @param user The user who owns the image.
     * @return The newly created image.
     * @throws IOException if an I/O error occurs.
     */
    @Transactional
    public Image createImageFromFile(MultipartFile file, User user) throws IOException {
        byte[] fileBytes = file.getBytes();
        BufferedImage originalBufferedImage = ImageIO.read(new ByteArrayInputStream(fileBytes));
        if (originalBufferedImage == null) {
            throw new IOException("Could not read image file: " + file.getOriginalFilename());
        }
        return createImageEntityAndProcess(originalBufferedImage, file.getOriginalFilename(), user, fileBytes);
    }

    private Image createImageEntityAndProcess(BufferedImage originalImage, String originalFilename, User user, byte[] originalFileBytes) throws IOException {
        String thumbnailFilename = generateThumbnailFilename(originalFilename);

        // 1. Save original bytes to destination first
        Path originalFilePath = fileStorageService.saveOriginalFile(originalFileBytes, originalFilename, user.getId());

        // 2. Extract metadata from the saved file
        Map<String, Object> exiftoolData = exifToolService.read(originalFilePath);
        if (exiftoolData == null) {
            exiftoolData = new HashMap<>();
        }

        // 3. Process Original Image (Resize & Compress) if needed
        boolean originalModified = false;
        BufferedImage processedOriginalImage = originalImage;

        if (originalImage.getWidth() > imageMaxDimension || originalImage.getHeight() > imageMaxDimension) {
            processedOriginalImage = resizeImageByDimension(originalImage, imageMaxDimension);
            originalModified = true;
            logger.info("Original image {} resized to max dimension {}.", originalFilename, imageMaxDimension);
        }

        byte[] processedOriginalBytes = originalFileBytes;
        if (originalModified) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(processedOriginalImage, "jpg", baos); // Assuming JPG for processed images
            processedOriginalBytes = baos.toByteArray();
        }

        if (processedOriginalBytes.length > imageMaxFileSizeKB * 1024) {
            logger.info("Image {} exceeds max file size.", originalFilename);
            processedOriginalImage = resizeImageByFileSize(processedOriginalImage, originalFilename, imageMaxFileSizeKB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(processedOriginalImage, "jpg", baos);
            processedOriginalBytes = baos.toByteArray();
            originalModified = true;
        }

        // 4. If modified, overwrite the file
        if (originalModified) {
             fileStorageService.saveOriginalFile(processedOriginalBytes, originalFilename, user.getId());
        }

        // Create Thumbnail from the processed original
        BufferedImage thumbnailImage = resizeImageByDimension(processedOriginalImage, thumbnailMaxDimension);
        saveThumbnailToStorage(thumbnailImage, thumbnailFilename, user.getId());

        Image image = new Image(user);
        image.setImportedAt(LocalDateTime.now());
        image.setOriginalFileName(originalFilename);
        image.setThumbnailFileName(thumbnailFilename);
        image.setExiftool(objectMapper.writeValueAsString(exiftoolData));

        updateImageFieldsFromMetadata(image, exiftoolData);
        syncTagsFromMetadata(exiftoolData, image);

        return imageRepository.save(image);
    }

    private void updateImageFieldsFromMetadata(Image image, Map<String, Object> exiftoolData) {
        Object systemFileNameObj = getMetadataValue("Name", exiftoolData);
        if (systemFileNameObj != null) {
            // We generally want to keep the original filename as is, but if we want to sync with metadata name:
            // image.setOriginalFileName(String.valueOf(systemFileNameObj));
            // For now, let's assume originalFileName is set from the file itself.
        }

        Object createDateObj = getMetadataValue("Created", exiftoolData);
        if (createDateObj instanceof String) {
            try {
                String dateString = ((String) createDateObj).split("\\+")[0].trim();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
                image.setCreatedAt(LocalDateTime.parse(dateString, formatter));
            } catch (DateTimeParseException e) {
                logger.warn("Could not parse create date '{}' for image '{}'.", createDateObj, image.getOriginalFileName(), e);
            }
        }

        Object fileModifyDateObj = getMetadataValue("Modified", exiftoolData);
        if (fileModifyDateObj instanceof String) {
            try {
                String dateString = ((String) fileModifyDateObj).split("\\+")[0].trim();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
                image.setModifiedAt(LocalDateTime.parse(dateString, formatter));
            } catch (DateTimeParseException e) {
                logger.warn("Could not parse file modify date '{}' for image '{}'", fileModifyDateObj, image.getOriginalFileName(), e);
            }
        }

        Object ratingObj = getMetadataValue("Rating", exiftoolData);
        if (ratingObj != null) {
            try {
                image.setRating(Integer.parseInt(String.valueOf(ratingObj)));
            } catch (NumberFormatException e) {
                logger.warn("Could not parse rating '{}' for image '{}'", ratingObj, image.getOriginalFileName(), e);
            }
        }
    }

    private Object getMetadataValue(String fieldName, Map<String, Object> metadata) {
        String keys = sortableFields.get(fieldName);
        if (keys == null) {
            return null;
        }
        return Arrays.stream(keys.split(","))
                .map(String::trim)
                .map(metadata::get)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String generateThumbnailFilename(String originalFilename) {
        String baseName = getBaseName(originalFilename);
        return baseName + "_thumb.jpg";
    }

    private void saveThumbnailToStorage(BufferedImage thumbnailImage, String thumbnailFileName, Long userId) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(thumbnailImage, "jpg", baos);
        fileStorageService.saveThumbnailFile(baos.toByteArray(), thumbnailFileName, userId);
    }

    private BufferedImage resizeImageByDimension(BufferedImage originalImage, int maxDim) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        int newWidth, newHeight;
        if (originalWidth > originalHeight) {
            newWidth = maxDim;
            newHeight = (int) (originalHeight * ((double) maxDim / originalWidth));
        } else {
            newHeight = maxDim;
            newWidth = (int) (originalWidth * ((double) maxDim / originalHeight));
        }
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();
        return resizedImage;
    }

    private BufferedImage resizeImageByFileSize(BufferedImage image, String filename, long maxSizeKB) throws IOException {
        float quality = 0.8f;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

        while (true) {
            baos.reset();
            param.setCompressionQuality(quality);
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), param);
            }
            if (baos.size() <= maxSizeKB * 1024 || quality < 0.1f) {
                logger.info("Image {} resized by quality. Final size: {} KB, Quality: {}", filename, baos.size() / 1024, quality);
                return ImageIO.read(new ByteArrayInputStream(baos.toByteArray()));
            }
            quality -= 0.1f;
        }
    }

    private void syncTagsFromMetadata(Map<String, Object> metadata, Image image) {
        Set<String> keywords = new HashSet<>();
        for (Map.Entry<String, String> entry : tagSourceKeys.entrySet()) {
            String configuredKeys = entry.getValue();
            Arrays.stream(configuredKeys.split(","))
                  .map(String::trim)
                  .filter(s -> !s.isEmpty())
                  .forEach(key -> addKeywords(keywords, metadata.get(key)));
        }

        if (!keywords.isEmpty()) {
            Set<Tag> imageTags = new HashSet<>();
            for (String keyword : keywords) {
                String tagName = keyword.toLowerCase();
                Tag tag = tagRepository.findByName(tagName).orElse(new Tag(tagName));
                tagRepository.save(tag);

                imageTags.add(tag);
            }
            image.setTags(imageTags);
        }
    }

    private void addKeywords(Set<String> keywords, Object value) {
        if (value instanceof String str) {
            String trimmedStr = str.trim();
            if (!trimmedStr.isEmpty()) {
                keywords.add(trimmedStr);
            }
        } else if (value instanceof java.util.List) {
            ((java.util.List<?>) value).stream()
                    .filter(String.class::isInstance)
                    .map(s -> ((String) s).trim())
                    .filter(s -> !s.isEmpty())
                    .forEach(keywords::add);
        }
    }

    private String getBaseName(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? filename : filename.substring(0, dotIndex);
    }

    /**
     * Deletes an image.
     *
     * @param image The image to delete.
     * @throws IOException if an I/O error occurs.
     */
    @Transactional
    public void deleteImage(Image image) throws IOException {
        Set<Tag> tagsToRemove = new HashSet<>(image.getTags());

        image.getTags().clear();
        imageRepository.save(image);

        fileStorageService.deleteThumbnailFile(image.getThumbnailFileName(), image.getUser().getId());
        fileStorageService.moveOriginalToDeleted(image.getOriginalFileName(), image.getUser().getId());
        imageRepository.delete(image);

        for (Tag tag : tagsToRemove) {
            if (imageRepository.countByTags(tag) == 0) {
                tagRepository.delete(tag);
            }
        }
    }

    /**
     * Gets a list of images for a user.
     *
     * @param user The user.
     * @return A list of images for the user.
     */
    public List<Image> getImagesForUser(User user) {
        List<Image> images;
        if (user.isAdmin()) {
            images = imageRepository.findAll();
        } else {
            images = imageRepository.findByUserId(user.getId());
        }
        return images.stream()
                .filter(image -> imageSecurityService.canRead(user, image))
                .collect(Collectors.toList());
    }

    /**
     * Finds a list of images by their IDs.
     *
     * @param imageIds The list of image IDs.
     * @return A list of images.
     */
    public List<Image> findImagesByIds(List<Long> imageIds) {
        return imageRepository.findAllById(imageIds);
    }

    /**
     * Adds tags to an image.
     *
     * @param imageId    The ID of the image.
     * @param tagsString A comma-separated string of tags.
     * @param user       The user performing the action.
     * @throws RuntimeException      if the image is not found.
     * @throws AccessDeniedException if the user does not have permission to modify the image.
     */
    @Transactional
    public void addTagToImage(Long imageId, String tagsString, User user) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        if (!imageSecurityService.canUpdate(user, image)) {
            throw new AccessDeniedException("You do not have permission to modify tags for this image.");
        }

        String[] tagNames = tagsString.split(",");

        for (String tagName : tagNames) {
            String trimmedTagName = tagName.trim().toLowerCase();
            if (trimmedTagName.isEmpty()) {
                continue;
            }

            Tag tag = tagRepository.findByName(trimmedTagName).orElseGet(() -> {
                Tag newTag = new Tag(trimmedTagName);
                return tagRepository.save(newTag);
            });

            image.getTags().add(tag);
        }
        imageRepository.save(image);
    }

    /**
     * Removes tags from an image.
     *
     * @param imageId  The ID of the image.
     * @param tagNames The list of tag names to remove.
     * @param user     The user performing the action.
     * @throws RuntimeException      if the image is not found.
     * @throws AccessDeniedException if the user does not have permission to modify the image.
     */
    @Transactional
    public void removeTagFromImage(Long imageId, List<String> tagNames, User user) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        if (!imageSecurityService.canUpdate(user, image)) {
            throw new AccessDeniedException("You do not have permission to modify tags for this image.");
        }

        Set<String> lowerCaseTagNamesToRemove = tagNames.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        Set<Tag> tagsToRemove = image.getTags().stream()
                .filter(tag -> lowerCaseTagNamesToRemove.contains(tag.getName()))
                .collect(Collectors.toSet());

        image.getTags().removeAll(tagsToRemove);
        imageRepository.save(image);

        for (Tag tag : tagsToRemove) {
            if (imageRepository.countByTags(tag) == 0) {
                tagRepository.delete(tag);
            }
        }
    }

    /**
     * Deletes tags globally from all images.
     *
     * @param tagNames The list of tag names to delete.
     * @param user     The user performing the action.
     */
    @Transactional
    public void deleteTagsGlobally(List<String> tagNames, User user) {
        List<Tag> tagsToDelete = tagRepository.findByNameIn(new HashSet<>(tagNames));

        for (Tag tag : tagsToDelete) {
            List<Image> imagesWithTag = imageRepository.findByTags(tag);

            for (Image image : imagesWithTag) {
                if (imageSecurityService.canUpdate(user, image)) {
                    image.getTags().remove(tag);
                    imageRepository.save(image);
                } else {
                    logger.warn("User {} does not have permission to remove tag '{}' from image {}. Skipping.", user.getEmail(), tag.getName(), image.getId());
                }
            }

            if (imageRepository.countByTags(tag) == 0) {
                tagRepository.delete(tag);
                logger.info("Tag '{}' is no longer used by any image and has been deleted.", tag.getName());
            } else {
                logger.info("Tag '{}' is still used by other images and will not be deleted from the system.", tag.getName());
            }
        }
    }

    /**
     * Gets an image by its ID.
     *
     * @param imageId The ID of the image.
     * @return The image.
     * @throws RuntimeException if the image is not found.
     */
    public Image getImageById(Long imageId) {
        return imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found with id: " + imageId));
    }

    /**
     * Loads an image as a resource.
     *
     * @param fileName The name of the file.
     * @param userId   The ID of the user who owns the image.
     * @return The image resource.
     * @throws MalformedURLException if the file URL is malformed.
     */
    public Resource loadImageAsResource(String fileName, Long userId) throws MalformedURLException {
        return fileStorageService.loadOriginalAsResource(fileName, userId);
    }

    /**
     * Loads a thumbnail as a resource.
     *
     * @param fileName The name of the thumbnail file.
     * @param userId   The ID of the user who owns the image.
     * @return The thumbnail resource.
     * @throws MalformedURLException if the file URL is malformed.
     */
    public Resource loadThumbnailAsResource(String fileName, Long userId) throws MalformedURLException {
        return fileStorageService.loadThumbnailAsResource(fileName, userId);
    }

    /**
     * Zips a list of images.
     *
     * @param imageIds        The list of image IDs to zip.
     * @param zipOutputStream The zip output stream.
     * @throws IOException if an I/O error occurs.
     */
    public void zipImages(List<Long> imageIds, ZipOutputStream zipOutputStream) throws IOException {
        Path tempDir = Files.createTempDirectory("tagfolio-export-");
        try {
            Map<String, Path> filesToZip = new HashMap<>();

            for (Long id : imageIds) {
                Image image = imageRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Image not found with id: " + id));

                Resource resource = fileStorageService.loadOriginalAsResource(image.getOriginalFileName(), image.getUser().getId());
                if (resource.exists()) {
                    Path tempImagePath = tempDir.resolve(image.getOriginalFileName());
                    Files.copy(resource.getInputStream(), tempImagePath, StandardCopyOption.REPLACE_EXISTING);

                    List<String> tagNames = image.getTags().stream()
                            .map(Tag::getName)
                            .collect(Collectors.toList());

                    if (!tagNames.isEmpty()) {
                        Map<String, Object> tagsMap = new HashMap<>();
                        tagsMap.put("XMP-tf:Tags", tagNames);
                        String tagsJson = objectMapper.writeValueAsString(tagsMap);
                        exifToolService.write(tempImagePath, tagsJson);
                    }
                    filesToZip.put(image.getOriginalFileName(), tempImagePath);
                }
            }
            fileStorageService.zipFiles(filesToZip, zipOutputStream);
        } finally {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(file -> {
                        try {
                            Files.delete(file.toPath());
                        } catch (IOException e) {
                            logger.warn("Failed to delete temporary file: {}", file.toPath(), e);
                        }
                    });
        }
    }

    /**
     * Regenerates thumbnails for all images of a user.
     *
     * @param user The user.
     * @throws IOException if an I/O error occurs.
     */
    @Transactional
    public void regenerateThumbnailsForUser(User user) throws IOException {
        List<Image> images = getImagesForUser(user);
        for (Image image : images) {
            try {
                Resource resource = fileStorageService.loadOriginalAsResource(image.getOriginalFileName(), user.getId());
                if (resource.exists()) {
                    BufferedImage originalImage = ImageIO.read(resource.getInputStream());
                    if (originalImage != null) {
                        BufferedImage thumbnailImage = resizeImageByDimension(originalImage, thumbnailMaxDimension);
                        saveThumbnailToStorage(thumbnailImage, image.getThumbnailFileName(), user.getId());
                        logger.info("Regenerated thumbnail for image: {}", image.getOriginalFileName());
                    } else {
                        logger.warn("Could not read original image for thumbnail regeneration: {}", image.getOriginalFileName());
                    }
                } else {
                    logger.warn("Original image not found for thumbnail regeneration: {}", image.getOriginalFileName());
                }
            } catch (Exception e) {
                logger.error("Error regenerating thumbnail for image {}: {}", image.getOriginalFileName(), e.getMessage());
            }
        }
    }
}
