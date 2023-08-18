package com.corgi.dao;


import com.alibaba.dubbo.config.annotation.Reference;
import com.corgi.entity.UserMongo;
import com.corgi.entity.UserMongoBase;
import com.corgi.user.api.CorgiBlacklistService;
import com.corgi.user.api.CorgiExtraService;
import com.corgi.user.api.CorgiUserService;
import com.corgi.user.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
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
import java.util.stream.Collectors;


/**
 * @author tairanliu
 */
@Slf4j
@Component
public class CorgiUserDao {

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
    @Reference
    private CorgiBlacklistService corgiBlacklistService;

    private final static Double RADIUS = 6371.0;

    public void updateUser(UserDetail userDetail) {
        mongoTemplate.findAllAndRemove(new Query(Criteria.where("userId").is(userDetail.getUserId())), UserMongo.class);
        if (userDetail.getLng() != null && userDetail.getLng() < 180
                && userDetail.getLat() != null && userDetail.getLat() < 90) {
            UserExtra userExtra = corgiExtraService.getUserExtra(userDetail.getUserId());
            UserMongo userMongo = new UserMongo(userDetail);
            BeanUtils.copyProperties(userDetail, userMongo);
            userMongo.setUserExtra(userExtra);
            mongoTemplate.save(userMongo);
        }
    }

    public void deleteUser(String userId) {
        mongoTemplate.findAllAndRemove(new Query(Criteria.where("userId").is(userId)), UserMongo.class);
    }

    public List<String> getNearbyUserIds(UserQuery userQuery) {
        Query query = this.getQuery(userQuery, false, false, null);
        List<UserMongo> userMongos = mongoTemplate.find(query, UserMongo.class);
        if (!CollectionUtils.isEmpty(userMongos)) {
            return userMongos.stream().map(u -> u.getUserId()).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public List<UserMatchItem> findUser(UserQuery userQuery) {
        UserDetail detail = corgiUserService.getUserDetailBasic(userQuery.getUserId());
        UserExtra userExtra = corgiExtraService.getUserExtra(userQuery.getUserId());
        List<String> interests = new ArrayList<>();
        if (!StringUtils.isEmpty(userExtra.getInterests())) {
            interests = Arrays.asList(userExtra.getInterests().split(","));
        }
        if (UserDetail.VERIFIED.equals(detail.getAvatarCheckStatus()) || "normal".equals(detail.getAvatarCheckStatus())) {
            return filterFace(userQuery, interests);
        } else {
            return filterNoFace(userQuery, interests);
        }
    }

    private List<UserMatchItem> filterFace(UserQuery userQuery, List<String> interests) {
        List<UserMatchItem> items = new ArrayList<>();
        List<String> userIds = new ArrayList<>();
        List<UserMongo> userMongos;
        if (!CollectionUtils.isEmpty(interests)) {
            userMongos = this.getUserMongos(userQuery, interests, FACE_INTERESTS);
            this.filterUsers(userMongos, userQuery, FACE_INTERESTS, interests, items, userIds, 6 - items.size());
            if (items.size() >= 6) {
                return items;
            }
        }
        userMongos = this.getUserMongos(userQuery, interests, FACE_NO_INTERESTS);
        this.filterUsers(userMongos, userQuery, FACE_NO_INTERESTS, interests, items, userIds, 6 - items.size());
        if (items.size() >= 6) {
            return items;
        }

        if (!CollectionUtils.isEmpty(interests)) {
            userMongos = this.getUserMongos(userQuery, interests, NO_FACE_INTERESTS);
            this.filterUsers(userMongos, userQuery, NO_FACE_INTERESTS, interests, items, userIds, 6 - items.size());
            if (items.size() >= 6) {
                return items;
            }
        }
        userMongos = this.getUserMongos(userQuery, interests, NO_FACE_NO_INTERESTS);
        this.filterUsers(userMongos, userQuery, NO_FACE_NO_INTERESTS, interests, items, userIds, 6 - items.size());
        return items;

    }

    private List<UserMatchItem> filterNoFace(UserQuery userQuery, List<String> interests) {
        List<UserMatchItem> items = new ArrayList<>();
        List<String> userIds = new ArrayList<>();
        List<UserMongo> userMongos;
        if (!CollectionUtils.isEmpty(interests)) {
            userMongos = this.getUserMongos(userQuery, interests, NO_FACE_INTERESTS);
            this.filterUsers(userMongos, userQuery, NO_FACE_INTERESTS, interests, items, userIds, 6 - items.size());
            if (items.size() >= 6) {
                return items;
            }
        }
        userMongos = this.getUserMongos(userQuery, interests, NO_FACE_NO_INTERESTS);
        this.filterUsers(userMongos, userQuery, NO_FACE_NO_INTERESTS, interests, items, userIds, 6 - items.size());
        return items;
    }

    private List<UserMongo> getUserMongos(UserQuery userQuery, List<String> interests, String filterType) {
        Query query = new Query();
        if (FACE_INTERESTS.equals(filterType)) {
            query = this.getQuery(userQuery, true, true, interests);
        }
        if (FACE_NO_INTERESTS.equals(filterType)) {
            query = this.getQuery(userQuery, true, false, interests);
        }
        if (NO_FACE_INTERESTS.equals(filterType)) {
            query = this.getQuery(userQuery, false, true, interests);
        }
        if (NO_FACE_NO_INTERESTS.equals(filterType)) {
            query = this.getQuery(userQuery, false, false, interests);
        }
        return mongoTemplate.find(query, UserMongo.class);
    }

    private Query getQuery(UserQuery query, boolean hasFace, boolean hasInterests, List<String> interests) {

        Query q = new Query().with(Sort.by(Sort.Direction.DESC, "id")).limit(5000);
        if (query.getRange() == null || query.getRange() <= 0 || query.getRange() > 100) {
            q.addCriteria(Criteria.where("location").nearSphere(new Point(query.getLng(), query.getLat())));
        } else {
            q.addCriteria(Criteria.where("location").nearSphere(new Point(query.getLng(), query.getLat())).maxDistance(query.getRange() / 6371.0));
        }
        if (interests == null) {
            q = new Query().limit(200);
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
        if (interests != null) {
            if (hasInterests) {
                List<String> i = query.getInterests();
                if (i == null) {
                    i = new ArrayList<>();
                }
                i.addAll(interests);
                q.addCriteria(Criteria.where("interestList").in(i));
            } else {
                if (!CollectionUtils.isEmpty(query.getInterests())) {
                    q.addCriteria(Criteria.where("interestList").in(query.getInterests()));
                }
                if (!CollectionUtils.isEmpty(interests)) {
                    q.addCriteria(Criteria.where("interestList").nin(interests));
                }
            }
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
        } else if (hasFace) {
            q.addCriteria(new Criteria().orOperator(Criteria.where("avatarCheckStatus").is("verified"), Criteria.where("avatarCheckStatus").is("normal")));
        } else if (interests != null) {
            q.addCriteria(new Criteria().andOperator(Criteria.where("avatarCheckStatus").ne("verified"), Criteria.where("avatarCheckStatus").ne("normal")));
        }
        return q;
    }

    private List<? extends UserMongoBase> filterUsers(List<UserMongo> mongos,
                                                      UserQuery userQuery,
                                                      String filterType,
                                                      List<String> interests,
                                                      List<UserMatchItem> result,
                                                      List<String> userIds,
                                                      Integer size) {
        log.info("item size:{},mongo size:{}, userIds size:{}", result.size(), mongos.size(), userIds.size());
        String userId = userQuery.getUserId();
        String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String viewKey = "user_match_view_" + dateStr + userId;
        String matchKey = "user_match_" + userId;
        List<UserMongoBase> remain = new ArrayList<>();
        try {
            List<String> matchViews = redisTemplate.opsForList().range(viewKey, 0, -1);
            List<String> matchUsers = redisTemplate.opsForList().range(matchKey, 0, -1);
            List<String> blackIds = getBlackIds(userId);
            Long nowTime = System.currentTimeMillis();
            Long threshold = nowTime - 14 * 24 * 3600 * 1000l;
            for (UserMongo mongo : mongos) {
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
//                boolean hasInterest = false;
//                List<String> userInterest = mongo.getInterestList();
//                if (!CollectionUtils.isEmpty(userInterest)) {
//                    for (String interest : interests) {
//                        if (userInterest.contains(interest)) {
//                            hasInterest = true;
//                            break;
//                        }
//                    }
//                }
//                boolean hasFace = "normal".equals(mongo.getAvatarCheckStatus()) || UserDetail.VERIFIED.equals(mongo.getAvatarCheckStatus());
//                if (filterType.equals(FACE_INTERESTS)) {
//                    if (!hasFace || !hasInterest) {
//                        remain.add(mongo);
//                        continue;
//                    }
//                }
//                if (filterType.equals(NO_FACE_INTERESTS)) {
//                    if (hasFace || !hasInterest) {
//                        remain.add(mongo);
//                        continue;
//                    }
//                }
//                if (filterType.equals(FACE_NO_INTERESTS)) {
//                    if (!hasFace || hasInterest) {
//                        remain.add(mongo);
//                        continue;
//                    }
//                }
//                if (filterType.equals(NO_FACE_NO_INTERESTS)) {
//                    if (hasFace || hasInterest) {
//                        remain.add(mongo);
//                        continue;
//                    }
//                }

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
        log.info("item size:{}, userIds size:{}", result.size(), userIds.size());
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

    private List<String> getBlackIds(String userId) {
        String blackKey = "black_cache_" + userId;
        List<String> blackUserIds = new ArrayList<>();
        if (!redisTemplate.hasKey(blackKey)) {
            List<UserBasic> basicList = corgiBlacklistService.getBlackUser(userId);
            List<String> beBlackedIds = corgiBlacklistService.getBeBlacked(userId);
            if (!CollectionUtils.isEmpty(basicList)) {
                for (UserBasic basic : basicList) {
                    blackUserIds.add(basic.getUserId());
                }
            }
            if (!CollectionUtils.isEmpty(beBlackedIds)) {
                blackUserIds.addAll(beBlackedIds);
            }
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, -30);
            if (CollectionUtils.isEmpty(blackUserIds)) {
                redisTemplate.delete(blackKey);
                redisTemplate.opsForList().leftPush(blackKey, "null");
            } else {
                redisTemplate.opsForList().leftPushAll(blackKey, blackUserIds);
            }
            redisTemplate.expire(blackKey, 1L, TimeUnit.DAYS);
        } else {
            blackUserIds = redisTemplate.opsForList().range(blackKey, 0, -1);
            if (blackUserIds.size() == 1 && "null".equals(blackUserIds.get(0))) {
                return new ArrayList<>();
            }
        }
        return blackUserIds;
    }

    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }
}
