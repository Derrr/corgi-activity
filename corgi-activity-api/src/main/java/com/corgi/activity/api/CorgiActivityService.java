package com.corgi.activity.api;

import com.corgi.activity.entity.ActivityPage;
import com.corgi.activity.entity.CorgiActivity;
import com.corgi.entity.ActivityQuery;

import java.util.HashMap;
import java.util.List;

/**
 * @author tairanliu
 */
public interface CorgiActivityService {
    CorgiActivity addCorgiActivity(CorgiActivity corgiActivity);

    List<CorgiActivity> getCityCorgiActivityByRange(double lng, double lat, double range, ActivityQuery activityQuery);

    List<CorgiActivity> getCorgiActivityByRange(double lng, double lat, double range, ActivityQuery activityQuery);

    List<CorgiActivity> getUserRunningActivity(String userId, Integer page, Integer size);

    List<CorgiActivity> getUserAllRunningActivity(String userId, Integer page, Integer size);

    List<CorgiActivity> getUserEndedActivity(String userId, Integer page, Integer size);

    List<CorgiActivity> getActivityByIds(List<String> activityIds);

    CorgiActivity updateCorgiActivity(CorgiActivity corgiActivity);

    void updateCorgiActivityStatus(CorgiActivity corgiActivity);

    CorgiActivity deleteCorgiActivity(String activityId);

    List<CorgiActivity> searchCorgiActivity(CorgiActivity activity, Integer page, Integer pageSize);

    List<CorgiActivity> searchActivity(CorgiActivity activity, Integer page, Integer pageSize);

    long countCorgiActivity(CorgiActivity activity);

    long countPublishActivity(String date);

    long countRangePublishActivity(String beginDate, String endDate);

    long countUserActivity(String userId);

    List<CorgiActivity> getActivityByUserIds(List<String> userIds, String status, Integer page, Integer size);

    List<CorgiActivity> getAllActivityByUserIds(String userId, List<String> userIds, String status, Integer page, Integer size);

    List<CorgiActivity> getSimilarActivity(CorgiActivity corgiActivity);

    void updateByColumnn(String activity, String column, String value);

    void deleteUserActivity(String userId);

    void removeActivity(String activityId);

    List<HashMap> groupByActivity(String key, String beginDate, String endDate);

    List<CorgiActivity> getBarActivity(CorgiActivity activity);

    ActivityPage getRecommendActivity(Double lat, Double lng, ActivityQuery activityQuery);

    List<CorgiActivity> getCityRecommendActivity(String city, ActivityQuery activityQuery);

    List<CorgiActivity> getFeedActivity(ActivityQuery query);

    void refreshActivity();
}
