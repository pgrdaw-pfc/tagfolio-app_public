package com.pgrdaw.tagfolio.service.util;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

/**
 * A service that provides information about allowed filter fields.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Service
public class FilterFieldService {

    private static final Set<String> ALLOWED_FIELDS;

    static {
        Set<String> fields = new HashSet<>();
        fields.add("rating");
        fields.add("createdat");
        fields.add("updatedat");
        fields.add("filemodifiedat");
        fields.add("originalfilename");
        fields.add("created");
        fields.add("modified");
        fields.add("imported");
        ALLOWED_FIELDS = Collections.unmodifiableSet(fields);
    }

    /**
     * Gets the set of allowed filter fields.
     *
     * @return An unmodifiable set of allowed filter fields.
     */
    public Set<String> getAllowedFields() {
        return ALLOWED_FIELDS;
    }
}
