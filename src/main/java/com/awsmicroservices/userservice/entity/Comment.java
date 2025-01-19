package com.awsmicroservices.userservice.entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class Comment {
    private long id;
    private long taskId;
    private String sender;
    private String body;
    @Builder.Default
    private LocalDateTime timeStamp = LocalDateTime.now();
}
