package com.pgrdaw.tagfolio.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a user of the application.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
@ToString(exclude = {"images", "roles", "filters"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false, unique = true)
    private String email;

    @Setter
    @Column(nullable = false)
    private String password;

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<Image> images = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_has_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private final Set<Role> roles = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<Filter> filters = new HashSet<>();

    /**
     * Constructs a new User with the specified email and password.
     *
     * @param email    The user's email address.
     * @param password The user's password.
     */
    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    /**
     * Adds a role to the user.
     *
     * @param role The role to add.
     */
    public void addRole(Role role) {
        this.roles.add(role);
    }

    /**
     * Checks if the user has a specific role.
     *
     * @param roleType The type of the role to check for.
     * @return True if the user has the role, false otherwise.
     */
    public boolean hasRole(RoleType roleType) {
        return this.roles.stream().anyMatch(role -> role.getName().equals(roleType));
    }

    /**
     * Checks if the user is an administrator.
     *
     * @return True if the user has the ADMIN role, false otherwise.
     */
    public boolean isAdmin() {
        return hasRole(RoleType.ADMIN);
    }

    /**
     * Checks if the user is a regular user.
     *
     * @return True if the user has the USER role, false otherwise.
     */
    public boolean isUser() {
        return hasRole(RoleType.USER);
    }

    /**
     * Checks if the user is anonymous.
     *
     * @return True if the user has the ANONYMOUS role, false otherwise.
     */
    public boolean isAnonymous() {
        return hasRole(RoleType.ANONYMOUS);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        if (this.id == null) {
            return Objects.equals(email, user.email);
        } else {
            return Objects.equals(id, user.id);
        }
    }

    @Override
    public int hashCode() {
        if (this.id == null) {
            return Objects.hash(email);
        } else {
            return Objects.hash(id);
        }
    }
}
