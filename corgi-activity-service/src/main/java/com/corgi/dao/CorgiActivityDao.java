package com.corgi.dao;


import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.config.annotation.Reference;
import com.corgi.activity.entity.CorgiActivity;
import com.corgi.entity.ActivityMongo;
import com.corgi.entity.ActivityQuery;
import com.corgi.user.api.CorgiFavorActivityService;
import com.corgi.user.api.CorgiToolService;
import com.corgi.user.api.CorgiUserActivityService;
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
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    @Reference
    private CorgiUserActivityService corgiUserActivityService;
    @Reference
    private CorgiToolService corgiToolService;

    private final static Double RADIUS = 6371.0;

    //private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");

    //private SimpleDateFormat created_sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    private static List<String> LIKE_FIELDS = Arrays.asList("title", "content", "address", "coverUrl");

    public List<ActivityMongo> getBarAppraisedActivityId(String barId, String activityId, Integer size) {
        Query query = getDescIdQuery(Criteria.where("barId").is(barId), 0, size);
        query.addCriteria(Criteria.where("category").in("video", "text", "image"))
                .addCriteria(Criteria.where("checkStatus").ne("fail"))
                .addCriteria(Criteria.where("status").ne(CorgiActivity.DELETED));
        if (!StringUtils.isEmpty(activityId)) {
            query.addCriteria(Criteria.where("_id").lt(new ObjectId(activityId)));
        }
        return mongoTemplate.find(query, ActivityMongo.class);
    }

    public Long countBarAppraisedActivityId(String barId) {
        Query query = new Query(Criteria.where("barId").is(barId));
        query.addCriteria(Criteria.where("category").in("video", "text", "image"))
                .addCriteria(Criteria.where("checkStatus").ne("fail"))
                .addCriteria(Criteria.where("status").ne(CorgiActivity.DELETED));
        return mongoTemplate.count(query, ActivityMongo.class);
    }

    public ActivityMongo getActivityById(String activityId) {
        ActivityMongo mongo = new ActivityMongo();
        try {
            mongo = mongoTemplate.findById(new ObjectId(activityId), ActivityMongo.class);
            if (mongo != null) {
                mongo.setCurrentTime(new SimpleDateFormat("yyyy/MM/dd HH:mm").format(new Date()));
            } else {
                mongo = new ActivityMongo();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return mongo;
    }

    public List<ActivityMongo> getActivityByIds(List<ObjectId> activityIds) {
        Query query = new Query(Criteria.where("_id").in(activityIds));
        List<ActivityMongo> activityMongoList = mongoTemplate.find(query, ActivityMongo.class);
        return activityMongoList;
    }

    public ActivityMongo addActivity(CorgiActivity corgiActivity) {
        ActivityMongo activity = ActivityUtil.getMongo(corgiActivity);
        activity.setCreateTime(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
        activity.setStatus(CorgiActivity.CREATED);
        ActivityMongo mongo = mongoTemplate.insert(activity);
        return mongo;
    }

    public void updateActivityStatus(CorgiActivity corgiActivity) {
        Query query = new Query(Criteria.where("mongoId").is(new ObjectId(corgiActivity.getId())));
        if (!StringUtils.isEmpty(corgiActivity.getStatus())) {
            Update update = new Update().set("status", corgiActivity.getStatus());
            mongoTemplate.updateFirst(query, update, ActivityMongo.class);
        }
        if (!StringUtils.isEmpty(corgiActivity.getCheckStatus())) {
            Update update = new Update().set("checkStatus", corgiActivity.getCheckStatus());
            mongoTemplate.updateFirst(query, update, ActivityMongo.class);
        }
        if (!StringUtils.isEmpty(corgiActivity.getRecommend())) {
            Update update = new Update().set("recommend", corgiActivity.getRecommend());
            mongoTemplate.updateFirst(query, update, ActivityMongo.class);
        }

    }

    public ActivityMongo updateActivity(CorgiActivity corgiActivity) {
        ActivityMongo activity = ActivityUtil.getMongo(corgiActivity);
        activity.setUpdateTime(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
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
        if (StringUtils.isEmpty(activity.getStartTime()) && !StringUtils.isEmpty(oldMongo.getStartTime())) {
            activity.setStartTime(oldMongo.getStartTime());
        }
        if (StringUtils.isEmpty(activity.getEndTime()) && !StringUtils.isEmpty(oldMongo.getEndTime())) {
            activity.setEndTime(oldMongo.getEndTime());
        }
        if (StringUtils.isEmpty(activity.getAddress()) && !StringUtils.isEmpty(oldMongo.getAddress())) {
            activity.setAddress(oldMongo.getAddress());
        }
        if (StringUtils.isEmpty(activity.getCity()) && !StringUtils.isEmpty(oldMongo.getCity())) {
            activity.setCity(oldMongo.getCity());
        }
        if (StringUtils.isEmpty(activity.getBarId()) && !StringUtils.isEmpty(oldMongo.getBarId())) {
            activity.setBarId(oldMongo.getBarId());
        }
        if (activity.getLat() == 0 && oldMongo.getLat() != 0) {
            activity.setLat(oldMongo.getLat());
        }

        ActivityMongo mongo = mongoTemplate.save(activity);
        return mongo;
    }

    public ActivityMongo deleteActivityById(String activityId) {
        ActivityMongo activity = mongoTemplate.findById(new ObjectId(activityId), ActivityMongo.class);
        if (activity != null) {
            activity.setUpdateTime(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
            activity.setStatus(CorgiActivity.DELETED);
            mongoTemplate.save(activity);
        }
        return activity;
    }

    public List<ActivityMongo> getRecommendActivities(double lng, double lat, ActivityQuery activityQuery) {
        Double dStep = 100.0;
        Integer tStep = 12;
        if (activityQuery.getDPage() == null) {
            activityQuery.setDPage(0);
        }
        if (activityQuery.getTPage() == null) {
            activityQuery.setTPage(0);
        }
        Criteria categoryCriteria = Criteria.where("category").in(CorgiActivity.CAT_IMAGE, CorgiActivity.CAT_TEXT, CorgiActivity.CAT_VIDEO);
        Criteria statusCriteria = Criteria.where("status").ne(CorgiActivity.DELETED);
        Criteria checkCriteria = Criteria.where("checkStatus").is("pass");
        Criteria topicCriteria = Criteria.where("topics").ne("-1");
        if (!StringUtils.isEmpty(activityQuery.getTopic())) {
            if ("20".equals(activityQuery.getTopic())) {
                topicCriteria = Criteria.where("topics").in("20", "21", "22");
            } else {
                topicCriteria = Criteria.where("topics").is(activityQuery.getTopic());
            }
        }
        List<ActivityMongo> activityMongoList = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
//        while (true) {
//            if (activityQuery.getTPage() * tStep > 24 * 90) {
//                break;
//            }
        //翻页
//            ActivityUtil.addPage(activityQuery);
        //获取距离条件
//            Calendar calendar = Calendar.getInstance();
//            Criteria distanceCriteria = Criteria.where("location").nearSphere(new Point(lng, lat));
//            distanceCriteria.minDistance(dStep * (activityQuery.getDPage() - 1) / RADIUS);
//            distanceCriteria.maxDistance(dStep * activityQuery.getDPage() / RADIUS);
        //获取时间条件
//            calendar.add(Calendar.HOUR, -1 * tStep * activityQuery.getTPage());
//            String startTime = sdf.format(calendar.getTime());
//            calendar.add(Calendar.HOUR, tStep);
//            String endTime = sdf.format(calendar.getTime());
//            Criteria startCriteria = Criteria.where("createTime").gte(startTime);
//            Criteria endCriteria = Criteria.where("createTime").lte(endTime);

//            Criteria queryCriteria = new Criteria().andOperator(statusCriteria, distanceCriteria, startCriteria, endCriteria, categoryCriteria, checkCriteria, topicCriteria);
        Criteria queryCriteria = new Criteria().andOperator(statusCriteria, categoryCriteria, checkCriteria, topicCriteria);
        Query query = new Query(queryCriteria).with(Sort.by(Sort.Direction.DESC, "createTime")).skip(activityQuery.getTPage()).limit(activityQuery.getPageSize());
        activityQuery.setTPage(activityQuery.getTPage() + activityQuery.getPageSize());
        List<ActivityMongo> corgiActivities = mongoTemplate.find(query, ActivityMongo.class);
        if (corgiActivities.size() > 0) {
            List<String> userIds = corgiActivities.stream().map(activityMongo -> activityMongo.getUserId()).collect(Collectors.toList());
            List<String> resultUserIds = corgiUserService.filterUser(userIds, activityQuery);
            corgiActivities = corgiActivities.stream().filter(activityMongo -> resultUserIds.contains(activityMongo.getUserId())).collect(Collectors.toList());
            activityMongoList.addAll(corgiActivities);
//                if (activityMongoList.size() >= activityQuery.getPageSize()) {
//                    break;
//                }
        }
//        }
//        if (activityMongoList.size() > 5 && activityQuery.getRPage() != null) {
//            List<ActivityMongo> businessList = getRecommendBusiness(activityQuery, activityMongoList.size() / 5);
//            activityQuery.setRPage(activityQuery.getRPage() + businessList.size());
//            return mergeActivity(activityMongoList, businessList);
//        }
        return activityMongoList;
    }

    public List<ActivityMongo> getCityRecommendActivity(String city) {
        Criteria categoryCriteria = Criteria.where("category").in(CorgiActivity.CAT_ACTIVITY, CorgiActivity.CAT_BUSINESS);
        Criteria statusCriteria = Criteria.where("status").is(CorgiActivity.CREATED);
        Criteria timeCriteria = new Criteria().orOperator(
                Criteria.where("endTime").gte(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()))
                , Criteria.where(ActivityMongo.SIGN_UP_TIME).gte(new SimpleDateFormat("yyyy/MM/dd HH:mm").format(new Date())));
        Criteria cityCriteria = Criteria.where("city").is(city);
        return mongoTemplate.find(new Query(new Criteria().andOperator(categoryCriteria, statusCriteria, timeCriteria, cityCriteria)), ActivityMongo.class);
    }

    private List<ActivityMongo> mergeActivity(List<ActivityMongo> activityList, List<ActivityMongo> businessList) {
        List<Integer> takenPositions = new ArrayList<>();
        Random random = new Random();
        int bound = activityList.size();
        for (ActivityMongo business : businessList) {
            int position = random.nextInt(bound);
            int index = findPosition(position, bound, takenPositions);
            activityList.add(index, business);
        }
        return activityList;
    }

    //将商户活动随机混入人员活动中，商户活动不能连续
    private int findPosition(int oldPosition, int bound, List<Integer> takenPositions) {
        int step = oldPosition <= bound / 2 ? 1 : -1;
        for (int i = 0; i < takenPositions.size(); i++) {
            if (takenPositions.contains(oldPosition)) {
                oldPosition += step;
            } else {
                break;
            }
        }
        takenPositions.add(oldPosition);

        int offset = 0;
        for (Integer takenPosition : takenPositions) {
            if (takenPosition < oldPosition) {
                offset++;
            }
        }
        return oldPosition + offset;
    }

    private List<ActivityMongo> getRecommendBusiness(ActivityQuery activityQuery, Integer size) {
        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("category").regex(CorgiActivity.CAT_BUSINESS));
        criteriaList.add(Criteria.where("recommend").is("enable"));
        if (!StringUtils.isEmpty(activityQuery.getCity())) {
            criteriaList.add(Criteria.where("city").regex(activityQuery.getCity() + ".*"));
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String time = sdf.format(new Date());
        criteriaList.add(Criteria.where("startTime").lte(time));
        criteriaList.add(Criteria.where("endTime").gte(time));
        Query query = getDescIdQuery(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])), activityQuery.getRPage(), size);
        return mongoTemplate.find(query, ActivityMongo.class);
    }

    public List<ActivityMongo> getCityNearActivities(double lng, double lat, double range, ActivityQuery activityQuery) {
        List<Criteria> criteriaList = new ArrayList<>();
        Criteria distanceCriteria = Criteria.where("location").nearSphere(new Point(lng, lat));

        if (range > 0) {
            distanceCriteria.maxDistance(range / RADIUS);
        }
        criteriaList.add(distanceCriteria);
        Criteria typeCriteria = new Criteria().orOperator(Criteria.where("refActivityId").exists(true)
                , Criteria.where("category").in(CorgiActivity.CAT_BUSINESS, CorgiActivity.CAT_ACTIVITY));
        criteriaList.add(typeCriteria);

        criteriaList.add(Criteria.where("status").is(CorgiActivity.CREATED));

        criteriaList.add(new Criteria().orOperator(Criteria.where("checkStatus").exists(false), Criteria.where("checkStatus").ne("fail")));
        if (CorgiActivity.CAT_BUSINESS.equals(activityQuery.getCategory())
                && !StringUtils.isEmpty(activityQuery.getStartTime())) {
            criteriaList.add(Criteria.where("createTime").gte(activityQuery.getStartTime()));
        }

        Query query = getQueryByCriteria(activityQuery, criteriaList);
        List<ActivityMongo> corgiActivities = mongoTemplate.find(query, ActivityMongo.class);
        log.info("near size... {} ", corgiActivities.size());
        if (CollectionUtils.isNotEmpty(corgiActivities) && !StringUtils.isEmpty(activityQuery.getUserId())) {
            List<String> userIds = new ArrayList<>();
            List<String> barIds = new ArrayList<>();
            for (ActivityMongo mongo : corgiActivities) {
                if (CorgiActivity.CAT_BUSINESS.equals(mongo.getCategory())) {
                    barIds.add(mongo.getUserId());
                } else {
                    userIds.add(mongo.getUserId());
                }
            }
        }
        log.info("near final size... {} ", corgiActivities.size());
        return corgiActivities;
    }

    public List<ActivityMongo> getNearActivities(double lng, double lat, double range, ActivityQuery activityQuery) {
        List<Criteria> criteriaList = new ArrayList<>();
        Criteria distanceCriteria = Criteria.where("location").nearSphere(new Point(lng, lat));

        if (range > 0) {
            distanceCriteria.maxDistance(range / RADIUS);
        }
        criteriaList.add(distanceCriteria);
        Criteria typeCriteria = Criteria.where("category").is(CorgiActivity.CAT_BUSINESS);
        Criteria corgiCriteria = new Criteria().andOperator(Criteria.where("userId").is("69548"), Criteria.where("category").is(CorgiActivity.CAT_ACTIVITY));
        criteriaList.add(new Criteria().orOperator(typeCriteria, corgiCriteria));

        criteriaList.add(Criteria.where("status").is(CorgiActivity.CREATED));

        criteriaList.add(new Criteria().orOperator(Criteria.where("checkStatus").exists(false), Criteria.where("checkStatus").ne("fail")));
//        if (StringUtils.isEmpty(activityQuery.getCategory())) {
//            Criteria imageCriteria = Criteria.where("category").is(CorgiActivity.CAT_IMAGE);
//            Criteria signUpCriteria = Criteria.where(ActivityMongo.SIGN_UP_TIME).gte(new SimpleDateFormat("yyyy/MM/dd HH:mm").format(new Date()));
//            criteriaList.add(new Criteria().orOperator(signUpCriteria, imageCriteria));
//        }
//
//        criteriaList.add(Criteria.where("status").is(CorgiActivity.CREATED));
        if (CorgiActivity.CAT_BUSINESS.equals(activityQuery.getCategory())
                && !StringUtils.isEmpty(activityQuery.getStartTime())) {
            criteriaList.add(Criteria.where("createTime").gte(activityQuery.getStartTime()));
        }
//
//        if (!StringUtils.isEmpty(activityQuery.getCategory())) {
//            criteriaList.add(Criteria.where("category").is(activityQuery.getCategory()));
//        }
//        if (CorgiActivity.CAT_ACTIVITY.equals(activityQuery.getCategory())) {
//            criteriaList.add(Criteria.where(ActivityMongo.SIGN_UP_TIME).gte(new SimpleDateFormat("yyyy/MM/dd HH:mm").format(new Date())));
//        }
        if (!StringUtils.isEmpty(activityQuery.getNotCity())) {
            criteriaList.add(Criteria.where("city").ne(activityQuery.getCity()));
        } else if (!StringUtils.isEmpty(activityQuery.getCity())) {
            criteriaList.add(Criteria.where("city").regex(activityQuery.getCity() + ".*"));
        }
        int skip = 0;
        int size = 1000;
        if (activityQuery.getPageSize() != null && activityQuery.getPageSize() > 0) {
            size = activityQuery.getPageSize();
        }
        if (activityQuery.getPage() != null && activityQuery.getPage() > 0) {
            skip = (activityQuery.getPage() - 1) * size;
        } else if (activityQuery.getOffset() != null && activityQuery.getOffset() > 0) {
            skip = activityQuery.getOffset();
        }
        Criteria queryCriteria = new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
        Query query = new Query(queryCriteria).skip(skip).limit(size);
        if (ActivityQuery.SORT_TIME.equals(activityQuery.getSort())) {
            query.with(Sort.by(Sort.Direction.DESC, "createTime"));
        }
        List<ActivityMongo> corgiActivities = mongoTemplate.find(query, ActivityMongo.class);
        log.info("near size... {} ", corgiActivities.size());
        if (CollectionUtils.isNotEmpty(corgiActivities) && !StringUtils.isEmpty(activityQuery.getUserId())) {
            List<String> userIds = new ArrayList<>();
            List<String> barIds = new ArrayList<>();
            for (ActivityMongo mongo : corgiActivities) {
                if (CorgiActivity.CAT_BUSINESS.equals(mongo.getCategory())) {
                    barIds.add(mongo.getUserId());
                } else {
                    userIds.add(mongo.getUserId());
                }
            }
//            List<String> resultUserIds = corgiUserService.filterUser(userIds, activityQuery);
//            if (CollectionUtils.isEmpty(resultUserIds) && CollectionUtils.isEmpty(barIds)) {
//                return new ArrayList<>();
//            }
//            Iterator<ActivityMongo> iterator = corgiActivities.iterator();
//            while (iterator.hasNext()) {
//                ActivityMongo mongo = iterator.next();
//                if (CorgiActivity.CAT_BUSINESS.equals(mongo.getCategory()) || resultUserIds.contains(mongo.getUserId())) {
//                    continue;
//                }
//                iterator.remove();
//            }
        }
        log.info("near final size... {} ", corgiActivities.size());
        return corgiActivities;
    }

    private Query getQueryByCriteria(ActivityQuery activityQuery, List<Criteria> criteriaList) {
        if (!StringUtils.isEmpty(activityQuery.getType())) {
            if ("其他".equals(activityQuery.getType())) {
                List<String> types = corgiToolService.getActivityTypes();
                for (String type : types) {
                    criteriaList.add(Criteria.where("activityType").ne(type));
                }
            } else {
                criteriaList.add(Criteria.where("activityType").is(activityQuery.getType()));
            }
        }
        if (CollectionUtils.isNotEmpty(activityQuery.getPayType())) {
            criteriaList.add(Criteria.where("payType").in(activityQuery.getPayType()));
        }

        if (!StringUtils.isEmpty(activityQuery.getNotCity())) {
            criteriaList.add(Criteria.where("city").ne(activityQuery.getCity()));
        } else if (!StringUtils.isEmpty(activityQuery.getCity())) {
            criteriaList.add(Criteria.where("city").regex(activityQuery.getCity() + ".*"));
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
//        if (!StringUtils.isEmpty(activityQuery.getStartTime())) {
//            criteriaList.add(Criteria.where(ActivityMongo.SIGN_UP_TIME).gte(activityQuery.getStartTime()));
//        }
        if (!StringUtils.isEmpty(activityQuery.getEndTime())) {
            criteriaList.add(Criteria.where(ActivityMongo.SIGN_UP_TIME).lte(activityQuery.getEndTime()));
        }
        if (!StringUtils.isEmpty(activityQuery.getTopic())) {
            criteriaList.add(Criteria.where("topics").is(activityQuery.getTopic()));
        }

        Criteria queryCriteria = new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
        int skip = 0;
        int size = 1000;
        if (activityQuery.getPageSize() != null && activityQuery.getPageSize() > 0) {
            size = activityQuery.getPageSize();
        }
        if (activityQuery.getPage() != null && activityQuery.getPage() > 0) {
            skip = (activityQuery.getPage() - 1) * size;
        } else if (activityQuery.getOffset() != null && activityQuery.getOffset() > 0) {
            skip = activityQuery.getOffset();
        }

        Query query = new Query(queryCriteria).skip(skip).limit(size);
        if (ActivityQuery.SORT_TIME.equals(activityQuery.getSort())) {
            query.with(Sort.by(Sort.Direction.DESC, "createTime"));
        }

        return query;
    }

    public List<ActivityMongo> getRunningActivities(String userId, Integer start, Integer size) {
        Criteria userCriteria = Criteria.where("userId").is(userId);
        Criteria statusCriteria = Criteria.where("status").is(CorgiActivity.CREATED);
        Criteria signUpCriteria = Criteria.where(ActivityMongo.SIGN_UP_TIME).gte(new SimpleDateFormat("yyyy/MM/dd HH:mm").format(new Date()));
        Query query = getDescIdQuery(new Criteria().andOperator(userCriteria, statusCriteria, signUpCriteria), start, size);
        List<ActivityMongo> corgiActivities = mongoTemplate.find(query, ActivityMongo.class);
        return corgiActivities;
    }

    public List<ActivityMongo> getAllRunningActivities(String userId, Integer start, Integer size) {
        Criteria userCriteria = Criteria.where("userId").is(userId);
        Criteria statusCriteria = Criteria.where("status").ne(CorgiActivity.DELETED);
        Criteria categoryCriteria1 = Criteria.where("category").is(CorgiActivity.CAT_IMAGE);
        Criteria categoryCriteria2 = Criteria.where("category").is(CorgiActivity.CAT_BUSINESS);
        Criteria categoryCriteria3 = Criteria.where("category").is(CorgiActivity.CAT_ATTENDANCE);
        Criteria categoryCriteria4 = Criteria.where("category").is(CorgiActivity.CAT_VIDEO);

        Criteria orCriteria = new Criteria().orOperator(categoryCriteria1, categoryCriteria2, categoryCriteria3, categoryCriteria4);
        Query query = getDescIdQuery(new Criteria().andOperator(userCriteria, statusCriteria, orCriteria), start, size);
        List<ActivityMongo> corgiActivities = mongoTemplate.find(query, ActivityMongo.class);
        return corgiActivities;
    }

    public List<ActivityMongo> getEndedActivities(String userId, Integer start, Integer size) {
        Criteria userCriteria = Criteria.where("userId").is(userId);
        Criteria statusCriteria = new Criteria().orOperator(
                Criteria.where("status").is(CorgiActivity.DELETED),
                Criteria.where(ActivityMongo.SIGN_UP_TIME).lt(new SimpleDateFormat("yyyy/MM/dd HH:mm").format(new Date()))
        );
        Query query = getDescIdQuery(new Criteria().andOperator(userCriteria, statusCriteria), start, size);
        List<ActivityMongo> corgiActivities = mongoTemplate.find(query, ActivityMongo.class);
        return corgiActivities;
    }

    public List<ActivityMongo> queryActivities(CorgiActivity activity, Integer start, Integer size) {
        List<Criteria> criteriaList = getCriteriaList(activity);
        List<ActivityMongo> mongos;
        if (criteriaList.size() > 0) {
            Query query;
            if (CorgiActivity.CAT_VIDEO.equals(activity.getCategory()) && activity.getLikeCount() != null && activity.getLikeCount() < 0) {
                query = getDescLikeCountQuery(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])), start, size);
            } else {
                query = getDescIdQuery(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])), start, size);
            }
            mongos = mongoTemplate.find(query, ActivityMongo.class);
        } else {
            mongos = mongoTemplate.find(new Query().with(Sort.by(Sort.Direction.DESC, "_id")).skip(start).limit(size), ActivityMongo.class);
        }
        if (CollectionUtils.isNotEmpty(mongos)) {
            String nowTime = new SimpleDateFormat("yyyy/MM/dd HH:mm").format(new Date());
            Iterator<ActivityMongo> it = mongos.iterator();
            while (it.hasNext()) {
                ActivityMongo activityMongo = it.next();
                activityMongo.setCurrentTime(nowTime);
//                if (activity.getPeopleCount() > 0) {
//                    long count = corgiUserActivityService.countSignUpUser(activity.getId());
//                    if (count < activity.getPeopleCount()) {
//                        it.remove();
//                    }
//                }
            }
        }
        return mongos;
    }


    public List<ActivityMongo> searchActivity(CorgiActivity activity, Integer start, Integer size) {
        Date now = new Date();
        Criteria statusCriteria = Criteria.where("status").is(CorgiActivity.CREATED);
        if (CorgiActivity.NOT_DELETED.equals(activity.getStatus())) {
            statusCriteria = Criteria.where("status").ne(CorgiActivity.DELETED);
        }
        Criteria titleCriteria = Criteria.where("title").regex("^.*" + activity.getTitle() + ".*$");
        Criteria contentCriteria = Criteria.where("content").regex("^.*" + activity.getTitle() + ".*$");
        Criteria addressCriteria = Criteria.where("address").regex("^.*" + activity.getTitle() + ".*$");
        Criteria signUpCriteria = Criteria.where(ActivityMongo.SIGN_UP_TIME).gte(new SimpleDateFormat("yyyy/MM/dd HH:mm").format(now));
        Criteria endTimeCriteria = Criteria.where("endTime").gte(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(now));
        Criteria contentCri = new Criteria().orOperator(titleCriteria, contentCriteria, addressCriteria);
        Criteria categoryCri = new Criteria().orOperator(endTimeCriteria, signUpCriteria);
        Criteria andCri = new Criteria().andOperator(categoryCri, contentCri, statusCriteria);
        Query query = getDescIdQuery(andCri, start, size);
        List<ActivityMongo> mongos = mongoTemplate.find(query, ActivityMongo.class);
        if (CollectionUtils.isNotEmpty(mongos)) {
            String nowTime = new SimpleDateFormat("yyyy/MM/dd HH:mm").format(new Date());
            for (ActivityMongo activityMongo : mongos) {
                activityMongo.setCurrentTime(nowTime);
            }
        }
        return mongos;
    }

    public long countActivities(CorgiActivity activity) {
        List<Criteria> criteriaList = getCriteriaList(activity);
        if (criteriaList.size() > 0) {
            Query query = new Query(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
            return mongoTemplate.count(query, ActivityMongo.class);
        }
        return mongoTemplate.count(new Query(), ActivityMongo.class);
    }

    public List<ActivityMongo> searchActivity(CorgiActivity activity, double range) {
        Criteria criteriaLocation = Criteria.where("location").withinSphere(new Circle(new Point(activity.getLng(), activity.getLat()), new Distance(range * 100000, Metrics.KILOMETERS)));
        //Criteria criteriaCheckStatus = Criteria.where("checkStatus").ne("fail");
        //Criteria criteriaSignUpTime = Criteria.where(ActivityMongo.SIGN_UP_TIME).gte(activity.getSignUpTime());
        Criteria criteriaStatus = Criteria.where("status").is(CorgiActivity.CREATED);
        Criteria criteriaCategory = Criteria.where("category").is(CorgiActivity.CAT_BUSINESS);
        Query query = getDescIdQuery(new Criteria().andOperator(criteriaLocation, criteriaCategory, criteriaStatus), 0, 20);
        return mongoTemplate.find(query, ActivityMongo.class);
    }

    public List<ActivityMongo> getBarActivity(CorgiActivity activity) {
        List<Criteria> criterias = new ArrayList<>();
        Criteria criteria = Criteria.where("category").is(CorgiActivity.CAT_BUSINESS);
        Criteria criteriaUserId = Criteria.where("userId").is(activity.getUserId());
        criterias.add(criteria);
        criterias.add(criteriaUserId);
        String status = activity.getStatus();
        if (!StringUtils.isEmpty(status)) {
            Criteria criteriaStatus = Criteria.where("status").is(status);
            criterias.add(criteriaStatus);
        }
//        if (!StringUtils.isEmpty(activity.getStartTime())) {
//            Criteria startTime = Criteria.where("startTime").lte(activity.getStartTime());
//            criterias.add(startTime);
//            Criteria endTime = Criteria.where("endTime").gte(activity.getStartTime());
//            criterias.add(endTime);
//        } else if (!StringUtils.isEmpty(activity.getEndTime())) {
//            Criteria endTime = Criteria.where("startTime").gte(activity.getEndTime());
//            criterias.add(endTime);
//        }

        criteria = new Criteria().andOperator(criterias.toArray(new Criteria[0]));
        return mongoTemplate.find(new Query(criteria), ActivityMongo.class);
    }

    public List<ActivityMongo> getActivityByFeed(String mongoId, Criteria criteria,Integer start, Integer size) {
        Query query = new Query().addCriteria(criteria).skip(start).limit(size).with(Sort.by(Sort.Direction.DESC, "mongoId"));
        if (!StringUtils.isEmpty(mongoId)) {
            Criteria idCri = Criteria.where("mongoId").lt(new ObjectId(mongoId));
            query.addCriteria(idCri);
        }
        return mongoTemplate.find(query, ActivityMongo.class);
    }

    public List<ActivityMongo> getActivityByUserIds(List<String> userIds, String status, Integer start, Integer
            size) {
        Criteria c = new Criteria().andOperator(Criteria.where("userId").in(userIds), Criteria.where("checkStatus").ne("fail"));
        if (CorgiActivity.CREATED.equals(status)) {
            Criteria signUp = Criteria.where(ActivityMongo.SIGN_UP_TIME).gte(new SimpleDateFormat("yyyy/MM/dd HH:mm").format(new Date()));
            c = new Criteria().andOperator(c, signUp, Criteria.where("status").ne(CorgiActivity.DELETED));
        } else if (CorgiActivity.ENDED.equals(status)) {
            c = new Criteria().andOperator(c, Criteria.where(ActivityMongo.SIGN_UP_TIME).lte(new SimpleDateFormat("yyyy/MM/dd HH:mm").format(new Date())));
        } else if (CorgiActivity.CAT_IMAGE.equals(status)) {
            Criteria image = Criteria.where("category").in(CorgiActivity.CAT_IMAGE, CorgiActivity.CAT_VIDEO, CorgiActivity.CAT_TEXT);
            c = new Criteria().andOperator(c, image, Criteria.where("status").ne(CorgiActivity.DELETED));
        } else if (CorgiActivity.CAT_VIDEO.equals(status)) {
            Criteria image = Criteria.where("category").in(CorgiActivity.CAT_VIDEO);
            c = new Criteria().andOperator(c, image, Criteria.where("status").ne(CorgiActivity.DELETED));
        } else {
            c = new Criteria().andOperator(c, Criteria.where("status").ne(CorgiActivity.DELETED));
        }
        Query query = getDescIdQuery(c, start, size);
        return mongoTemplate.find(query, ActivityMongo.class);
    }

    public List<ActivityMongo> getAllActivityByUserIds(String loginUserId, List<String> userIds, String
            status, Integer start, Integer size) {
        userIds.add(loginUserId);
        Criteria c = new Criteria().andOperator(Criteria.where("userId").in(userIds), Criteria.where("checkStatus").in("pass"));
        if (CorgiActivity.CREATED.equals(status)) {
            Criteria image = Criteria.where("category").is(CorgiActivity.CAT_IMAGE);
            Criteria video = Criteria.where("category").is(CorgiActivity.CAT_VIDEO);
            Criteria text = Criteria.where("category").is(CorgiActivity.CAT_TEXT);
            Criteria business = Criteria.where("category").is(CorgiActivity.CAT_BUSINESS);
            Criteria attendance = Criteria.where("category").is(CorgiActivity.CAT_ATTENDANCE);
            c = new Criteria().andOperator(c, new Criteria().orOperator(image, business, attendance, text, video), Criteria.where("status").ne(CorgiActivity.DELETED));
        } else if (CorgiActivity.ENDED.equals(status)) {
            c = new Criteria().andOperator(c, Criteria.where(ActivityMongo.SIGN_UP_TIME).lte(new SimpleDateFormat("yyyy/MM/dd HH:mm").format(new Date())));
        } else if (CorgiActivity.CAT_IMAGE.equals(status)) {
            Criteria image = Criteria.where("category").is(CorgiActivity.CAT_IMAGE);
            c = new Criteria().andOperator(c, new Criteria().orOperator(image), Criteria.where("status").ne(CorgiActivity.DELETED));
        } else {
            c = new Criteria().andOperator(c, Criteria.where("status").ne(CorgiActivity.DELETED));
        }
        Query query = getDescIdQuery(c, start, size);
        return mongoTemplate.find(query, ActivityMongo.class);
    }

    public List<ActivityMongo> getActivityIdByUserId(String userId) {
        Query query = new Query(Criteria.where("userId").is(userId));
        query.fields().include("_id");
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

    private Query getDescLikeCountQuery(Criteria criteria, Integer start, Integer size) {
        Query query = new Query(criteria).with(Sort.by(Sort.Direction.DESC, "likeCount")).skip(start).limit(size);
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
                    if ("id".equals(fieldName)) {
                        criteriaList.add(Criteria.where("mongoId").is(new ObjectId(value.toString())));
                    } else if ("status".equals(fieldName)) {
                        String currentTime = activity.getCurrentTime();
                        if (StringUtils.isEmpty(currentTime)) {
                            currentTime = new SimpleDateFormat("yyyy/MM/dd HH:mm").format(new Date());
                        }
                        if (CorgiActivity.ENDED.equals(value)) {
                            criteriaList.add(Criteria.where(ActivityMongo.SIGN_UP_TIME).lte(currentTime));
                            criteriaList.add(Criteria.where("status").ne(CorgiActivity.DELETED));
                        } else if (CorgiActivity.CREATED.equals(value)) {
                            criteriaList.add(Criteria.where(ActivityMongo.SIGN_UP_TIME).gte(currentTime));
                            criteriaList.add(Criteria.where("status").ne(CorgiActivity.DELETED));
                        } else if (CorgiActivity.NOT_DELETED.equals(value)) {
                            criteriaList.add(Criteria.where("status").ne(CorgiActivity.DELETED));
                        } else {
                            criteriaList.add(Criteria.where("status").is(value));
                        }
                    } else if ("city".equals(fieldName)) {
                        criteriaList.add(Criteria.where("city").regex("^" + value + ".*"));
                    } else if ("title".equals(fieldName)) {
                        criteriaList.add(new Criteria().
                                orOperator(Criteria.where("title").regex("^" + value + ".*"),
                                        Criteria.where("content").regex("^" + value + ".*")));
                    } else if ("createTime".equals(fieldName)) {
                        criteriaList.add(Criteria.where("createTime").regex("^" + value + ".*"));
                    } else if ("updateTime".equals(fieldName)) {
                        criteriaList.add(Criteria.where("updateTime").regex("^" + value + ".*"));
                    } else if (ActivityMongo.SIGN_UP_TIME.equals(fieldName)) {
                        criteriaList.add(Criteria.where(ActivityMongo.SIGN_UP_TIME).lte(value));
                    } else if (LIKE_FIELDS.contains(fieldName)) {
                        criteriaList.add(Criteria.where(field.getName()).regex("^.*" + value + ".*$"));
                    } else if ("category".equals(fieldName) && CorgiActivity.CAT_BUSINESS.equals(value)) {
                        Criteria corgiCriteria = new Criteria().andOperator(Criteria.where("userId").is("69548"), Criteria.where("category").is(CorgiActivity.CAT_ACTIVITY));
                        criteriaList.add(new Criteria().orOperator(Criteria.where(fieldName).is(value), corgiCriteria));
                    } else if (int.class.equals(field.getType()) && (int) value != 0) {
                        criteriaList.add(Criteria.where(field.getName()).is(value));
                    } else if (!int.class.equals(field.getType())) {
                        criteriaList.add(Criteria.where(field.getName()).is(value));
                    }
                }
            } catch (IllegalAccessException e) {
                log.error(e.getMessage(), e);
            }
        }
        return criteriaList;
    }

    public void deleteActivityByUserId(String userId) {
        Query query = new Query(Criteria.where("userId").is(userId));
        List<ActivityMongo> activityMongoList = mongoTemplate.find(query, ActivityMongo.class);
        for (ActivityMongo mongo : activityMongoList) {
            this.deleteActivityById(mongo.getMongoId().toString());
            corgiFavorActivityService.deleteByActivityId(mongo.getMongoId().toString());
            corgiUserActivityService.deleteActivityCreator(mongo.getMongoId().toString());
        }
    }

    public void removeActivity(String activityId) {
        corgiFavorActivityService.deleteByActivityId(activityId);
        this.deleteActivityById(activityId);
    }

    public void updateActivityByColumn(String activityId, String column, String value) {
        Update update;
        if ("likeCount".equals(column)) {
            update = new Update().set(column, Long.valueOf(value));
        } else {
            update = new Update().set(column, value);
        }
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

    public void refreshActivity() {
        Query query = new Query(Criteria.where("refActivityId").exists(true));
        List<ActivityMongo> mongos = mongoTemplate.find(query, ActivityMongo.class);
        for (ActivityMongo mongo : mongos) {
            String activityId = mongo.getRefActivityId();
            if (!StringUtils.isEmpty(activityId)) {
                ActivityMongo activityMongo = mongoTemplate.findById(new ObjectId(activityId), ActivityMongo.class);
                if (!StringUtils.isEmpty(activityMongo.getActivityType())) {
                    Query idQuery = new Query(Criteria.where("mongoId").is(mongo.getMongoId()));
                    Update update = new Update().set("activityType", activityMongo.getActivityType());
                    mongoTemplate.updateFirst(idQuery, update, ActivityMongo.class);
                }
            }
        }
    }

}
