package com.example.login.Classes;

import java.io.Serializable;

public class MyUser  implements Serializable {
    private String firstName;
    private String lastName;
    private String phone;
    private int yob;

    public MyUser() {
    }

    public MyUser(String firstName, String lastName, String phone, int yob) {

        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.yob = yob;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getYob() {
        return yob;
    }

    public void setYob(int yob) {
        this.yob = yob;
    }

}
