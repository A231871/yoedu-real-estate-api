package com.yoedu.yoedurealestateapi.service.impl;

import com.yoedu.yoedurealestateapi.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Override
    public void sendEmail(String to, String subject, String body) {
        log.info("[EMAIL DISPATCH] Sending email to: {}, Subject: '{}', Content snippet: '{}'",
                to, subject, body.length() > 100 ? body.substring(0, 100) + "..." : body);
        // Simulation / integration point for JavaMailSender or HTML template builder
    }
}
