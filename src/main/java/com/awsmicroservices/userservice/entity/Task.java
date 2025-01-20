package com.awsmicroservices.userservice.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
public class Task {
    public String id;
    public String name;
    public String description;
    public String status;
    public LocalDateTime deadline;
    public String responsibility;
    public LocalDateTime completed_at;

    @Override
    public String toString() {
        Map<String,String> map = new HashMap<>();
        map.put("id",this.id);
        map.put("name",this.name);
        map.put("status", this.status);
        map.put("deadline", this.deadline.toString());
        map.put("responsibility",this.responsibility);
        map.put("completed_at",this.completed_at.toString());
        String representation = map.toString();
        return representation.substring(1,representation.length() -2);
    }
}
