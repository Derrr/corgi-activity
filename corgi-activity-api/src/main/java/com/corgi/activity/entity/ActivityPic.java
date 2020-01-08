package com.corgi.activity.entity;

import com.corgi.entity.CorgiPic;
import lombok.Data;

import java.io.Serializable;

/**
 * @author tairanliu
 */
@Data
public class ActivityPic extends CorgiPic{
    private String activityId;
}
