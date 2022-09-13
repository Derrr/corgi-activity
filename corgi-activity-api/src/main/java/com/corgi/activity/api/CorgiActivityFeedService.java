package com.corgi.activity.api;

import com.corgi.activity.entity.ActivityPage;
import com.corgi.activity.entity.CorgiActivity;
import com.corgi.entity.ActivityQuery;

import java.util.HashMap;
import java.util.List;

/**
 * @author tairanliu
 */
public interface CorgiActivityFeedService {
    CorgiActivity addFeedActivity(CorgiActivity corgiActivity);

    CorgiActivity getActivityById(String id);

    List<CorgiActivity> queryActivityFeed(ActivityQuery activityQuery);
}
