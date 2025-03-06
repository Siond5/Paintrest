package com.example.login.Classes;

public class Image {
    private String profileImage;
    private String profileImageUri;

    public Image() {
    }

    public Image(String profileImage, String profileImageUri) {
        this.profileImage = profileImage;
        this.profileImageUri = profileImageUri;
    }

    public String getProfileImage() {return profileImage;}

    public void setProfileImage(String profileImage) {this.profileImage = profileImage;}

    public String getProfileImageUri() {return profileImageUri;}

    public void setProfileImageUri(String profileImageUri) {this.profileImageUri = profileImageUri;}
}

