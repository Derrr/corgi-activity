package com.corgi.activity.api;

import com.corgi.activity.entity.CorgiActivity;

import java.util.List;

/**
 * @author tairanliu
 */
public interface CorgiActivityService {
    CorgiActivity addCorgiActivity(CorgiActivity corgiActivity);

    List<CorgiActivity> getCorgiActivityByRange(double lng, double lat, double range, String type);

    List<CorgiActivity> getUserRunningActivity(String userId);

    List<CorgiActivity> getUserEndedActivity(String userId);

    List<CorgiActivity> getActivityByIds(List<String> activityIds);

    CorgiActivity updateCorgiActivity(CorgiActivity corgiActivity);

    CorgiActivity deleteCorgiActivity(String activityId);

    List<CorgiActivity> searchCorgiActivity(CorgiActivity activity);
}
