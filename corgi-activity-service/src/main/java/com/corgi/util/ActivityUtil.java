package com.corgi.util;

import com.corgi.activity.entity.CorgiActivity;
import com.corgi.entity.ActivityMongo;
import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;

public class ActivityUtil {
    public static ActivityMongo getMongo(CorgiActivity activity) {
        ActivityMongo activityMongo = new ActivityMongo(activity);
        BeanUtils.copyProperties(activity, activityMongo);
        if (!StringUtils.isEmpty(activity.getId())) {
            activityMongo.setMongoId(new ObjectId(activity.getId()));
            activityMongo.setId(null);
        }
        return activityMongo;
    }
}
