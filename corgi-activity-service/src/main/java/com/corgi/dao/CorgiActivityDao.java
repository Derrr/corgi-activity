package com.corgi.dao;


import com.corgi.activity.entity.CorgiActivity;
import com.corgi.entity.ActivityMongo;
import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaExtensionsKt;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author tairanliu
 */
@Component
public class CorgiActivityDao {
    @Autowired
    MongoTemplate mongoTemplate;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");

    private SimpleDateFormat created_sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    public ActivityMongo getActivityById(String activityId) {
        return mongoTemplate.findById(new ObjectId(activityId), ActivityMongo.class);
    }

    public ActivityMongo addActivity(CorgiActivity corgiActivity) {
        ActivityMongo activity = new ActivityMongo(corgiActivity);
        BeanUtils.copyProperties(corgiActivity, activity);
        activity.setCreateTime(created_sdf.format(new Date()));
        activity.setStatus(CorgiActivity.CREATED);
        return mongoTemplate.insert(activity);
    }

    public ActivityMongo updateActivity(CorgiActivity corgiActivity) {
        ActivityMongo activity = new ActivityMongo(corgiActivity);
        BeanUtils.copyProperties(corgiActivity, activity);
        activity.setUpdateTime(created_sdf.format(new Date()));
        return mongoTemplate.save(activity);
    }

    public ActivityMongo deleteActivityById(String activityId) {
        ActivityMongo activity = mongoTemplate.findById(new ObjectId(activityId), ActivityMongo.class);
        activity.setUpdateTime(created_sdf.format(new Date()));
        activity.setStatus(CorgiActivity.DELETED);
        return mongoTemplate.save(activity);
    }

    public List<ActivityMongo> getNearActivities(double lng, double lat, double range) {

        Criteria criteriaLocation = Criteria.where("location").withinSphere(new Circle(new Point(lng, lat), new Distance(range, Metrics.KILOMETERS)));
        Criteria criteriaSignUpTime = Criteria.where("signUpTime").gte(sdf.format(new Date()));
        Criteria criteriaStatus = Criteria.where("status").ne(CorgiActivity.DELETED);
        Query query = new Query(new Criteria().andOperator(criteriaLocation, criteriaSignUpTime, criteriaStatus));
        List<ActivityMongo> corgiActivities = mongoTemplate.find(query, ActivityMongo.class);
        return corgiActivities;
    }
}
