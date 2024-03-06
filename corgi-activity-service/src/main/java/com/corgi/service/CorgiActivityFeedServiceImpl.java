package com.corgi.service;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.corgi.activity.api.CorgiActivityFeedService;
import com.corgi.activity.entity.ActivityPage;
import com.corgi.activity.entity.ActivityPic;
import com.corgi.activity.entity.CorgiActivity;
import com.corgi.dao.CorgiActivityDao;
import com.corgi.entity.ActivityMongo;
import com.corgi.entity.ActivityQuery;
import com.corgi.entity.CorgiTopic;
import com.corgi.user.api.CorgiPicService;
import com.corgi.user.api.CorgiToolService;
import com.corgi.user.api.CorgiUserActivityService;
import com.corgi.user.entity.CorgiHashtag;
import lombok.extern.slf4j.Slf4j;
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
@Service(interfaceClass = CorgiActivityFeedService.class)
@Component
public class CorgiActivityFeedServiceImpl implements CorgiActivityFeedService {
    @Autowired
    private CorgiActivityDao corgiActivityDao;

    @Reference
    private CorgiToolService corgiToolService;
    @Reference
    private CorgiPicService corgiPicService;


    @Override
    public CorgiActivity addFeedActivity(CorgiActivity corgiActivity) {
        ActivityMongo activityMongo = corgiActivityDao.addActivity(corgiActivity);
        corgiToolService.updateActivityTopic(activityMongo.getMongoId().toHexString(), corgiActivity.getTopics());
        return activityMongo.getActivity();
    }

    @Override
    public CorgiActivity getActivityById(String id) {
        return corgiActivityDao.getActivityById(id).getActivity();
    }

    @Override
    public List<CorgiActivity> queryActivityFeed(ActivityQuery activityQuery) {
        List<ActivityMongo> mongos = corgiActivityDao.queryFeedActivity(activityQuery);
        return convertActivity(mongos);
    }

    List<CorgiActivity> convertActivity(List<ActivityMongo> activityMongoList) {
        List<CorgiActivity> corgiActivities = new ArrayList<>();
        if (activityMongoList != null) {
            for (ActivityMongo mongo : activityMongoList) {
                CorgiActivity activity = mongo.getActivity();
                List<ActivityPic> activityPics = corgiPicService.getActivityPic(activity.getId());
                if(StringUtils.isNotEmpty(activity.getCoverUrl())){
                    activity.setCoverUrl(activity.getCoverUrl().replaceAll("corgi-pic\\.oss-cn-beijing\\.aliyuncs\\.com", "image.corgi.org.cn"));
                }
                if (CorgiActivity.CAT_IMAGE.equals(mongo.getCategory()) && CollectionUtils.isEmpty(activityPics)) {
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
            }
        }
        return corgiActivities;
    }
}
