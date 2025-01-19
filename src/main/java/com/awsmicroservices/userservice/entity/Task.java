package com.awsmicroservices.userservice.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Task {
    public String id;
    public String name;
    public String description;
    public String status;
    public LocalDateTime deadline;
    public String responsibility;
    public LocalDateTime completed_at;
}
