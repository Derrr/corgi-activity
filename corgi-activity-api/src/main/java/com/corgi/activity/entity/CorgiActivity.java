package com.corgi.activity.entity;

import com.corgi.entity.CorgiTopic;
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
    public static final String CAT_VIDEO = "video";
    public static final String CAT_TEXT = "text";
    public static final String CAT_USER = "user";
    public static final String CAT_GOODS = "goods";

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
    private String addressDetail;
    private String signUpTime;
    private int peopleCount;
    private double lat;
    private double lng;
    private String city;
    private Long height;
    private Long width;
    private String payType;
    private Integer budget;
    private Long likeCount;
    private String createTime;
    private String updateTime;
    private String currentTime;
    private String startTime;
    private String endTime;
    private String status;
    private String checkStatus;
    private String recommend;
    private String refActivityId;
    private String refActivityPic;
    private String refActivityAddress;
    private String refActivityTitle;
    private String videoUrl;
    private String coverUrl;
    private String videoId;
    private List<ActivityPic> pics;
    private List<String> topics;
    private List<CorgiTopic> topicDetails;


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
