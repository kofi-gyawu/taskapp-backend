package com.awsmicroservices.userservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class Task {
    public String id;
    public String name;
    public String description;
    public String status;
    public LocalDateTime deadline;
    public String responsibility;
    public LocalDateTime completed_at;
    public String comment;

    @Override
    public String toString() {
        Map<String,String> map = new HashMap<>();
        map.put("id",this.id);
        map.put("name",this.name);
        map.put("status", this.status);
        map.put("deadline", this.deadline.toString());
        map.put("responsibility",this.responsibility);
        if(this.completed_at != null){map.put("completed_at",this.completed_at.toString());}
        String representation = map.toString();
        return representation.substring(1,representation.length() -2);
    }
}
