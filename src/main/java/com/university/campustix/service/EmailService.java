package com.university.campustix.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

@Service
public class EmailService {

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${spring.mail.password}")
    private String appPassword;

    public void sendBookingConfirmation(String toEmail, String name, String eventName,
                                        String seatNum, String venue, String time, String qrCodeBase64) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, appPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Your CampusTix Ticket: " + eventName);

            String qrBlock = qrCodeBase64 != null
                ? "<div style='text-align:center;margin:24px 0;'>" +
                  "<p style='color:#94a3b8;font-size:12px;margin-bottom:12px;text-transform:uppercase;letter-spacing:2px;'>SCAN TO VERIFY</p>" +
                  "<img src='data:image/png;base64," + qrCodeBase64 + "' style='width:180px;height:180px;border-radius:12px;border:2px solid #4f46e5;'>" +
                  "</div>"
                : "";

            String htmlContent = String.format(
                "<div style='background:#06081C;color:white;padding:40px;font-family:\"Inter\",sans-serif;max-width:520px;margin:0 auto;border-radius:24px;'>" +
                "<div style='text-align:center;margin-bottom:28px;'>" +
                "<span style='background:#4f46e5;color:white;padding:8px 20px;border-radius:50px;font-size:13px;font-weight:800;letter-spacing:3px;text-transform:uppercase;'>Campus Tix</span>" +
                "</div>" +
                "<div style='background:#0E1028;border:1px solid #1e2d5a;border-radius:20px;padding:28px;margin-bottom:20px;'>" +
                "<p style='color:#818cf8;font-size:11px;font-weight:800;text-transform:uppercase;letter-spacing:3px;margin:0 0 8px;'>Booking Confirmed</p>" +
                "<h2 style='color:white;font-size:22px;font-weight:800;margin:0 0 20px;'>%s</h2>" +
                "<table style='width:100%%;border-collapse:collapse;'>" +
                "<tr><td style='color:#64748b;font-size:12px;padding:8px 0;border-bottom:1px solid #1e2d5a;'>ATTENDEE</td><td style='color:white;font-size:13px;font-weight:600;padding:8px 0;border-bottom:1px solid #1e2d5a;text-align:right;'>%s</td></tr>" +
                "<tr><td style='color:#64748b;font-size:12px;padding:8px 0;border-bottom:1px solid #1e2d5a;'>SEAT</td><td style='color:#818cf8;font-size:18px;font-weight:900;padding:8px 0;border-bottom:1px solid #1e2d5a;text-align:right;'>%s</td></tr>" +
                "<tr><td style='color:#64748b;font-size:12px;padding:8px 0;border-bottom:1px solid #1e2d5a;'>VENUE</td><td style='color:white;font-size:13px;font-weight:600;padding:8px 0;border-bottom:1px solid #1e2d5a;text-align:right;'>%s</td></tr>" +
                "<tr><td style='color:#64748b;font-size:12px;padding:8px 0;'>DATE & TIME</td><td style='color:white;font-size:13px;font-weight:600;padding:8px 0;text-align:right;'>%s</td></tr>" +
                "</table>" +
                "</div>" +
                "%s" +
                "<p style='color:#475569;font-size:11px;text-align:center;margin-top:20px;'>Present this QR code at the venue entrance. Keep this email as your receipt.</p>" +
                "</div>",
                eventName, name, seatNum, venue, time, qrBlock
            );

            message.setContent(htmlContent, "text/html");
            Transport.send(message);
            System.out.println("Ticket dispatched to: " + toEmail);

        } catch (MessagingException e) {
            System.err.println("Email failed: " + e.getMessage());
        }
    }
}
