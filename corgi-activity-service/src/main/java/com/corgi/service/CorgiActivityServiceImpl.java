package com.corgi.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.corgi.activity.api.CorgiActivityService;
import com.corgi.activity.entity.ActivityPic;
import com.corgi.activity.entity.CorgiActivity;
import com.corgi.dao.CorgiActivityDao;
import com.corgi.entity.ActivityMongo;
import com.corgi.entity.ActivityQuery;
import com.corgi.user.api.CorgiPicService;
import com.corgi.user.api.CorgiToolService;
import com.corgi.user.api.CorgiUserActivityService;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
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
    @Reference
    private CorgiToolService corgiToolService;
    @Reference
    private CorgiUserActivityService corgiUserActivityService;

    @Override
    public CorgiActivity addCorgiActivity(CorgiActivity corgiActivity) {
        ActivityMongo activityMongo = corgiActivityDao.addActivity(corgiActivity);
        if (corgiActivity.getPics() != null) {
            for (ActivityPic pic : corgiActivity.getPics()) {
                pic.setActivityId(activityMongo.getMongoId().toHexString());
                corgiPicService.addActivityPic(pic);
            }
        }
        corgiToolService.updateActivityTopic(activityMongo.getMongoId().toHexString(), corgiActivity.getTopics());
        String category = corgiActivity.getCategory();
        if (CorgiActivity.CAT_ATTENDANCE.equals(category)) {
            category = category + corgiActivity.getBarId();
        }
        corgiUserActivityService.addActivityCreator(corgiActivity.getUserId(), activityMongo.getMongoId().toHexString(), category);
        return activityMongo.getActivity();
    }

    @Override
    public List<CorgiActivity> getCorgiActivityByRange(double lng, double lat, double range, ActivityQuery activityQuery) {
        List<ActivityMongo> find = corgiActivityDao.getNearActivities(lng, lat, range, activityQuery);
        return convertActivity(find);
    }

    @Override
    public List<CorgiActivity> getUserRunningActivity(String userId, Integer page, Integer size) {
        List<ActivityMongo> mongoList = corgiActivityDao.getRunningActivities(userId, (page - 1) * size, size);
        return convertActivity(mongoList);
    }

    @Override
    public List<CorgiActivity> getUserAllRunningActivity(String userId, Integer page, Integer size) {
        List<ActivityMongo> mongoList = corgiActivityDao.getAllRunningActivities(userId, (page - 1) * size, size);
        return convertActivity(mongoList);
    }

    @Override
    public List<CorgiActivity> getUserEndedActivity(String userId, Integer page, Integer size) {
        List<ActivityMongo> mongoList = corgiActivityDao.getEndedActivities(userId, (page - 1) * size, size);
        return convertActivity(mongoList);
    }

    @Override
    public List<CorgiActivity> getActivityByIds(List<String> activityIds) {
        List<ActivityMongo> mongoList = new ArrayList<>();
        if (activityIds != null) {
            for (String id : activityIds) {
                ActivityMongo mongo = corgiActivityDao.getActivityById(id);
                if (mongo != null) {
                    mongoList.add(mongo);
                }
            }
        }
        return convertActivity(mongoList);
    }

    @Override
    public CorgiActivity updateCorgiActivity(CorgiActivity corgiActivity) {
        ActivityMongo activityMongo = corgiActivityDao.updateActivity(corgiActivity);
        corgiToolService.updateActivityTopic(activityMongo.getMongoId().toHexString(), corgiActivity.getTopics());
        return activityMongo.getActivity();
    }

    @Override
    public void updateCorgiActivityStatus(CorgiActivity corgiActivity) {
        corgiActivityDao.updateActivityStatus(corgiActivity);
    }

    @Override
    public CorgiActivity deleteCorgiActivity(String activityId) {
        ActivityMongo activityMongo = corgiActivityDao.deleteActivityById(activityId);
        return activityMongo.getActivity();
    }

    @Override
    public List<CorgiActivity> searchCorgiActivity(CorgiActivity activity, Integer page, Integer pageSize) {
        return convertActivity(corgiActivityDao.queryActivities(activity, page < 1 ? 0 : (page - 1) * pageSize, pageSize), page != -1 && !"check".equals(activity.getCheckStatus()));
    }

    @Override
    public long countCorgiActivity(CorgiActivity activity) {
        return corgiActivityDao.countActivities(activity);
    }

    @Override
    public long countPublishActivity(String date) {
        return corgiActivityDao.countActivity(date);
    }

    @Override
    public long countRangePublishActivity(String beginDate, String endDate) {
        return corgiActivityDao.countActivity(beginDate, endDate);
    }

    @Override
    public long countUserActivity(String userId) {
        return corgiActivityDao.countUserActivity(userId);
    }

    @Override
    public List<CorgiActivity> getActivityByUserIds(List<String> userIds, String status, Integer page, Integer size) {
        return convertActivity(corgiActivityDao.getActivityByUserIds(userIds, status, (page - 1) * size, size));
    }

    @Override
    public List<CorgiActivity> getAllActivityByUserIds(String loginUserId, List<String> userIds, String status, Integer page, Integer size) {
        return convertActivity(corgiActivityDao.getAllActivityByUserIds(loginUserId, userIds, status, (page - 1) * size, size));
    }


    @Override
    public List<CorgiActivity> getSimilarActivity(CorgiActivity corgiActivity) {
        return convertActivity(corgiActivityDao.searchActivity(corgiActivity, 2));
    }

    @Override
    public void updateByColumnn(String activity, String column, String value) {
        corgiActivityDao.updateActivityByColumn(activity, column, value);
    }

    @Override
    public void deleteUserActivity(String userId) {
        corgiActivityDao.deleteActivityByUserId(userId);
    }

    @Override
    public void removeActivity(String activityId) {
        corgiActivityDao.removeActivity(activityId);
    }

    @Override
    public List<HashMap> groupByActivity(String type, String beginDate, String endDate) {
        return corgiActivityDao.groupByActivity(type, beginDate, endDate);
    }

    @Override
    public List<CorgiActivity> getBarActivity(CorgiActivity activity) {
        return convertActivity(corgiActivityDao.getBarActivity(activity));
    }

    List<CorgiActivity> convertActivity(List<ActivityMongo> activityMongoList, boolean checkPic) {
        List<CorgiActivity> corgiActivities = new ArrayList<>();
        if (activityMongoList != null) {
            for (ActivityMongo mongo : activityMongoList) {
                CorgiActivity activity = mongo.getActivity();
                List<ActivityPic> activityPics = corgiPicService.getActivityPic(activity.getId());
                if (checkPic && CollectionUtils.isEmpty(activityPics)) {
                    continue;
                }
                activity.setPics(activityPics);
                corgiActivities.add(activity);
            }
        }
        return corgiActivities;
    }

    List<CorgiActivity> convertActivity(List<ActivityMongo> activityMongoList) {
        return convertActivity(activityMongoList, true);
    }
}
