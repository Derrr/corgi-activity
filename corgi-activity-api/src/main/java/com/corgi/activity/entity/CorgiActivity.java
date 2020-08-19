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
    public static final String FULL = "full";
    public static final String NOT_DELETED = "not_deleted";

    public static final String CAT_IMAGE = "image";
    public static final String CAT_ACTIVITY = "activity";
    public static final String CAT_BUSINESS = "business";
    public static final String CAT_ATTENDANCE = "attendance";

    private String id;
    private String barId;
    private String title;
    private String checkTitle;
    private String content;
    private String checkContent;
    private String category;
    private String activityType;
    private String checkActivityType;
    private String userId;
    private String address;
    private String signUpTime;
    private int peopleCount;
    private double lat;
    private double lng;
    private String city;
    private String adname;
    private String businessArea;
    private String station;
    private String payType;
    private Integer budget;
    private String createTime;
    private String updateTime;
    private String currentTime;
    private String startTime;
    private String endTime;
    private String status;
    private String checkStatus;
    private String recommend;
    private List<ActivityPic> pics;
    private List<String> topics;


    public String getStatus() {
        if (!DELETED.equals(status)
                && currentTime != null
                && signUpTime != null
                && currentTime.compareTo(signUpTime) >= 0) {
            return ENDED;
        }
        return status;
    }
}
