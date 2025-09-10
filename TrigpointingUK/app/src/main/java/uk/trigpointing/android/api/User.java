package uk.trigpointing.android.api;

/**
 * User data model for the new API authentication response
 */
public class User {
    private int id;
    private String name;
    private String firstname;
    private String surname;
    private String about;
    private String email;
    private String email_valid;
    private String admin_ind;
    private String public_ind;
    private String auth0_user_id;
    private String auth0_username;

    // Default constructor required for deserialization
    public User() {}

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmailValid() {
        return email_valid;
    }

    public void setEmailValid(String email_valid) {
        this.email_valid = email_valid;
    }

    public String getAdminInd() {
        return admin_ind;
    }

    public void setAdminInd(String admin_ind) {
        this.admin_ind = admin_ind;
    }

    public String getPublicInd() {
        return public_ind;
    }

    public void setPublicInd(String public_ind) {
        this.public_ind = public_ind;
    }

    public String getAuth0UserId() {
        return auth0_user_id;
    }

    public void setAuth0UserId(String auth0_user_id) {
        this.auth0_user_id = auth0_user_id;
    }

    public String getAuth0Username() {
        return auth0_username;
    }

    public void setAuth0Username(String auth0_username) {
        this.auth0_username = auth0_username;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", firstname='" + firstname + '\'' +
                ", surname='" + surname + '\'' +
                ", email='" + email + '\'' +
                ", admin_ind='" + admin_ind + '\'' +
                ", auth0_user_id='" + auth0_user_id + '\'' +
                ", auth0_username='" + auth0_username + '\'' +
                '}';
    }
}
