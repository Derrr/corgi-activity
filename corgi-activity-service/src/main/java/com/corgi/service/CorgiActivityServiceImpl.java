package com.corgi.service;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.corgi.activity.api.CorgiActivityService;
import com.corgi.activity.entity.ActivityPage;
import com.corgi.activity.entity.ActivityPic;
import com.corgi.activity.entity.CorgiActivity;
import com.corgi.dao.CorgiActivityDao;
import com.corgi.entity.ActivityMongo;
import com.corgi.entity.ActivityQuery;
import com.corgi.entity.CorgiTopic;
import com.corgi.user.api.CorgiBlacklistService;
import com.corgi.user.api.CorgiPicService;
import com.corgi.user.api.CorgiToolService;
import com.corgi.user.api.CorgiUserActivityService;
import com.corgi.user.entity.CorgiHashtag;
import com.corgi.user.entity.UserBasic;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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
    @Reference
    private CorgiBlacklistService corgiBlacklistService;

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
    public List<CorgiActivity> getCityCorgiActivityByRange(double lng, double lat, double range, ActivityQuery activityQuery) {
        List<ActivityMongo> find = corgiActivityDao.getCityNearActivities(lng, lat, range, activityQuery);
        return convertActivity(find, false);
    }

    @Override
    public List<CorgiActivity> getCorgiActivityByRange(double lng, double lat, double range, ActivityQuery activityQuery) {
        List<ActivityMongo> find = corgiActivityDao.getNearActivities(lng, lat, range, activityQuery);
        return convertActivity(find, false);
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
                if (mongo != null && !CorgiActivity.DELETED.equals(mongo.getStatus())) {
                    mongoList.add(mongo);
                }
            }
        }
        return convertActivity(mongoList, false);
    }

    @Override
    public List<CorgiActivity> getBarAppraisedActivity(String barId, String activityId, Integer size) {
        return convertActivity(corgiActivityDao.getBarAppraisedActivityId(barId, activityId, size));
    }

    @Override
    public Long countBarAppraisedActivity(String barId) {
        return corgiActivityDao.countBarAppraisedActivityId(barId);
    }

    @Override
    public CorgiActivity updateCorgiActivity(CorgiActivity corgiActivity) {
        ActivityMongo mongo = corgiActivityDao.getActivityById(corgiActivity.getId());
        if (mongo == null || !mongo.getUserId().equals(corgiActivity.getUserId())) {
            return corgiActivity;
        }
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
    public List<CorgiActivity> searchActivity(CorgiActivity activity, Integer page, Integer pageSize) {
        return convertActivity(corgiActivityDao.searchActivity(activity, (page - 1) * pageSize, pageSize));
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

    @Override
    public ActivityPage getRecommendActivity(Double lat, Double lng, ActivityQuery activityQuery) {
        List<ActivityMongo> mongos = corgiActivityDao.getRecommendActivities(lng, lat, activityQuery);
        ActivityPage page = new ActivityPage();
        page.setDPage(activityQuery.getDPage());
        page.setTPage(activityQuery.getTPage());
        page.setRPage(activityQuery.getRPage());
        page.setCorgiActivityList(convertActivity(mongos, false));
        return page;
    }

    @Override
    public List<CorgiActivity> getCityRecommendActivity(String city, ActivityQuery activityQuery) {
        return convertActivity(corgiActivityDao.getCityRecommendActivity(city));
    }

    @Override
    public List<CorgiActivity> getFeedActivity(ActivityQuery query) {
        List<Criteria> resultList = new ArrayList<>();
        Criteria checkStatus = Criteria.where("checkStatus").ne("fail");
        resultList.add(checkStatus);
        Criteria StatusCri = Criteria.where("status").ne(CorgiActivity.DELETED);
        resultList.add(StatusCri);
        if (StringUtils.isNotEmpty(query.getUserId())) {
            Criteria userCri = Criteria.where("userId").is(query.getUserId());
            resultList.add(userCri);
        }
        if (StringUtils.isNotEmpty(query.getCity())) {
            Criteria cityCri = Criteria.where("city").is(query.getCity());
            resultList.add(cityCri);
        }
        Criteria categoryCri = Criteria.where("category").in(CorgiActivity.CAT_TEXT, CorgiActivity.CAT_VIDEO, CorgiActivity.CAT_IMAGE, CorgiActivity.CAT_BUSINESS);
        if (StringUtils.isNotEmpty(query.getCategory())) {
            categoryCri = Criteria.where("category").is(query.getCategory());
        }
        if (StringUtils.isNotEmpty(query.getLoginUserId())) {
            List<UserBasic> blackUsers = corgiBlacklistService.getBlackUser(query.getLoginUserId());
            if (!CollectionUtils.isEmpty(blackUsers)) {
                Criteria blackCri = Criteria.where("userId").nin(blackUsers.stream().map(u -> u.getUserId()).collect(Collectors.toList()));
                resultList.add(blackCri);
            }
        }
        resultList.add(categoryCri);
        resultList.toArray();
        Criteria[] a = new Criteria[resultList.size()];
        resultList.toArray(a);
        int start = 0;
        if (query.getPage() != null) {
            start = (query.getPage() - 1) * query.getPageSize();
        }
        return convertActivity(corgiActivityDao.getActivityByFeed(query.getActivityId(), new Criteria().andOperator(a), start, query.getPageSize()));
    }

    @Override
    public void refreshActivity() {
        corgiActivityDao.refreshActivity();
    }

    List<CorgiActivity> convertActivity(List<ActivityMongo> activityMongoList, boolean checkPic) {
        List<CorgiActivity> corgiActivities = new ArrayList<>();
        if (activityMongoList != null) {
            for (ActivityMongo mongo : activityMongoList) {
                CorgiActivity activity = mongo.getActivity();
                List<ActivityPic> activityPics = corgiPicService.getActivityPic(activity.getId());
                if (checkPic && CorgiActivity.CAT_IMAGE.equals(mongo.getCategory()) && CollectionUtils.isEmpty(activityPics)) {
                    continue;
                }
                activity.setPics(activityPics);
                corgiActivities.add(activity);
                List<CorgiTopic> topics = corgiToolService.getActivityTopicDetails(activity.getId());
                List<CorgiTopic> result1 = new ArrayList<>();
                if (!CollectionUtils.isEmpty(topics)) {
                    for (CorgiTopic topic : topics) {
                        if (topic != null && StringUtils.isNotEmpty(topic.getTopic())) {
                            result1.add(topic);
                        }
                    }
                }
                activity.setTopicDetails(result1);

                List<CorgiHashtag> hashtags = corgiToolService.getActivityHashTagDetails(activity.getId());
                List<CorgiHashtag> result2 = new ArrayList<>();
                if (!CollectionUtils.isEmpty(topics)) {
                    for (CorgiHashtag hashtag : hashtags) {
                        if (hashtag != null && StringUtils.isNotEmpty(hashtag.getHastagName())) {
                            result2.add(hashtag);
                        }
                    }
                }
                activity.setHashtagDetails(result2);
                if (StringUtils.isNotEmpty(activity.getRefActivityId()) && StringUtils.isEmpty(activity.getRefActivityTitle())) {
                    String activityId = activity.getRefActivityId();
                    ActivityMongo refMongo = corgiActivityDao.getActivityById(activityId);
                    activity.setRefActivityTitle(refMongo.getTitle());
                }
            }
        }
        return corgiActivities;
    }

    List<CorgiActivity> convertActivity(List<ActivityMongo> activityMongoList) {
        return convertActivity(activityMongoList, true);
    }
}
