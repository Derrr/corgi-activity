package com.corgi.dao;


import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.config.annotation.Reference;
import com.corgi.activity.entity.CorgiActivity;
import com.corgi.entity.ActivityMongo;
import com.corgi.entity.ActivityQuery;
import com.corgi.entity.UserMongo;
import com.corgi.entity.UserOnlineMongo;
import com.corgi.user.api.CorgiFavorActivityService;
import com.corgi.user.api.CorgiToolService;
import com.corgi.user.api.CorgiUserActivityService;
import com.corgi.user.api.CorgiUserService;
import com.corgi.user.entity.UserDetail;
import com.corgi.user.entity.UserQuery;
import com.corgi.util.ActivityUtil;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
public class CorgiUserDao {
    @Autowired
    MongoTemplate mongoTemplate;
    @Reference
    private CorgiUserService corgiUserService;

    private final static Double RADIUS = 6371.0;

    public void updateUser(UserDetail userDetail) {
        UserMongo userMongo = new UserMongo(userDetail);
        UserOnlineMongo userOnlineMongo = new UserOnlineMongo(userDetail);
        BeanUtils.copyProperties(userDetail, userMongo);
        BeanUtils.copyProperties(userDetail, userOnlineMongo);
        mongoTemplate.save(userOnlineMongo);
        mongoTemplate.findAllAndRemove(new Query(Criteria.where("userId").is(userDetail.getUserId())), UserMongo.class);
        mongoTemplate.save(userMongo);
    }

    public List<UserDetail> findUser(UserQuery userQuery) {
        Query query = new Query(Criteria.where("location").nearSphere(new Point(userQuery.getLng(), userQuery.getLat())))
                .limit(6);
        List<UserDetail> userDetails = new ArrayList<>();
        List<UserOnlineMongo> userOnlines = mongoTemplate.find(query, UserOnlineMongo.class);
        if (userOnlines.size() < 6) {
            List<UserMongo> users = mongoTemplate.find(query, UserMongo.class);
            for (UserMongo userMongo : users) {
                userDetails.add(userMongo.getUserDetail());
            }
        } else {
            for (UserOnlineMongo onlineMongo : userOnlines) {
                userDetails.add(onlineMongo.getUserDetail());
            }
        }
        return userDetails;
    }

}
