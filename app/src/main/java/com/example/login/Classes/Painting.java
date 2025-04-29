package com.example.login.Classes;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a painting uploaded by a user, including metadata like description,
 * creation time, likes, and anonymous posting option.
 */
public class Painting implements Serializable {
    /** URL of the painting image stored online. */
    private String imageUrl;

    /** Document ID associated with the painting (e.g., in a database). */
    private String docId;

    /** UID of the user who uploaded the painting. */
    private String uid;

    /** Name or title of the painting. */
    private String name;

    /** Description of the painting. */
    private String description;

    /** Name of the author (may be hidden if posted anonymously). */
    private String authorName;

    /** Timestamp representing when the painting was created (in milliseconds). */
    private long creationTime;

    /** Number of likes the painting has received. */
    private int likes;

    /** List of user IDs who liked the painting. */
    private List<String> likedBy;

    /** Indicates whether the painting was posted anonymously. */
    private boolean isAnonymous;

    /**
     * Constructs a new Painting object with all fields initialized.
     *
     * @param imageUrl     The URL of the painting image.
     * @param docId        The document ID of the painting.
     * @param uid          The UID of the uploader.
     * @param name         The title of the painting.
     * @param description  A description of the painting.
     * @param creationTime The time the painting was created.
     * @param likes        The number of likes.
     * @param isAnonymous  Whether the painting is posted anonymously.
     * @param authorName   The name of the author.
     */
    public Painting(String imageUrl, String docId, String uid, String name, String description,
                    long creationTime, int likes, boolean isAnonymous, String authorName) {
        this.imageUrl = imageUrl;
        this.docId = docId;
        this.uid = uid;
        this.name = name;
        this.description = description;
        this.creationTime = creationTime;
        this.likes = likes;
        this.isAnonymous = isAnonymous;
        this.authorName = authorName;
    }

    // Getters

    /** @return The URL of the painting image. */
    public String getImageUrl() { return imageUrl; }

    /** @return The document ID of the painting. */
    public String getDocId() { return docId; }

    /** @return The UID of the uploader. */
    public String getUid() { return uid; }

    /** @return The title of the painting. */
    public String getName() { return name; }

    /** @return The description of the painting. */
    public String getDescription() { return description; }

    /** @return The name of the author. */
    public String getAuthorName() { return authorName; }

    /** @return The creation time of the painting in milliseconds. */
    public long getCreationTime() { return creationTime; }

    /** @return The number of likes. */
    public int getLikes() { return likes; }

    /** @return A list of user IDs who liked the painting. */
    public List<String> getLikedBy() { return likedBy; }

    /** @return True if posted anonymously, false otherwise. */
    public boolean getIsAnonymous() { return isAnonymous; }

    // Setters

    /** @param imageUrl The new URL for the painting image. */
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    /** @param docId The new document ID for the painting. */
    public void setDocId(String docId) { this.docId = docId; }

    /** @param uid The new UID of the uploader. */
    public void setUid(String uid) { this.uid = uid; }

    /** @param name The new title for the painting. */
    public void setName(String name) { this.name = name; }

    /** @param description The new description for the painting. */
    public void setDescription(String description) { this.description = description; }

    /** @param authorName The new author name. */
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    /** @param creationTime The new creation time in milliseconds. */
    public void setCreationTime(long creationTime) { this.creationTime = creationTime; }

    /** @param likes The new number of likes. */
    public void setLikes(int likes) { this.likes = likes; }

    /** @param likedBy The new list of user IDs who liked the painting. */
    public void setLikedBy(List<String> likedBy) { this.likedBy = likedBy; }

    /** @param isAnonymous Sets whether the painting is posted anonymously. */
    public void setIsAnonymous(boolean isAnonymous) { this.isAnonymous = isAnonymous; }
}