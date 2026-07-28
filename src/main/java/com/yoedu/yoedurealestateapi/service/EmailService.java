package com.yoedu.yoedurealestateapi.service;

public interface EmailService {
    void sendEmail(String to, String subject, String body);
}
