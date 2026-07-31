package com.yoedu.yoedurealestateapi.security.password;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {

        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigit = false;

        for (char c : password.toCharArray()) {
            if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }

            if (hasLower && hasUpper && hasDigit) {
                return true;
            }
        }

        context.disableDefaultConstraintViolation();

        if (!hasLower) {
            context.buildConstraintViolationWithTemplate(
                    "Password must contain at least one lowercase letter")
                    .addConstraintViolation();
        }

        if (!hasUpper) {
            context.buildConstraintViolationWithTemplate(
                    "Password must contain at least one uppercase letter")
                    .addConstraintViolation();
        }

        if (!hasDigit) {
            context.buildConstraintViolationWithTemplate(
                    "Password must contain at least one digit")
                    .addConstraintViolation();
        }

        return false;
    }
}
