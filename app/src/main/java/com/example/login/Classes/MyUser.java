package com.example.login.Classes;

import java.io.Serializable;

/**
 * Represents a user with basic information: first name, last name, phone number, and year of birth.
 */
public class MyUser implements Serializable {
    private String firstName;
    private String lastName;
    private String phone;
    private int yob;

    /**
     * Default constructor for MyUser.
     */
    public MyUser() {
    }

    /**
     * Constructs a MyUser object with the specified details.
     *
     * @param firstName The user's first name.
     * @param lastName  The user's last name.
     * @param phone     The user's phone number.
     * @param yob       The user's year of birth.
     */
    public MyUser(String firstName, String lastName, String phone, int yob) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.yob = yob;
    }

    /**
     * Gets the user's first name.
     *
     * @return The first name.
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the user's first name.
     *
     * @param firstName The first name to set.
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Gets the user's last name.
     *
     * @return The last name.
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the user's last name.
     *
     * @param lastName The last name to set.
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Gets the user's phone number.
     *
     * @return The phone number.
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Sets the user's phone number.
     *
     * @param phone The phone number to set.
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Gets the user's year of birth.
     *
     * @return The year of birth.
     */
    public int getYob() {
        return yob;
    }

    /**
     * Sets the user's year of birth.
     *
     * @param yob The year of birth to set.
     */
    public void setYob(int yob) {
        this.yob = yob;
    }
}