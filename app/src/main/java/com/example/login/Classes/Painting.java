package com.example.login.Classes;

import java.io.Serializable;
import java.util.List;

public class Painting implements Serializable {
    private String imageUrl;
    private String docId;
    private String uid;
    private String name;
    private String description;
    private String authorName;
    private long creationTime;
    private int likes;
    private List<String> likedBy;
    private boolean isAnonymous;

    // Full constructor updated with isAnonymous and authorName.
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
    public String getImageUrl() { return imageUrl; }
    public String getDocId() { return docId; }
    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getAuthorName() { return authorName; }
    public long getCreationTime() { return creationTime; }
    public int getLikes() { return likes; }
    public List<String> getLikedBy() { return likedBy; }
    public boolean getIsAnonymous() { return isAnonymous; }


    // Setters
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setDocId(String docId) { this.docId = docId; }
    public void setUid(String uid) { this.uid = uid; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public void setCreationTime(long creationTime) { this.creationTime = creationTime; }
    public void setLikes(int likes) { this.likes = likes; }
    public void setLikedBy(List<String> likedBy) { this.likedBy = likedBy; }
    public void setIsAnonymous(boolean isAnonymous) { this.isAnonymous = isAnonymous; }

}
