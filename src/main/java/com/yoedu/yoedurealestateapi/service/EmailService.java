package com.yoedu.yoedurealestateapi.service;

public interface EmailService {

    // TODO: Send text email for now. Can use HTML template later
    void sendEmail(String to, String subject, String body);
}
