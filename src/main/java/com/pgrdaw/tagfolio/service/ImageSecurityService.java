package com.pgrdaw.tagfolio.service;

import com.pgrdaw.tagfolio.model.Image;
import com.pgrdaw.tagfolio.model.User;
import org.springframework.stereotype.Service;

/**
 * A service for checking user permissions on images.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Service
public class ImageSecurityService {

    private boolean isOwnerOrAdmin(User user, Image image) {
        return user.isAdmin() || image.getUser().getId().equals(user.getId());
    }

    /**
     * Checks if a user can read an image.
     *
     * @param user  The user.
     * @param image The image.
     * @return True if the user can read the image, false otherwise.
     */
    public boolean canRead(User user, Image image) {
        return isOwnerOrAdmin(user, image);
    }

    /**
     * Checks if a user can update an image.
     *
     * @param user  The user.
     * @param image The image.
     * @return True if the user can update the image, false otherwise.
     */
    public boolean canUpdate(User user, Image image) {
        return isOwnerOrAdmin(user, image);
    }

    /**
     * Checks if a user can delete an image.
     *
     * @param user  The user.
     * @param image The image.
     * @return True if the user can delete the image, false otherwise.
     */
    public boolean canDelete(User user, Image image) {
        return isOwnerOrAdmin(user, image);
    }
}
