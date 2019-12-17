package com.corgi.dao;


import com.corgi.activity.entity.CorgiActivity;
import com.corgi.entity.ActivityMongo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author tairanliu
 */
@Component
public class CorgiActivityDao {
    @Autowired
    MongoTemplate mongoTemplate;

    public CorgiActivity addActivity(CorgiActivity corgiActivity) {
        return mongoTemplate.insert(new ActivityMongo(corgiActivity));
    }

    public List<ActivityMongo> getNearActivities(double lng, double lat, double range) {
        Criteria criteria = Criteria.where("location").withinSphere(new Circle(new Point(lng, lat), new Distance(range, Metrics.KILOMETERS)));
        Query query = new Query(criteria);
        List<ActivityMongo> corgiActivities = mongoTemplate.find(query, ActivityMongo.class);
        return corgiActivities;
    }
}
