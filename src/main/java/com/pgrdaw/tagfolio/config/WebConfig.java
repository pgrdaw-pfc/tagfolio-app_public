package com.pgrdaw.tagfolio.config;

import com.pgrdaw.tagfolio.service.FileStorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for the application, including resource handlers.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final FileStorageService fileStorageService;

    /**
     * Constructs a new WebConfig.
     *
     * @param fileStorageService The file storage service.
     */
    public WebConfig(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * Adds resource handlers to serve static content.
     * Configures handlers for thumbnails and original images.
     *
     * @param registry The {@link ResourceHandlerRegistry} to configure.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/thumbnails/**")
                .addResourceLocations("file:" + fileStorageService.getThumbnailsPath().toString() + "/");

        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + fileStorageService.getOriginalsPath().toString() + "/");
    }
}
