package com.corgi.dao;


import com.corgi.activity.entity.CorgiActivity;
import com.corgi.entity.ActivityMongo;
import com.corgi.util.ActivityUtil;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author tairanliu
 */
@Slf4j
@Component
public class CorgiActivityDao {
    @Autowired
    MongoTemplate mongoTemplate;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");

    private SimpleDateFormat created_sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    private static List<String> LIKE_FIELDS = Arrays.asList("title", "content", "address");

    public ActivityMongo getActivityById(String activityId) {
        ActivityMongo mongo = mongoTemplate.findById(new ObjectId(activityId), ActivityMongo.class);
        mongo.setCreateTime(sdf.format(new Date()));
        return mongo;
    }

    public List<ActivityMongo> getActivityByIds(List<ObjectId> activityIds) {
        Query query = new Query(Criteria.where("_id").in(activityIds));
        List<ActivityMongo> activityMongoList = mongoTemplate.find(query, ActivityMongo.class);
        if (activityMongoList != null) {
            for (ActivityMongo mongo : activityMongoList) {
                mongo.setCreateTime(sdf.format(new Date()));
            }
        }
        return activityMongoList;
    }

    public ActivityMongo addActivity(CorgiActivity corgiActivity) {
        ActivityMongo activity = ActivityUtil.getMongo(corgiActivity);
        activity.setCreateTime(created_sdf.format(new Date()));
        activity.setStatus(CorgiActivity.CREATED);
        ActivityMongo mongo = mongoTemplate.insert(activity);
        mongo.setCreateTime(sdf.format(new Date()));
        return mongo;
    }

    public ActivityMongo updateActivity(CorgiActivity corgiActivity) {
        ActivityMongo activity = ActivityUtil.getMongo(corgiActivity);
        activity.setUpdateTime(created_sdf.format(new Date()));
        ActivityMongo mongo = mongoTemplate.save(activity);
        mongo.setCreateTime(sdf.format(new Date()));
        return mongo;
    }

    public ActivityMongo deleteActivityById(String activityId) {
        ActivityMongo activity = mongoTemplate.findById(new ObjectId(activityId), ActivityMongo.class);
        activity.setUpdateTime(created_sdf.format(new Date()));
        activity.setStatus(CorgiActivity.DELETED);
        activity.setCreateTime(sdf.format(new Date()));
        return activity;
    }

    public List<ActivityMongo> getNearActivities(double lng, double lat, double range, String type) {
        Criteria criteriaLocation = Criteria.where("location").withinSphere(new Circle(new Point(lng, lat), new Distance(range, Metrics.KILOMETERS)));
        Criteria criteriaSignUpTime = Criteria.where("signUpTime").gte(sdf.format(new Date()));
        Criteria criteriaStatus = Criteria.where("status").ne(CorgiActivity.DELETED);
        Criteria queryCriteria = new Criteria().andOperator(criteriaLocation, criteriaSignUpTime, criteriaStatus);
        if (!StringUtils.isEmpty(type)) {
            queryCriteria = queryCriteria.andOperator(Criteria.where("activityType").is(type));
        }
        Query query = new Query(queryCriteria);
        List<ActivityMongo> corgiActivities = mongoTemplate.find(query, ActivityMongo.class);
        return corgiActivities;
    }

    public List<ActivityMongo> getRunningActivities(String userId) {
        Criteria userCriteria = Criteria.where("userId").is(userId);
        Criteria statusCriteria = Criteria.where("status").is(CorgiActivity.CREATED);
        Criteria signUpCriteria = Criteria.where("signUpTime").gte(sdf.format(new Date()));
        Query query = new Query(new Criteria().andOperator(userCriteria, statusCriteria, signUpCriteria));
        List<ActivityMongo> corgiActivities = mongoTemplate.find(query, ActivityMongo.class);
        return corgiActivities;
    }

    public List<ActivityMongo> getEndedActivities(String userId) {
        Criteria userCriteria = Criteria.where("userId").is(userId);
        Criteria statusCriteria = new Criteria().orOperator(
                Criteria.where("status").is(CorgiActivity.DELETED),
                Criteria.where("signUpTime").lt(sdf.format(new Date()))
        );
        Query query = new Query(new Criteria().andOperator(userCriteria, statusCriteria));
        List<ActivityMongo> corgiActivities = mongoTemplate.find(query, ActivityMongo.class);
        if (corgiActivities != null) {
            for (ActivityMongo mongo : corgiActivities) {
                mongo.setCreateTime(sdf.format(new Date()));
            }
        }
        return corgiActivities;
    }

    public List<ActivityMongo> queryActivities(CorgiActivity activity) {
        Field[] fields = CorgiActivity.class.getDeclaredFields();
        List<Criteria> criteriaList = new ArrayList<>();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            field.setAccessible(true);
            try {
                Object value = field.get(activity);
                if (value != null) {
                    String fieldName = field.getName();
                    log.info("field " + fieldName + " value=" + value);

                    if (LIKE_FIELDS.contains(fieldName)) {
                        criteriaList.add(Criteria.where(field.getName()).regex("^.*" + value + ".*$"));
                    } else {
                        criteriaList.add(Criteria.where(field.getName()).is(value));
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        if (criteriaList.size() > 0) {
            Query query = new Query(new Criteria().andOperator((Criteria[]) criteriaList.toArray())).limit(20);
            mongoTemplate.find(query, ActivityMongo.class);
        }
        return new ArrayList<>();
    }
}
