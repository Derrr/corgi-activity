package com.corgi.activity.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author tairanliu
 */
@Data
public class CorgiActivity implements Serializable {
    private String id;
    private String title;
    private String content;
    private String activityType;
    private String address;
    private String enlistTime;
    private int peopleCount;
    private double lat;
    private double lng;
    private String payType;
    private Integer budget;
    private List<String> pics;
}
