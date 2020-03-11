package com.corgi.dao;


import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.config.annotation.Reference;
import com.corgi.activity.entity.CorgiActivity;
import com.corgi.entity.ActivityMongo;
import com.corgi.entity.ActivityQuery;
import com.corgi.user.api.CorgiFavorActivityService;
import com.corgi.user.api.CorgiUserService;
import com.corgi.util.ActivityUtil;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.CheckedOutputStream;

/**
 * @author tairanliu
 */
@Slf4j
@Component
public class CorgiActivityDao {
    @Autowired
    MongoTemplate mongoTemplate;
    @Reference
    private CorgiUserService corgiUserService;
    @Reference
    private CorgiFavorActivityService corgiFavorActivityService;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");

    private SimpleDateFormat created_sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    private static List<String> LIKE_FIELDS = Arrays.asList("title", "content", "address");

    public ActivityMongo getActivityById(String activityId) {
        ActivityMongo mongo = mongoTemplate.findById(new ObjectId(activityId), ActivityMongo.class);
        return mongo;
    }

    public List<ActivityMongo> getActivityByIds(List<ObjectId> activityIds) {
        Query query = new Query(Criteria.where("_id").in(activityIds));
        List<ActivityMongo> activityMongoList = mongoTemplate.find(query, ActivityMongo.class);
        return activityMongoList;
    }

    public ActivityMongo addActivity(CorgiActivity corgiActivity) {
        ActivityMongo activity = ActivityUtil.getMongo(corgiActivity);
        activity.setCreateTime(created_sdf.format(new Date()));
        activity.setStatus(CorgiActivity.CREATED);
        ActivityMongo mongo = mongoTemplate.insert(activity);
        return mongo;
    }

    public ActivityMongo updateActivity(CorgiActivity corgiActivity) {
        ActivityMongo activity = ActivityUtil.getMongo(corgiActivity);
        activity.setUpdateTime(created_sdf.format(new Date()));
        ActivityMongo oldMongo = mongoTemplate.findById(activity.getMongoId(), ActivityMongo.class);
        if (StringUtils.isEmpty(activity.getCheckTitle()) && !StringUtils.isEmpty(oldMongo.getCheckTitle())) {
            activity.setCheckTitle(oldMongo.getTitle());
        }
        if (StringUtils.isEmpty(activity.getCheckContent()) && !StringUtils.isEmpty(oldMongo.getCheckContent())) {
            activity.setCheckContent(oldMongo.getCheckContent());
        }
        if (StringUtils.isEmpty(activity.getCheckStatus()) && !StringUtils.isEmpty(oldMongo.getCheckStatus())) {
            activity.setCheckStatus(oldMongo.getCheckStatus());
        }
        if (StringUtils.isEmpty(activity.getCreateTime()) && !StringUtils.isEmpty(oldMongo.getCreateTime())) {
            activity.setCreateTime(oldMongo.getCreateTime());
        }
        if (StringUtils.isEmpty(activity.getStatus()) && !StringUtils.isEmpty(oldMongo.getStatus())) {
            activity.setStatus(oldMongo.getStatus());
        }
        ActivityMongo mongo = mongoTemplate.save(activity);
        return mongo;
    }

    public ActivityMongo deleteActivityById(String activityId) {
        ActivityMongo activity = mongoTemplate.findById(new ObjectId(activityId), ActivityMongo.class);
        activity.setUpdateTime(created_sdf.format(new Date()));
        activity.setStatus(CorgiActivity.DELETED);
        mongoTemplate.save(activity);
        return activity;
    }

    public List<ActivityMongo> getNearActivities(double lng, double lat, double range, ActivityQuery activityQuery) {
        List<Criteria> criteriaList = new ArrayList<>();
        if (range > 0) {
            criteriaList.add(Criteria.where("location").nearSphere(new Point(lng, lat)).maxDistance(range / 6371));
            //new Distance(range * 1000, Metrics.KILOMETERS))));
        }
        criteriaList.add(Criteria.where(ActivityMongo.SIGN_UP_TIME).gte(sdf.format(new Date())));
        criteriaList.add(Criteria.where("status").ne(CorgiActivity.DELETED));
        if (!StringUtils.isEmpty(activityQuery.getType())) {
            criteriaList.add(Criteria.where("activityType").is(activityQuery.getType()));
        }
        if (CollectionUtils.isNotEmpty(activityQuery.getPayType())) {
            criteriaList.add(Criteria.where("payType").in(activityQuery.getPayType()));
        }
        if (!StringUtils.isEmpty(activityQuery.getCity())) {
            criteriaList.add(Criteria.where("city").is(activityQuery.getCity()));
        }
        if (!StringUtils.isEmpty(activityQuery.getAdname())) {
            criteriaList.add(Criteria.where("adname").is(activityQuery.getAdname()));
        }
        if (!StringUtils.isEmpty(activityQuery.getBusinessArea())) {
            criteriaList.add(Criteria.where("businessArea").is(activityQuery.getBusinessArea()));
        }
        if (!StringUtils.isEmpty(activityQuery.getStation())) {
            criteriaList.add(Criteria.where("station").is(activityQuery.getStation()));
        }
        if (activityQuery.getStartBudget() > 0) {
            criteriaList.add(Criteria.where("budget").gte(activityQuery.getStartBudget()));
        }
        if (activityQuery.getEndBudget() > 0) {
            criteriaList.add(Criteria.where("budget").lte(activityQuery.getEndBudget()));
        }
        if (!StringUtils.isEmpty(activityQuery.getStartTime())) {
            criteriaList.add(Criteria.where(ActivityMongo.SIGN_UP_TIME).gte(activityQuery.getStartTime()));
        }
        if (!StringUtils.isEmpty(activityQuery.getEndTime())) {
            criteriaList.add(Criteria.where(ActivityMongo.SIGN_UP_TIME).lte(activityQuery.getEndTime()));
        }

        Criteria queryCriteria = new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
        Query query = new Query(queryCriteria).limit(100);
        if(range <= 0){
            query = query.with(Sort.by(Sort.Direction.DESC, "mongoId"));
        }
        List<ActivityMongo> corgiActivities = mongoTemplate.find(query, ActivityMongo.class);

        if (activityQuery.checkUser() && CollectionUtils.isNotEmpty(corgiActivities)) {
            List<String> userIds = new ArrayList<>();
            for (ActivityMongo mongo : corgiActivities) {
                userIds.add(mongo.getUserId());
            }
            List<String> resultUserIds = corgiUserService.filterUser(userIds, activityQuery);
            if (CollectionUtils.isEmpty(resultUserIds)) {
                return new ArrayList<>();
            }
            Iterator<ActivityMongo> iterator = corgiActivities.iterator();
            while (iterator.hasNext()) {
                ActivityMongo mongo = iterator.next();
                if (resultUserIds.contains(mongo.getUserId())) {
                    continue;
                }
                iterator.remove();
            }
        }
        return corgiActivities;
    }

    public List<ActivityMongo> getRunningActivities(String userId, Integer start, Integer size) {
        Criteria userCriteria = Criteria.where("userId").is(userId);
        Criteria statusCriteria = Criteria.where("status").is(CorgiActivity.CREATED);
        Criteria signUpCriteria = Criteria.where(ActivityMongo.SIGN_UP_TIME).gte(sdf.format(new Date()));
        Query query = getDescIdQuery(new Criteria().andOperator(userCriteria, statusCriteria, signUpCriteria), start, size);
        List<ActivityMongo> corgiActivities = mongoTemplate.find(query, ActivityMongo.class);
        return corgiActivities;
    }

    public List<ActivityMongo> getEndedActivities(String userId, Integer start, Integer size) {
        Criteria userCriteria = Criteria.where("userId").is(userId);
        Criteria statusCriteria = new Criteria().orOperator(
                Criteria.where("status").is(CorgiActivity.DELETED),
                Criteria.where(ActivityMongo.SIGN_UP_TIME).lt(sdf.format(new Date()))
        );
        Query query = getDescIdQuery(new Criteria().andOperator(userCriteria, statusCriteria), start, size);
        List<ActivityMongo> corgiActivities = mongoTemplate.find(query, ActivityMongo.class);
        return corgiActivities;
    }

    public List<ActivityMongo> queryActivities(CorgiActivity activity, Integer start, Integer size) {
        List<Criteria> criteriaList = getCriteriaList(activity);
        if (criteriaList.size() > 0) {
            Query query = getDescIdQuery(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])), start, size);
            return mongoTemplate.find(query, ActivityMongo.class);
        }
        return mongoTemplate.find(new Query().with(Sort.by(Sort.Direction.DESC, "_id")).skip(start).limit(size), ActivityMongo.class);
    }

    public long countActivities(CorgiActivity activity) {
        List<Criteria> criteriaList = getCriteriaList(activity);
        if (criteriaList.size() > 0) {
            Query query = getDescIdQuery(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])), 0, 20);
            return mongoTemplate.count(query, ActivityMongo.class);
        }
        return mongoTemplate.count(new Query(), ActivityMongo.class);
    }

    public List<ActivityMongo> searchActivity(CorgiActivity activity, double range) {
        Criteria criteriaLocation = Criteria.where("location").withinSphere(new Circle(new Point(activity.getLng(), activity.getLat()), new Distance(range * 100000, Metrics.KILOMETERS)));
        Criteria criteriaSignUpTime = Criteria.where(ActivityMongo.SIGN_UP_TIME).gte(activity.getSignUpTime());
        Criteria criteriaStatus = Criteria.where("status").ne(CorgiActivity.DELETED);
        Query query = getDescIdQuery(new Criteria().andOperator(criteriaLocation, criteriaSignUpTime, criteriaStatus), 0, 20);
        return mongoTemplate.find(query, ActivityMongo.class);
    }

    public List<ActivityMongo> getActivityByUserIds(List<String> userIds, String status, Integer start, Integer size) {
        Criteria c = Criteria.where("userId").in(userIds);
        if (CorgiActivity.CREATED.equals(status)) {
            new Criteria().andOperator(c, Criteria.where("status").ne(CorgiActivity.DELETED));
            //c = new Criteria().andOperator(c, Criteria.where(ActivityMongo.SIGN_UP_TIME).gte(sdf.format(new Date())));
        } else if (CorgiActivity.ENDED.equals(status)) {
            c = new Criteria().andOperator(c, Criteria.where(ActivityMongo.SIGN_UP_TIME).lte(sdf.format(new Date())));
        } else if (!StringUtils.isEmpty(start)) {
            c = new Criteria().andOperator(c, Criteria.where("status").is(status));
        }
        Query query = getDescIdQuery(c, start, size);
        return mongoTemplate.find(query, ActivityMongo.class);
    }

    public long countActivity(String date) {
        Pattern pattern = Pattern.compile("^" + date);
        Query query = new Query(Criteria.where("createTime").regex(pattern));
        return mongoTemplate.count(query, ActivityMongo.class);
    }

    public long countActivity(String beginDate, String endDate) {
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("createTime").gte(beginDate),
                Criteria.where("createTime").lte(endDate));
        Query query = new Query(criteria);
        return mongoTemplate.count(query, ActivityMongo.class);
    }

    public long countUserActivity(String userId) {
        Query query = new Query(Criteria.where("userId").is(userId));
        return mongoTemplate.count(query, ActivityMongo.class);
    }

    private Query getDescIdQuery(Criteria criteria, Integer start, Integer size) {
        Query query = new Query(criteria).with(Sort.by(Sort.Direction.DESC, "mongoId")).skip(start).limit(size);
        return query;
    }

    private List<Criteria> getCriteriaList(CorgiActivity activity) {
        Field[] fields = CorgiActivity.class.getDeclaredFields();
        List<Criteria> criteriaList = new ArrayList<>();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            field.setAccessible(true);
            if (Modifier.isStatic(field.getModifiers()) || field.getType().equals(double.class)) {
                continue;
            }
            try {
                Object value = field.get(activity);
                if (value != null && !"".equals(value.toString())) {
                    String fieldName = field.getName();
                    log.info("field " + fieldName + " value=" + value);
                    if ("status".equals(fieldName)) {
                        if (CorgiActivity.ENDED.equals(value)) {
                            criteriaList.add(Criteria.where(ActivityMongo.SIGN_UP_TIME).gte(sdf.format(new Date())));
                            criteriaList.add(Criteria.where("status").is(CorgiActivity.CREATED));
                        } else {
                            criteriaList.add(Criteria.where("status").is(value));
                        }
                    } else if (LIKE_FIELDS.contains(fieldName)) {
                        criteriaList.add(Criteria.where(field.getName()).regex("^.*" + value + ".*$"));
                    } else if (int.class.equals(field.getType()) && (int) value != 0) {
                        criteriaList.add(Criteria.where(field.getName()).is(value));
                    } else if (!int.class.equals(field.getType())) {
                        criteriaList.add(Criteria.where(field.getName()).is(value));
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return criteriaList;
    }

    public void deleteActivityByUserId(String userId) {
        Query query = new Query(Criteria.where("userId").is(userId));
        List<ActivityMongo> activityMongoList = mongoTemplate.findAllAndRemove(query, ActivityMongo.class);
        for (ActivityMongo mongo : activityMongoList) {
            corgiFavorActivityService.deleteByActivityId(mongo.getMongoId().toString());
        }
    }

    public void removeActivity(String activityId) {
        Query query = new Query(Criteria.where("mongoId").is(new ObjectId(activityId)));
        corgiFavorActivityService.deleteByActivityId(activityId);
        mongoTemplate.remove(query, ActivityMongo.class);
    }

    public void updateActivityByColumn(String activityId, String column, String value) {
        Update update = new Update().set(column, value);
        Query query = new Query(Criteria.where("mongoId").is(new ObjectId(activityId)));
        mongoTemplate.updateFirst(query, update, ActivityMongo.class);
    }

    public List<HashMap> groupByActivity(String key, String beginDate, String endDate) {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(new Criteria().andOperator(
                        Criteria.where("createTime").gte(beginDate),
                        Criteria.where("createTime").lte(endDate)
                )),
                Aggregation.project(key),
                Aggregation.group(key).count().as("count")
        );
        AggregationResults<HashMap> results = mongoTemplate.aggregate(agg, ActivityMongo.class, HashMap.class);
        return results.getMappedResults();
    }
}
