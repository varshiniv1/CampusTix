package com.university.campustix.service;

import com.university.campustix.model.Booking;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
public class PdfTicketService {

    private static final float W = 595f;
    private static final float H = 260f;

    public byte[] generate(Booking booking) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(W, H));
            doc.addPage(page);

            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font reg  = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font obl  = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                // Main background
                fill(cs, 0.024f, 0.031f, 0.11f,  0, 0, W, H);

                // Left accent
                fill(cs, 0.31f, 0.275f, 0.898f,  0, 0, 7, H);

                // Header strip
                fill(cs, 0.055f, 0.063f, 0.16f,  7, 210, W - 7, 50);

                // CAMPUSTIX
                text(cs, bold, 20, 1f, 1f, 1f,   22, 228);
                cs.showText("CAMPUSTIX"); cs.endText();

                // TICKET badge
                fill(cs, 0.31f, 0.275f, 0.898f, 128, 222, 46, 16);
                text(cs, bold, 7, 1f, 1f, 1f, 132, 228);
                cs.showText("TICKET"); cs.endText();

                // Booking ref (top right)
                text(cs, reg, 8, 0.45f, 0.45f, 0.65f, 360, 228);
                cs.showText("REF: " + booking.getBookingReference()); cs.endText();

                // Separator
                line(cs, 0.1f, 0.13f, 0.3f, 18, 208, W - 18, 208, 0.5f, null);

                // Event name
                String evName = truncate(booking.getEvent().getName(), 40);
                text(cs, bold, 16, 1f, 1f, 1f, 18, 185);
                cs.showText(evName); cs.endText();

                // Category
                if (booking.getEvent().getCategory() != null) {
                    text(cs, bold, 8, 0.51f, 0.69f, 1f, 18, 170);
                    cs.showText(booking.getEvent().getCategory().toUpperCase()); cs.endText();
                }

                // Venue + datetime
                String venue = orTbd(booking.getEvent().getVenue());
                String dateStr = booking.getEvent().getEventTime() != null
                    ? booking.getEvent().getEventTime()
                        .format(DateTimeFormatter.ofPattern("EEE MMM d, yyyy  ·  h:mm a"))
                    : "Date TBD";
                text(cs, reg, 9, 0.65f, 0.7f, 0.8f, 18, 155);
                cs.showText(venue); cs.endText();
                text(cs, reg, 9, 0.65f, 0.7f, 0.8f, 18, 141);
                cs.showText(dateStr); cs.endText();

                // Divider (dashed)
                line(cs, 0.12f, 0.15f, 0.32f, 18, 122, 390, 122, 0.5f, new float[]{4, 4});

                // ATTENDEE block
                label(cs, bold, reg, "ATTENDEE", booking.getBuyerName(), 18, 110, 95);
                text(cs, reg, 8, 0.4f, 0.45f, 0.6f, 18, 83);
                cs.showText(booking.getBuyerEmail()); cs.endText();

                // SEAT block
                label(cs, bold, bold, "SEAT", booking.getSeat().getSeatNumber(), 180, 110, 86);
                // Override seat number color/size
                text(cs, bold, 26, 0.51f, 0.51f, 0.9f, 180, 83);
                cs.showText(booking.getSeat().getSeatNumber()); cs.endText();

                // STATUS block
                boolean confirmed = "CONFIRMED".equals(booking.getStatus());
                label(cs, bold, reg, "STATUS", "", 290, 110, 95);
                text(cs, bold, 12, confirmed ? 0.2f : 0.9f, confirmed ? 0.85f : 0.3f, confirmed ? 0.4f : 0.3f, 290, 95);
                cs.showText(booking.getStatus()); cs.endText();

                // PRICE block
                if (booking.getEvent().getPrice() != null) {
                    label(cs, bold, reg, "PRICE PAID", "", 350, 110, 95);
                    text(cs, bold, 14, 0.2f, 0.85f, 0.45f, 350, 93);
                    cs.showText(String.format("$%.2f", booking.getEvent().getPrice())); cs.endText();
                }

                // Footer
                text(cs, obl, 7, 0.3f, 0.33f, 0.5f, 18, 14);
                cs.showText("Present at venue entrance · Keep as receipt · campustix.io");
                cs.endText();

                // QR panel
                if (booking.getQrCodeBase64() != null) {
                    fill(cs, 0.055f, 0.063f, 0.16f,  W - 185, 0, 185, H);
                    line(cs, 0.12f, 0.15f, 0.32f,  W - 185, 18,  W - 185, H - 18, 0.5f, new float[]{4, 4});

                    byte[] qrBytes = Base64.getDecoder().decode(booking.getQrCodeBase64());
                    PDImageXObject qr = PDImageXObject.createFromByteArray(doc, qrBytes, "qr");
                    cs.drawImage(qr, W - 178, 50, 155, 155);

                    text(cs, bold, 8, 0.38f, 0.43f, 0.58f, W - 172, 226);
                    cs.showText("SCAN TO ENTER"); cs.endText();

                    text(cs, reg, 7, 0.38f, 0.43f, 0.58f, W - 172, 34);
                    cs.showText(booking.getBookingReference()); cs.endText();
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private void fill(PDPageContentStream cs, float r, float g, float b,
                      float x, float y, float w, float h) throws IOException {
        cs.setNonStrokingColor(r, g, b);
        cs.addRect(x, y, w, h);
        cs.fill();
    }

    private void text(PDPageContentStream cs, PDType1Font font, float size,
                      float r, float g, float b, float x, float y) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(r, g, b);
        cs.newLineAtOffset(x, y);
    }

    private void label(PDPageContentStream cs, PDType1Font labelFont, PDType1Font valueFont,
                       String label, String value, float x, float labelY, float valueY) throws IOException {
        text(cs, labelFont, 7, 0.38f, 0.43f, 0.58f, x, labelY);
        cs.showText(label); cs.endText();
        if (!value.isEmpty()) {
            text(cs, valueFont, 12, 1f, 1f, 1f, x, valueY);
            cs.showText(value); cs.endText();
        }
    }

    private void line(PDPageContentStream cs, float r, float g, float b,
                      float x1, float y1, float x2, float y2, float width,
                      float[] dash) throws IOException {
        cs.setStrokingColor(r, g, b);
        cs.setLineWidth(width);
        if (dash != null) cs.setLineDashPattern(dash, 0);
        cs.moveTo(x1, y1); cs.lineTo(x2, y2); cs.stroke();
        if (dash != null) cs.setLineDashPattern(new float[]{}, 0);
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "…" : (s != null ? s : "");
    }

    private String orTbd(String s) { return s != null ? s : "TBD"; }
}
