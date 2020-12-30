package com.tng.invitation.service;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class InvitationsValidationService {

    public boolean isFirstNamePresent(String firstName){
        return !firstName.isBlank();
    }
    public boolean isLastNamePresent(String lastName){
        return !lastName.isBlank();
    }

    public boolean isEmailPresent(String email){
        return !email.isBlank();
    }

    public boolean isProvinceStateValid(String provinceState){
        return (provinceState.length()==2) || (provinceState.length()<1);
    }

    public boolean isValidEmail(String email){
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\."+
                "[a-zA-Z0-9_+&*-]+)*@" +
                "(?:[a-zA-Z0-9-]+\\.)+[a-z" +
                "A-Z]{2,7}$";

        Pattern pat = Pattern.compile(emailRegex);
        return pat.matcher(email).matches();
    }



}
