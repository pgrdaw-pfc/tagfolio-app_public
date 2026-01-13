package com.pgrdaw.tagfolio.service.util;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * A service for calculating hash values.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Service
public class HashService {

    /**
     * Calculates the SHA-256 hash of a given string.
     *
     * @param text The input string to hash.
     * @return The SHA-256 hash as a hexadecimal string.
     * @throws IllegalStateException if the SHA-256 algorithm is not found.
     */
    public String calculateSha256Hash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not found. This should not happen in a standard Java environment.", e);
        }
    }
}
