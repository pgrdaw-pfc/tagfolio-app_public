package com.pgrdaw.tagfolio.service.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

/**
 * A service to track the status of the database seeding process.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Setter
@Getter
@Service
public class SeedingStatusService {

    private volatile boolean seedingComplete = false;

}
