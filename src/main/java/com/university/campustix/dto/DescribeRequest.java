package com.university.campustix.dto;

import lombok.Data;

@Data
public class DescribeRequest {
    private String eventName;
    private String venue;
    private String category;
    private Double price;
}
