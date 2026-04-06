package com.remindledger.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "cognito_sub", nullable = false, unique = true)
    private String cognitoSub;

    @Column
    private String email;

    @Column(name = "display_name")
    private String displayName;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
 * No-argument constructor required by JPA for entity instantiation.
 *
 * Intended for use by JPA and other frameworks that create entities via reflection.
 */
public User() {}

    /**
     * Creates a new User with the specified Cognito subject
     *
     * @param cognitoSub the Cognito subject (unique identifier) for the user
     */
    public User(String cognitoSub) {
        this.cognitoSub = cognitoSub;
    }

    /**
     * Creates a new User with the specified Cognito subject, email, and display name.
     *
     * @param cognitoSub the Cognito subject (unique identifier) for the user
     * @param email the user's email address
     * @param displayName the user's display name
     */
    public User(String cognitoSub, String email, String displayName) {
        this.cognitoSub = cognitoSub;
        this.email = email;
        this.displayName = displayName;
    }

    /**
     * Get the entity's primary key.
     *
     * @return the user's primary key UUID
     */
    public UUID getId() { return id; }

    /**
     * Retrieves the Cognito subject identifier for the user.
     *
     * @return the Cognito `sub` value that uniquely identifies the user
     */
    public String getCognitoSub() { return cognitoSub; }

    /**
     * Gets the user's email address.
     *
     * @return the user's email address
     */
    public String getEmail() { return email; }
    /**
     * Set the user's email address.
     *
     * @param email the new email address
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * Gets the user's display name.
     *
     * @return the display name, or `null` if not set
     */
    public String getDisplayName() { return displayName; }
    /**
     * Sets the user's display name.
     *
     * @param displayName the display name to assign to the user
     */
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    /**
     * The creation timestamp for this user, set by JPA auditing.
     *
     * @return the creation timestamp, or `null` if it has not been set (e.g., before persistence)
     */
    public Instant getCreatedAt() { return createdAt; }

    /**
     * Timestamp when the user was last modified.
     *
     * Populated by JPA auditing; may be null before the entity is persisted.
     *
     * @return the last modification timestamp, or `null` if not set
     */
    public Instant getUpdatedAt() { return updatedAt; }
}
