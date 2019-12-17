package com.corgi.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.corgi.activity.api.CorgiActivityService;
import com.corgi.activity.entity.CorgiActivity;
import com.corgi.dao.CorgiActivityDao;
import com.corgi.entity.ActivityMongo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author tairanliu
 */
@Service(interfaceClass = CorgiActivityService.class)
@Component
public class CorgiActivityServiceImpl implements CorgiActivityService {
    @Autowired
    private CorgiActivityDao corgiActivityDao;

    @Override
    public CorgiActivity addCorgiActivity(CorgiActivity corgiActivity) {
        return corgiActivityDao.addActivity(corgiActivity);
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
