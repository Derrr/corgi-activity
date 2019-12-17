package com.corgi.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.corgi.activity.api.CorgiActivityService;
import com.corgi.activity.entity.CorgiActivity;
import com.corgi.dao.CorgiActivityDao;
import com.corgi.entity.ActivityMongo;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public CorgiActivity addCorgiActivity(CorgiActivity corgiActivity) {
        ActivityMongo activityMongo = corgiActivityDao.addActivity(corgiActivity);
        log.info(activityMongo.getId());
        return activityMongo;
    }

    @Override
    public List<CorgiActivity> getCorgiActivityByRange(double lng, double lat, double range) {
        List<CorgiActivity> result = new ArrayList<>();
        List<ActivityMongo> find = corgiActivityDao.getNearActivities(lng, lat, range);
        for (ActivityMongo activityMongo : find) {
            result.add(activityMongo);
        }
        return result;
    }
}
