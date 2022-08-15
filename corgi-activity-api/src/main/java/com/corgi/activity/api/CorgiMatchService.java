package com.corgi.activity.api;

import com.corgi.activity.entity.ActivityPage;
import com.corgi.activity.entity.CorgiActivity;
import com.corgi.entity.ActivityQuery;
import com.corgi.user.entity.UserDetail;
import com.corgi.user.entity.UserMatchItem;
import com.corgi.user.entity.UserQuery;

import java.util.HashMap;
import java.util.List;

/**
 * @author tairanliu
 */
public interface CorgiMatchService {
    void updateUser(UserDetail userDetail);

    List<UserMatchItem> getMatchItems(UserQuery userQuery);
}
