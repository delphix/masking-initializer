package com.delphix.masking.initializer.pojo.apiBody;

/**
 * Created by bpage on 5/21/17.
 */
public class LoginApiBody {

    String username;
    String password;

    LoginApiBody() {

    }

    public LoginApiBody(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
