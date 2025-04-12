package com.example.login.Classes;

import java.io.Serializable;

public class Painting implements Serializable {
    private String imageUrl;
    private String docId;
    private String name;           // Painting name
    private String description;    // Painting description
    private long creationTime;     // Creation timestamp in milliseconds
    private int likes;             // Number of likes

    // Full constructor
    public Painting(String imageUrl, String docId, String name, String description, long creationTime, int likes) {
        this.imageUrl = imageUrl;
        this.docId = docId;
        this.name = name;
        this.description = description;
        this.creationTime = creationTime;
        this.likes = likes;
    }

    // Getters
    public String getImageUrl() {
        return imageUrl;
    }
    public String getDocId() {
        return docId;
    }
    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
    public long getCreationTime() {
        return creationTime;
    }
    public int getLikes() {
        return likes;
    }

    // Setters
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    public void setDocId(String docId) {
        this.docId = docId;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }
    public void setLikes(int likes) {
        this.likes = likes;
    }
}
