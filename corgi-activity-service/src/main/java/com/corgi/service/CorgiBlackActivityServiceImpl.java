package com.corgi.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.corgi.activity.api.CorgiBlackActivityService;
import com.corgi.activity.entity.CorgiActivity;
import com.corgi.dao.CorgiActivityDao;
import com.corgi.entity.ActivityMongo;
import com.corgi.user.api.CorgiFavorActivityService;
import com.corgi.user.api.CorgiUserActivityService;
import com.corgi.user.entity.UserProfile;
import com.corgi.user.entity.UserSignUp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author tairanliu
 */
@Slf4j
@Service(interfaceClass = CorgiBlackActivityService.class)
@Component
public class CorgiBlackActivityServiceImpl implements CorgiBlackActivityService {
    @Autowired
    private CorgiActivityDao corgiActivityDao;
    @Reference
    private CorgiFavorActivityService corgiFavorActivityService;
    @Reference
    private CorgiUserActivityService corgiUserActivityService;


    @Override
    public void deleteFavorActivity(String userId, String blackId) {
        List<ActivityMongo> activityIds = corgiActivityDao.getActivityIdByUserId(blackId);
        if (!CollectionUtils.isEmpty(activityIds)) {
            for (ActivityMongo activityId : activityIds) {
                corgiFavorActivityService.deleteFavor(userId, activityId.getMongoId().toHexString());
            }
        }

        activityIds = corgiActivityDao.getActivityIdByUserId(userId);
        if (!CollectionUtils.isEmpty(activityIds)) {
            for (ActivityMongo activityId : activityIds) {
                corgiFavorActivityService.deleteFavor(blackId, activityId.getMongoId().toHexString());
            }
        }
    }

    @Override
    public boolean checkActivity(String userId, String blackId, String activityId) {
        ActivityMongo activityMongo = corgiActivityDao.getActivityById(activityId);
        if (activityMongo != null && blackId.equals(activityMongo.getUserId())) {
            if (activityMongo.getStatus().equals(CorgiActivity.FULL)) {
                List<UserProfile> userProfiles = corgiUserActivityService.getUsers(activityId, null, "");
                int count = 0;
                if (!CollectionUtils.isEmpty(userProfiles)) {
                    for (UserProfile userProfile : userProfiles) {
                        if (userProfile.getSignUpStatus() == UserSignUp.AGREE && !userProfile.getUserId().equals(userId)) {
                            count++;
                        }
                    }
                }
                if (count < activityMongo.getPeopleCount() - 1) {
                    CorgiActivity corgiActivity = new CorgiActivity();
                    corgiActivity.setId(activityId);
                    corgiActivity.setStatus(CorgiActivity.CREATED);
                    corgiActivityDao.updateActivityStatus(corgiActivity);
                }
            }
            return true;
        }
        return false;
    }
}
