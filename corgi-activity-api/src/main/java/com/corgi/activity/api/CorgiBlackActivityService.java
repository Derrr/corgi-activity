package com.corgi.activity.api;

public interface CorgiBlackActivityService {
    void deleteFavorActivity(String userId, String blackId);

    boolean checkActivity(String userId, String blackId, String activityId);
}
