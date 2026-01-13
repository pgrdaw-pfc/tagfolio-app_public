package com.pgrdaw.tagfolio.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents a shared filter, allowing public access to a user's filter.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Entity
@Table(name = "shared_filters")
@Getter
@Setter
@NoArgsConstructor
public class SharedFilter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 36)
    private String token;

    @Column(unique = true, nullable = false, length = 64)
    private String contentHash;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filter_id", nullable = false, unique = true)
    private Filter filter;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime expiresAt;

    /**
     * Constructs a new SharedFilter.
     *
     * @param token       The unique token for the shareable URL.
     * @param contentHash The SHA-256 hash of the filter's expression for deduplication.
     * @param filter      The filter being shared.
     * @param createdAt   The timestamp when the shared filter was created.
     */
    public SharedFilter(String token, String contentHash, Filter filter, LocalDateTime createdAt) {
        this.token = token;
        this.contentHash = contentHash;
        this.filter = filter;
        this.createdAt = createdAt;
    }
}
