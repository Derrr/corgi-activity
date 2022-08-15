package com.corgi.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.corgi.activity.api.CorgiBlackActivityService;
import com.corgi.activity.api.CorgiMatchService;
import com.corgi.activity.entity.CorgiActivity;
import com.corgi.dao.CorgiActivityDao;
import com.corgi.dao.CorgiUserDao;
import com.corgi.entity.ActivityMongo;
import com.corgi.user.api.CorgiFavorActivityService;
import com.corgi.user.api.CorgiUserActivityService;
import com.corgi.user.entity.UserDetail;
import com.corgi.user.entity.UserProfile;
import com.corgi.user.entity.UserSignUp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author tairanliu
 */
@Slf4j
@Service(interfaceClass = CorgiMatchService.class)
@Component
public class CorgiMatchServiceImpl implements CorgiMatchService {
    @Autowired
    private CorgiUserDao corgiUserDao;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public void updateUser(UserDetail userDetail) {
        if (redisTemplate.opsForValue().setIfAbsent("1", "1", 24l, TimeUnit.HOURS)) {
            corgiUserDao.addIndex();
        }
        corgiUserDao.updateUser(userDetail);
    }
}
