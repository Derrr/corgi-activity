package com.corgi.dao;


import com.alibaba.dubbo.config.annotation.Reference;
import com.corgi.entity.UserMongo;
import com.corgi.entity.UserMongoBase;
import com.corgi.entity.UserOnlineMongo;
import com.corgi.user.api.CorgiExtraService;
import com.corgi.user.api.CorgiUserService;
import com.corgi.user.entity.UserDetail;
import com.corgi.user.entity.UserExtra;
import com.corgi.user.entity.UserMatchItem;
import com.corgi.user.entity.UserQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author tairanliu
 */
@Slf4j
@Component
public class CorgiUserDao {
    final static String INTERESTS = "interests";
    final static String NO_INTERESTS = "no_interests";

    final static String FACE_INTERESTS = "face_interests";
    final static String FACE_NO_INTERESTS = "face_no_interests";
    final static String NO_FACE_INTERESTS = "no_face_interests";
    final static String NO_FACE_NO_INTERESTS = "no_face_no_interests";

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Reference
    private CorgiUserService corgiUserService;
    @Reference
    private CorgiExtraService corgiExtraService;

    private final static Double RADIUS = 6371.0;

    public void updateUser(UserDetail userDetail) {
        if (userDetail.getLng() != null && userDetail.getLng() < 180
                && userDetail.getLat() != null && userDetail.getLat() < 90) {
            UserMongo userMongo = new UserMongo(userDetail);
            UserExtra userExtra = corgiExtraService.getUserExtra(userDetail.getUserId());
            UserOnlineMongo userOnlineMongo = new UserOnlineMongo(userDetail);
            BeanUtils.copyProperties(userDetail, userMongo);
            BeanUtils.copyProperties(userDetail, userOnlineMongo);
            userMongo.setUserExtra(userExtra);
            userOnlineMongo.setUserExtra(userExtra);
            mongoTemplate.save(userOnlineMongo);
            mongoTemplate.findAllAndRemove(new Query(Criteria.where("userId").is(userDetail.getUserId())), UserMongo.class);
            mongoTemplate.save(userMongo);
        }
    }

    public void deleteUser(String userId) {
        mongoTemplate.findAllAndRemove(new Query(Criteria.where("userId").is(userId)), UserMongo.class);
    }

    public List<UserMatchItem> findUser(UserQuery userQuery) {
        Query query = this.getQuery(userQuery);
        List<UserOnlineMongo> onlineMongos = mongoTemplate.find(query, UserOnlineMongo.class);
        List<UserMongo> userMongos = mongoTemplate.find(query, UserMongo.class);
        UserDetail detail = corgiUserService.getUserDetailBasic(userQuery.getUserId());
        if (UserDetail.VERIFIED.equals(detail.getAvatarCheckStatus()) || "normal".equals(detail.getAvatarCheckStatus())) {
            return filterFace(userQuery, onlineMongos, userMongos);
        } else {
            return filterNoFace(userQuery, onlineMongos, userMongos);
        }
    }

    private List<UserMatchItem> filterFace(UserQuery userQuery, List<UserOnlineMongo> onlineMongos, List<UserMongo> userMongos) {
        UserExtra userExtra = corgiExtraService.getUserExtra(userQuery.getUserId());
        List<String> interests = Arrays.asList(userExtra.getInterests().split(","));
        List<UserMatchItem> items = new ArrayList<>();
        List<String> userIds = new ArrayList<>();
        if (!CollectionUtils.isEmpty(interests)) {
            onlineMongos = (List<UserOnlineMongo>) this.filterUsers(onlineMongos, userQuery, FACE_INTERESTS, interests, items, userIds, 6);
            ;
            if (items.size() >= 6) {
                return items;
            }
            userMongos = (List<UserMongo>) this.filterUsers(userMongos, userQuery, FACE_INTERESTS, interests, items, userIds, 6 - items.size());
            if (items.size() >= 6) {
                return items;
            }
        }
        onlineMongos = (List<UserOnlineMongo>) this.filterUsers(onlineMongos, userQuery, FACE_NO_INTERESTS, interests, items, userIds, 6 - items.size());
        if (items.size() >= 6) {
            return items;
        }
        userMongos = (List<UserMongo>) this.filterUsers(userMongos, userQuery, FACE_NO_INTERESTS, interests, items, userIds, 6 - items.size());
        if (items.size() >= 6) {
            return items;
        }

        if (!CollectionUtils.isEmpty(interests)) {
            onlineMongos = (List<UserOnlineMongo>) this.filterUsers(onlineMongos, userQuery, NO_FACE_INTERESTS, interests, items, userIds, 6);
            if (items.size() >= 6) {
                return items;
            }
            userMongos = (List<UserMongo>) this.filterUsers(userMongos, userQuery, NO_FACE_INTERESTS, interests, items, userIds, 6 - items.size());
            if (items.size() >= 6) {
                return items;
            }
        }
        this.filterUsers(onlineMongos, userQuery, NO_FACE_NO_INTERESTS, interests, items, userIds, 6 - items.size());
        if (items.size() >= 6) {
            return items;
        }
        this.filterUsers(userMongos, userQuery, NO_FACE_NO_INTERESTS, interests, items, userIds, 6 - items.size());
        return items;

    }

    private List<UserMatchItem> filterNoFace(UserQuery userQuery, List<UserOnlineMongo> onlineMongos, List<UserMongo> userMongos) {
        UserExtra userExtra = corgiExtraService.getUserExtra(userQuery.getUserId());
        List<String> interests = Arrays.asList(userExtra.getInterests().split(","));
        List<UserMatchItem> items = new ArrayList<>();
        List<String> userIds = new ArrayList<>();
        if (!CollectionUtils.isEmpty(interests)) {
            onlineMongos = (List<UserOnlineMongo>) this.filterUsers(onlineMongos, userQuery, INTERESTS, interests, items, userIds, 6);
            if (items.size() >= 6) {
                return items;
            }
            userMongos = (List<UserMongo>) this.filterUsers(userMongos, userQuery, INTERESTS, interests, items, userIds, 6 - items.size());
            if (items.size() >= 6) {
                return items;
            }
        }
        this.filterUsers(onlineMongos, userQuery, NO_INTERESTS, interests, items, userIds, 6 - items.size());
        if (items.size() >= 6) {
            return items;
        }
        this.filterUsers(userMongos, userQuery, NO_INTERESTS, interests, items, userIds, 6 - items.size());
        return items;
    }

    private Query getQuery(UserQuery query) {

        Query q = new Query().limit(5000);
        UserDetail detail = corgiUserService.getUserDetailBasic(query.getUserId());
        if (!UserDetail.VERIFIED.equals(detail.getAvatarCheckStatus()) && !"normal".equals(detail.getAvatarCheckStatus())) {
            q.addCriteria(new Criteria().andOperator(Criteria.where("avatarCheckStatus").ne(UserDetail.VERIFIED), Criteria.where("avatarCheckStatus").ne("normal")));
        }
        q.addCriteria(Criteria.where("location").nearSphere(new Point(query.getLng(), query.getLat())));
        if (query.getRange() != null && query.getRange() > 0 && query.getRange() < 100) {
            q.addCriteria(Criteria.where("location").maxDistance(query.getRange()/111.12));
        }
        q.addCriteria(Criteria.where("userId").ne(query.getUserId()));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        if (!CollectionUtils.isEmpty(query.getDateStatus())) {
            query.getDateStatus().add("");
            q.addCriteria(Criteria.where("dateStatus").in(query.getDateStatus()));
        } else {
            q.addCriteria(Criteria.where("dateStatus").ne("免打扰"));
        }
        if (!CollectionUtils.isEmpty(query.getGroup())) {
            q.addCriteria(Criteria.where("group").in(query.getGroup()));
        }
        if (!CollectionUtils.isEmpty(query.getRole())) {
            q.addCriteria(Criteria.where("role").in(query.getRole()));
        }
        if (!CollectionUtils.isEmpty(query.getAim())) {
            q.addCriteria(Criteria.where("aim").in(query.getAim()));
        }
        if (!CollectionUtils.isEmpty(query.getProfession())) {
            q.addCriteria(Criteria.where("profession").in(query.getProfession()));
        }
        if (!CollectionUtils.isEmpty(query.getEducation())) {
            q.addCriteria(Criteria.where("education").in(query.getEducation()));
        }
        if (!CollectionUtils.isEmpty(query.getXp())) {
            q.addCriteria(Criteria.where("xpList").in(query.getXp()));
        }
        if (!CollectionUtils.isEmpty(query.getInterests())) {
            q.addCriteria(Criteria.where("interestList").in(query.getInterests()));
        }
        if (!CollectionUtils.isEmpty(query.getTags())) {
            q.addCriteria(Criteria.where("tagList").in(query.getTags()));
        }

        String startYear = "";
        String endYear = "";
        if (query.getStartAge() != null && query.getStartAge() > 18) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.YEAR, -1 * query.getStartAge());
            endYear = sdf.format(calendar.getTime());
        }
        if (query.getEndAge() != null && query.getEndAge() < 70) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.YEAR, -1 * query.getEndAge());
            startYear = sdf.format(calendar.getTime());
        }
        if (!StringUtils.isEmpty(startYear) || !StringUtils.isEmpty(endYear)) {
            Criteria yearCri = Criteria.where("birthday");
            if (!StringUtils.isEmpty(startYear)) {
                yearCri.gt(startYear);
            }
            if (!StringUtils.isEmpty(endYear)) {
                yearCri.lt(endYear);
            }
            q.addCriteria(yearCri);
        }
        if ("verify".equals(query.getType())) {
            q.addCriteria(Criteria.where("avatarCheckStatus").is("verified"));
        }
        return q;
    }

    private List<? extends UserMongoBase> filterUsers(List<? extends UserMongoBase> mongos,
                                                      UserQuery userQuery,
                                                      String filterType,
                                                      List<String> interets,
                                                      List<UserMatchItem> result,
                                                      List<String> userIds,
                                                      Integer size) {
        String userId = userQuery.getUserId();
        String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String viewKey = "user_match_view_" + dateStr + userId;
        String matchKey = "user_match_" + userId;
        List<UserMongoBase> remain = new ArrayList<>();
        try {
            List<String> matchViews = redisTemplate.opsForList().range(viewKey, 0, -1);
            List<String> matchUsers = redisTemplate.opsForList().range(matchKey, 0, -1);
            Long nowTime = System.currentTimeMillis();
            Long threshold = nowTime - 14 * 24 * 3600 * 1000l;
            for (UserMongoBase mongo : mongos) {
                if (matchViews.contains(mongo.getUserId())) {
                    continue;
                }
                if (userIds.contains(mongo.getUserId())) {
                    continue;
                }
                boolean contains = false;
                for (String matchStr : matchUsers) {
                    String[] matchArr = matchStr.split("-");
                    String matchId = matchArr[0];
                    Long matchTime = 0l;
                    try {
                        matchTime = Long.valueOf(matchArr[1]);
                    } catch (Exception e) {
                        redisTemplate.opsForList().remove(matchKey, 1, matchStr);
                    }
                    if (matchTime < threshold) {
                        redisTemplate.opsForList().remove(matchKey, 1, matchStr);
                    }
                    if (mongo.getUserId().equals(matchId)) {
                        contains = true;
                        break;
                    }
                }
                redisTemplate.expire(matchKey, 14l, TimeUnit.DAYS);
                if (contains) {
                    continue;
                }
                boolean hasInterest = false;
                List<String> userInterest = mongo.getInterestList();
                if (!CollectionUtils.isEmpty(userInterest)) {
                    for (String interest : interets) {
                        if (userInterest.contains(interest)) {
                            hasInterest = true;
                            break;
                        }
                    }
                }
                boolean hasFace = "normal".equals(mongo.getAvatarCheckStatus()) || UserDetail.VERIFIED.equals(mongo.getAvatarCheckStatus());
                if (filterType.equals(INTERESTS)) {
                    if (hasFace) {
                        continue;
                    }
                    if (!hasInterest) {
                        remain.add(mongo);
                        continue;
                    }
                }
                if (filterType.equals(NO_INTERESTS)) {
                    if (hasFace) {
                        continue;
                    }
                    if (hasInterest) {
                        remain.add(mongo);
                        continue;
                    }
                }
                if (filterType.equals(FACE_INTERESTS)) {
                    if (!hasFace || !hasInterest) {
                        remain.add(mongo);
                        continue;
                    }
                }
                if (filterType.equals(NO_FACE_INTERESTS)) {
                    if (hasFace || !hasInterest) {
                        remain.add(mongo);
                        continue;
                    }
                }
                if (filterType.equals(FACE_NO_INTERESTS)) {
                    if (!hasFace || hasInterest) {
                        remain.add(mongo);
                        continue;
                    }
                }
                if (filterType.equals(NO_FACE_NO_INTERESTS)) {
                    if (hasFace || hasInterest) {
                        remain.add(mongo);
                        continue;
                    }
                }

                UserMatchItem item = new UserMatchItem();

                BeanUtils.copyProperties(mongo, item);
                item.setDistance(this.getDistance(mongo.getLng(), mongo.getLat(), userQuery));
                if (StringUtils.isEmpty(item.getDateStatus())) {
                    item.setDateStatus("想聊天");
                }
                if (StringUtils.isEmpty(item.getAvatarStatus()) || "-".equals(item.getAvatarStatus())) {
                    item.setAvatarStatus("");
                } else if ("influencer".equals(item.getAvatarStatus())) {
                    item.setAvatarStatus("influencer");
                } else if (dateStr.compareTo(item.getAvatarStatus()) <= 0) {
                    item.setAvatarStatus("vip");
                } else {
                    item.setAvatarStatus("");
                }
                item.setTimeShow("本周");
                Long timestamp = mongo.getTime();
                if (timestamp != null) {
                    Long diff = nowTime - timestamp;
                    if (diff < 5 * 60 * 1000) {
                        item.setTimeShow("在线");
                    } else if (diff < 2 * 3600 * 1000) {
                        item.setTimeShow("刚刚");
                    } else if (diff < 3 * 24 * 3600 * 1000) {
                        item.setTimeShow("今日");
                    }
                }
                result.add(item);
                userIds.add(item.getUserId());
                if (result.size() >= size) {
                    break;
                }
            }
        } catch (Exception e) {
            redisTemplate.delete(matchKey);
            redisTemplate.delete(viewKey);
        }
        return remain;
    }

    private String getDistance(Double lng, Double lat, UserQuery userQuery) {
        String distance = "0km";
        try {
            double radLat1 = rad(lat);
            double radLat2 = rad(userQuery.getLat());
            double a = radLat1 - radLat2;

            double b = rad(lng) - rad(userQuery.getLng());

            double s = RADIUS * 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
            Long dis = Math.round(s);
            if (dis > 100) {
                distance = ">100km";
            } else {
                distance = dis + "km";
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return distance;
    }

    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }
}
