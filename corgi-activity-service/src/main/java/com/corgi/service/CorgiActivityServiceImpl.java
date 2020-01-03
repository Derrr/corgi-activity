package com.corgi.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.corgi.activity.api.CorgiActivityService;
import com.corgi.activity.entity.ActivityPic;
import com.corgi.activity.entity.CorgiActivity;
import com.corgi.dao.CorgiActivityDao;
import com.corgi.entity.ActivityMongo;
import com.corgi.user.api.CorgiPicService;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author tairanliu
 */
@Slf4j
@Service(interfaceClass = CorgiActivityService.class)
@Component
public class CorgiActivityServiceImpl implements CorgiActivityService {
    @Autowired
    private CorgiActivityDao corgiActivityDao;
    @Reference
    private CorgiPicService corgiPicService;

    @Override
    public CorgiActivity addCorgiActivity(CorgiActivity corgiActivity) {
        ActivityMongo activityMongo = corgiActivityDao.addActivity(corgiActivity);
        if(corgiActivity.getPics() != null){
            for(ActivityPic pic:corgiActivity.getPics()){
                corgiPicService.addActivityPic(pic);
            }
        }
        return activityMongo.getActivity();
    }

    @Override
    public List<CorgiActivity> getCorgiActivityByRange(double lng, double lat, double range, String type) {
        List<ActivityMongo> find = corgiActivityDao.getNearActivities(lng, lat, range, type);
        return convertActivity(find);
    }

    @Override
    public List<CorgiActivity> getUserRunningActivity(String userId) {
        List<ActivityMongo> mongoList = corgiActivityDao.getRunningActivities(userId);
        return convertActivity(mongoList);
    }

    @Override
    public List<CorgiActivity> getUserEndedActivity(String userId) {
        List<ActivityMongo> mongoList = corgiActivityDao.getEndedActivities(userId);
        return convertActivity(mongoList);
    }

    @Override
    public List<CorgiActivity> getActivityByIds(List<String> activityIds) {
        List<ObjectId> objectIds = new ArrayList<>();
        if (activityIds != null) {
            for (String id : activityIds) {
                objectIds.add(new ObjectId(id));
            }
        }
        List<ActivityMongo> mongoList = corgiActivityDao.getActivityByIds(objectIds);
        return convertActivity(mongoList);
    }

    @Override
    public CorgiActivity updateCorgiActivity(CorgiActivity corgiActivity) {
        ActivityMongo activityMongo = corgiActivityDao.updateActivity(corgiActivity);
        return activityMongo.getActivity();
    }

    @Override
    public CorgiActivity deleteCorgiActivity(String activityId) {
        ActivityMongo activityMongo = corgiActivityDao.deleteActivityById(activityId);
        return activityMongo.getActivity();
    }

    @Override
    public List<CorgiActivity> searchCorgiActivity(CorgiActivity activity) {
        return convertActivity(corgiActivityDao.queryActivities(activity));
    }

    List<CorgiActivity> convertActivity(List<ActivityMongo> activityMongoList) {
        List<CorgiActivity> corgiActivities = new ArrayList<>();
        if (activityMongoList != null) {
            for (ActivityMongo mongo : activityMongoList) {
                log.info("mongo" + mongo);
                CorgiActivity activity = mongo.getActivity();
                activity.setPics(corgiPicService.getActivityPic(activity.getId()));
                corgiActivities.add(activity);
            }
        }
        return corgiActivities;
    }
}
