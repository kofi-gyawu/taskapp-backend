package com.awsmicroservices.userservice.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private String id;
    private String username;
//    private String role;
}
