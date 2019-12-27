package com.corgi.activity.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author tairanliu
 */
@Data
public class CorgiActivity implements Serializable {
    public static final String CREATED = "created";
    public static final String ENDED = "ended";
    public static final String DELETED = "deleted";

    private String id;
    private String title;
    private String content;
    private String activityType;
    private String userId;
    private String address;
    private String signUpTime;
    private int peopleCount;
    private double lat;
    private double lng;
    private String payType;
    private Integer budget;
    private String createTime;
    private String updateTime;
    private String currentTime;
    private String status;
    private List<String> pics;


    public String getStatus() {
        if (!DELETED.equals(status) && currentTime != null && currentTime.compareTo(signUpTime) >= 0) {
            return ENDED;
        }
        return status;
    }
}
