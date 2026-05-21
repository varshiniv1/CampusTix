package com.university.campustix.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

@Service
public class EmailService {

    // Pulls from application.properties
    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${spring.mail.password}")
    private String appPassword;

    public void sendBookingConfirmation(String toEmail, String name, String eventName, String seatNum, String venue, String time) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        // Authenticate the Sender (Post Office)
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, appPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));

            // DYNAMIC RECIPIENT: Set from the method parameter
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));

            message.setSubject("🎟️ Your CampusTix: " + eventName);

            String htmlContent = String.format(
                    "<div style='background:#0b0f1a; color:white; padding:40px; border-radius:30px; font-family:sans-serif; text-align:center; border: 1px solid #1e293b;'>" +
                            "<h1 style='color:#6366f1;'>CAMPUS TIX</h1>" +
                            "<div style='background:#1e293b; padding:30px; border-radius:20px; margin:20px 0; border-left:8px solid #6366f1; text-align:left;'>" +
                            "<h2>%s</h2>" +
                            "<p><b>ATTENDEE:</b> %s</p>" +
                            "<p><b>SEAT:</b> <span style='font-size:24px; color:#6366f1;'>%s</span></p>" +
                            "<p><b>LOCATION:</b> %s</p>" +
                            "<p><b>TIME:</b> %s</p>" +
                            "</div>" +
                            "</div>",
                    eventName, name, seatNum, venue, time
            );

            message.setContent(htmlContent, "text/html");
            Transport.send(message);
            System.out.println("Ticket sent dynamically to: " + toEmail);

        } catch (MessagingException e) {
            throw new RuntimeException("Email failed: " + e.getMessage());
        }
    }
}