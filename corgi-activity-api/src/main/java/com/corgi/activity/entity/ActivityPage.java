package com.corgi.activity.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ActivityPage implements Serializable {
    List<CorgiActivity> corgiActivityList;
    Integer tPage;
    Integer dPage;
}
