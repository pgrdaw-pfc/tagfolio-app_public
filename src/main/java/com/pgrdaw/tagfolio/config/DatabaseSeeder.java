package com.pgrdaw.tagfolio.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgrdaw.tagfolio.model.*;
import com.pgrdaw.tagfolio.repository.*;
import com.pgrdaw.tagfolio.service.FileStorageService;
import com.pgrdaw.tagfolio.service.ImageService;
import com.pgrdaw.tagfolio.service.SharedFilterService;
import com.pgrdaw.tagfolio.service.util.SeedingStatusService;
import com.pgrdaw.tagfolio.util.DummyMultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A command-line runner that seeds the database with development data.
 * This seeder is only active when the "seed" profile is enabled.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Component
@Profile("seed")
@Order(2)
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSeeder.class);

    private static final int NUM_ADMINS = 1;
    private static final int NUM_USERS = 1;
    private static final int NUM_IMAGES_PER_USER = 5;
    private static final int NUM_FILTERS_PER_USER = 1;
    private static final int NUM_REPORTS_PER_USER = 1;

    private static final Path SAMPLE_IMAGES_DIR = Paths.get("sample_images");

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final FilterRepository filterRepository;
    private final ReportRepository reportRepository;
    private final ImageRepository imageRepository;
    private final ImageService imageService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final SharedFilterService sharedFilterService;
    private final SeedingStatusService seedingStatusService;
    private final ReportTypeRepository reportTypeRepository;
    private final FileStorageService fileStorageService;

    /**
     * Constructs a new DatabaseSeeder.
     *
     * @param roleRepository       The role repository.
     * @param userRepository       The user repository.
     * @param filterRepository     The filter repository.
     * @param reportRepository     The report repository.
     * @param imageRepository      The image repository.
     * @param imageService         The image service.
     * @param passwordEncoder      The password encoder.
     * @param objectMapper         The object mapper.
     * @param sharedFilterService  The shared filter service.
     * @param seedingStatusService The seeding status service.
     * @param reportTypeRepository The report type repository.
     * @param fileStorageService   The file storage service.
     */
    public DatabaseSeeder(RoleRepository roleRepository, UserRepository userRepository,
                          FilterRepository filterRepository, ReportRepository reportRepository,
                          ImageRepository imageRepository, ImageService imageService,
                          PasswordEncoder passwordEncoder, ObjectMapper objectMapper,
                          SharedFilterService sharedFilterService, SeedingStatusService seedingStatusService,
                          ReportTypeRepository reportTypeRepository, FileStorageService fileStorageService) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.filterRepository = filterRepository;
        this.reportRepository = reportRepository;
        this.imageRepository = imageRepository;
        this.imageService = imageService;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.sharedFilterService = sharedFilterService;
        this.seedingStatusService = seedingStatusService;
        this.reportTypeRepository = reportTypeRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional
    public synchronized void run(String... args) throws Exception {
        logger.info("Running development data seeder...");
        clearExistingImageData();
        imageRepository.deleteAll();
        logger.info("Cleared all image records from the database.");

        seedAdminUsers();
        seedNormalUsers();
        seedingStatusService.setSeedingComplete(true);
        logger.info("Development data seeding process has finished.");
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void clearExistingImageData() {
        logger.info("Clearing existing image data from storage...");

        Path originalsPath = fileStorageService.getOriginalsPath();
        Path thumbnailsPath = fileStorageService.getThumbnailsPath();
        Path deletedPath = fileStorageService.getDeletedPath();

        deleteFilesInDirectory(originalsPath);
        deleteFilesInDirectory(thumbnailsPath);
        deleteFilesInDirectory(deletedPath);
    }

    private void deleteFilesInDirectory(Path directory) {
        logger.info("Deleting files in directory: {}", directory);
        if (Files.exists(directory) && Files.isDirectory(directory)) {
            try (Stream<Path> walk = Files.walk(directory)) {
                walk.sorted(Comparator.reverseOrder())
                    .filter(p -> !p.equals(directory))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                            logger.debug("Deleted: {}", p);
                        } catch (IOException e) {
                            logger.error("Failed to delete: {}", p, e);
                        }
                    });
            } catch (IOException e) {
                logger.error("Error walking directory: {}", directory, e);
            }
        } else {
            logger.warn("Directory not found or not a directory: {}", directory);
        }
    }

    private void seedAdminUsers() {
        Role adminRole = roleRepository.findByName(RoleType.ADMIN)
                .orElseThrow(() -> new RuntimeException("ADMIN role not found!"));

        for (int i = 1; i <= NUM_ADMINS; i++) {
            String email = "admin" + i + "@tagfolio.com";
            if (userRepository.findByEmail(email).isEmpty()) {
                User adminUser = new User(email, passwordEncoder.encode(email));
                adminUser.addRole(adminRole);
                userRepository.save(adminUser);
            }
        }
    }

    private void seedNormalUsers() throws IOException {
        Role userRole = roleRepository.findByName(RoleType.USER)
                .orElseThrow(() -> new RuntimeException("USER role not found!"));

        List<File> allSampleImageFiles;
        try (Stream<Path> paths = Files.list(SAMPLE_IMAGES_DIR)) {
            allSampleImageFiles = paths
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Warning: Could not read sample images directory. Seeding images will be skipped. {}", e.getMessage());
            allSampleImageFiles = List.of();
        }

        if (allSampleImageFiles.isEmpty()) {
            logger.warn("No sample images found in {}. Skipping image seeding for normal users.", SAMPLE_IMAGES_DIR);
            return;
        }

        for (int i = 1; i <= NUM_USERS; i++) {
            String email = "user" + i + "@tagfolio.com";
            User normalUser = userRepository.findByEmail(email).orElseGet(() -> {
                User newUser = new User(email, passwordEncoder.encode(email));
                newUser.addRole(userRole);
                return userRepository.save(newUser);
            });

            List<Image> userImages = seedImagesForUser(normalUser, allSampleImageFiles);
            seedFiltersForUser(normalUser, userImages);
            seedReportsForUser(normalUser, userImages);
        }
    }

    private List<Image> seedImagesForUser(User user, List<File> allSampleImageFiles) throws IOException {
        logger.info("Seeding user {} with {} images.", user.getEmail(), NUM_IMAGES_PER_USER);
        List<Image> userImages = new ArrayList<>();
        Collections.shuffle(allSampleImageFiles);

        for (int j = 0; j < Math.min(NUM_IMAGES_PER_USER, allSampleImageFiles.size()); j++) {
            File imageFile = allSampleImageFiles.get(j);
            byte[] fileContent = Files.readAllBytes(imageFile.toPath());
            String originalFileName = imageFile.getName();
            String contentType = Files.probeContentType(imageFile.toPath());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            DummyMultipartFile dummyFile = new DummyMultipartFile(
                    fileContent, "file", originalFileName, contentType
            );

            try {
                ImageService.UploadResult result = imageService.processAndSaveFile(dummyFile, user);
                Image imageToPersist = result.getImage();

                if (imageToPersist != null) {
                    Image persistedImage = imageRepository.save(imageToPersist);
                    if (persistedImage.getId() != null) {
                        userImages.add(persistedImage);
                    } else {
                        logger.warn("Warning: Persisted Image has null ID for {}", originalFileName);
                    }
                }
            } catch (IOException e) {
                logger.error("Error processing sample image {} for user {}: {}", originalFileName, user.getEmail(), e.getMessage());
            }
        }
        return userImages;
    }

    private String removeAccentsAndSpecialChars(String str) {
        String normalized = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String result = pattern.matcher(normalized).replaceAll("");
        result = result.replace('ñ', 'n').replace('Ñ', 'N');
        return result.replaceAll("[^a-zA-Z0-9_]+", "_");
    }

    private String generateDefaultFilterName(List<Map<String, String>> expression) {
        String name = expression.stream()
                .map(item -> {
                    String type = item.get("type");
                    String value = item.get("value");
                    if ("tag".equals(type)) {
                        return removeAccentsAndSpecialChars(value).toLowerCase();
                    } else if ("operator".equals(type)) {
                        return "_" + value.toUpperCase() + "_";
                    } else if ("parenthesis".equals(type)) {
                        return value;
                    } else if ("comparator".equals(type)) {
                        return "_" + value + "_";
                    } else if ("field".equals(type)) {
                        return "_" + removeAccentsAndSpecialChars(value).replace(':', '_').toLowerCase() + "_";
                    }
                    return "";
                })
                .collect(Collectors.joining(""))
                .trim()
                .replaceAll("__+", "_");

        return name.replaceAll("^_|_$", "");
    }

    private void seedFiltersForUser(User user, List<Image> userImages) {

        List<String> tagList = userImages.stream()
                .flatMap(image -> image.getTags().stream())
                .map(Tag::getName).distinct().collect(Collectors.toList());
        Collections.shuffle(tagList);

        List<Filter> filtersToSeed = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < NUM_FILTERS_PER_USER; i++) {
            if (tagList.isEmpty()) {
                break;
            }

            int filterType = random.nextInt(5);
            List<Map<String, String>> expression = new ArrayList<>();

            switch (filterType) {
                case 0:
                    String tagName = tagList.get(random.nextInt(tagList.size()));
                    expression.add(Map.of("type", "tag", "value", tagName));
                    break;
                case 1:
                    if (tagList.size() >= 2) {
                        String tag1 = tagList.get(random.nextInt(tagList.size()));
                        String tag2;
                        do {
                            tag2 = tagList.get(random.nextInt(tagList.size()));
                        } while (tag1.equals(tag2));
                        expression.add(Map.of("type", "tag", "value", tag1));
                        expression.add(Map.of("type", "operator", "value", "AND"));
                        expression.add(Map.of("type", "tag", "value", tag2));
                    }
                    break;
                case 2:
                    if (tagList.size() >= 2) {
                        String tag1 = tagList.get(random.nextInt(tagList.size()));
                        String tag2;
                        do {
                            tag2 = tagList.get(random.nextInt(tagList.size()));
                        } while (tag1.equals(tag2));
                        expression.add(Map.of("type", "tag", "value", tag1));
                        expression.add(Map.of("type", "operator", "value", "OR"));
                        expression.add(Map.of("type", "tag", "value", tag2));
                    }
                    break;
                case 3:
                    tagName = tagList.get(random.nextInt(tagList.size()));
                    expression.add(Map.of("type", "operator", "value", "NOT"));
                    expression.add(Map.of("type", "tag", "value", tagName));
                    break;
                case 4:
                    if (tagList.size() >= 3) {
                        String tag1 = tagList.get(random.nextInt(tagList.size()));
                        String tag2;
                        do {
                            tag2 = tagList.get(random.nextInt(tagList.size()));
                        } while (tag1.equals(tag2));
                        String tag3;
                        do {
                            tag3 = tagList.get(random.nextInt(tagList.size()));
                        } while (tag3.equals(tag1) || tag3.equals(tag2));

                        expression.add(Map.of("type", "parenthesis", "value", "("));
                        expression.add(Map.of("type", "tag", "value", tag1));
                        expression.add(Map.of("type", "operator", "value", "AND"));
                        expression.add(Map.of("type", "tag", "value", tag2));
                        expression.add(Map.of("type", "parenthesis", "value", ")"));
                        expression.add(Map.of("type", "operator", "value", "OR"));
                        expression.add(Map.of("type", "tag", "value", tag3));
                    }
                    break;
            }

            if (!expression.isEmpty()) {
                String filterName = generateDefaultFilterName(expression);
                filtersToSeed.add(createFilter(user, filterName, expression));
            }
        }

        List<Filter> savedFilters = filterRepository.saveAll(filtersToSeed);

        for (int i = 0; i < savedFilters.size() / 2; i++) {
            sharedFilterService.createShareableLink(savedFilters.get(i).getId());
        }
    }

    private Filter createFilter(User user, String name, List<Map<String, String>> expression) {
        Filter filter = new Filter();
        filter.setName(name);
        filter.setUser(user);
        try {
            filter.setExpression(objectMapper.writeValueAsString(expression));
        } catch (JsonProcessingException e) {
            logger.error("Error serializing filter expression: {}", e.getMessage());
            filter.setExpression("[]");
        }
        return filter;
    }

    private void seedReportsForUser(User user, List<Image> userImages) {
        if (userImages.isEmpty()) {
            return;
        }

        ReportType compactReportType = reportTypeRepository.findByName("compact").orElseThrow();
        ReportType extendedReportType = reportTypeRepository.findByName("extended").orElseThrow();

        Random random = new Random();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

        for (int i = 1; i <= NUM_REPORTS_PER_USER; i++) {
            Report report = new Report();
            report.setCreationDate(java.time.LocalDateTime.now().minusDays(random.nextInt(30)));
            report.setName(report.getCreationDate().format(formatter));
            report.setUser(user);
            report.setReportType(compactReportType);

            List<Image> imagesForReport = new ArrayList<>();
            int numImagesToSelect = Math.min(userImages.size(), random.nextInt(5) + 1);

            List<Image> shuffledUserImages = new ArrayList<>(userImages);
            Collections.shuffle(shuffledUserImages);

            for (int j = 0; j < numImagesToSelect; j++) {
                imagesForReport.add(shuffledUserImages.get(j));
            }

            if (imagesForReport.isEmpty()) {
                continue;
            }

            report = reportRepository.save(report);

            List<ReportImage> reportImages = new ArrayList<>();
            for (int j = 0; j < imagesForReport.size(); j++) {
                Image img = imagesForReport.get(j);
                ReportImageId reportImageId = new ReportImageId();
                reportImageId.setReportId(report.getId());
                reportImageId.setImageId(img.getId());

                ReportImage reportImage = new ReportImage();
                reportImage.setId(reportImageId);
                reportImage.setReport(report);
                reportImage.setImage(img);
                reportImage.setSortingOrder(j);
                reportImages.add(reportImage);
            }
            report.setReportImages(reportImages);

            reportRepository.save(report);
        }
    }
}
