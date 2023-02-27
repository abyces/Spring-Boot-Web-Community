package com.nowcoder.community.util;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * class that hold forgettign passwords' authorization codes
 */
@Component
public class AuthCodeHolder {
    private Map<String, String> authorizationCodes = new HashMap<>();


    public void setCode(String email, String code) {
        authorizationCodes.put(email, code);
    }

    public String getAuthCode(String email) {
        return authorizationCodes.get(email);
    }

    public void clear(String email) {
        authorizationCodes.remove(email);
    }

}
