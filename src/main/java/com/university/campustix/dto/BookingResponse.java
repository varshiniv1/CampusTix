package com.university.campustix.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookingResponse {
    private Long id;
    private String bookingReference;
    private String eventName;
    private String eventImageUrl;
    private String venue;
    private String eventTime;
    private String seatNumber;
    private String status;
    private String qrCodeBase64;
    private String bookedAt;
    private String buyerName;
}
