package com.corgi.util;

import com.corgi.activity.entity.CorgiActivity;
import com.corgi.entity.ActivityMongo;
import com.corgi.entity.ActivityQuery;
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
        activityMongo.setPics(null);
        return activityMongo;
    }

    public static ActivityQuery addPage(ActivityQuery activityQuery) {
        Integer tPage = activityQuery.getTPage();
        Integer dPage = activityQuery.getDPage();
        if(dPage == 0){
            dPage++;
        }else if (tPage > 0 && dPage < 50) {
            dPage++;
            tPage--;
        } else {
            tPage += dPage;
            dPage = 1;
        }
        activityQuery.setTPage(tPage);
        activityQuery.setDPage(dPage);
        return activityQuery;
    }
}
