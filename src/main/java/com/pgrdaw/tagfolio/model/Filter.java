package com.pgrdaw.tagfolio.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a saved filter created by a user.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Entity
@Table(name = "filters")
@Getter
@Setter
@NoArgsConstructor
public class Filter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Lob
    @Column(nullable = false, columnDefinition = "CLOB")
    private String expression;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(mappedBy = "filter", cascade = CascadeType.ALL, orphanRemoval = true)
    private SharedFilter sharedFilter;

    /**
     * Constructs a new Filter with the specified name, expression, and user.
     *
     * @param name       The name of the filter.
     * @param expression The filter expression as a JSON string.
     * @param user       The user who created the filter.
     */
    public Filter(String name, String expression, User user) {
        this.name = name;
        this.expression = expression;
        this.user = user;
    }
}
