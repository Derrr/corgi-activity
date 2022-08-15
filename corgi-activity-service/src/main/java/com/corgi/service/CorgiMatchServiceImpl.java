package com.corgi.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.corgi.activity.api.CorgiMatchService;
import com.corgi.dao.CorgiUserDao;
import com.corgi.user.entity.UserDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
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
        log.info("adding..."+userDetail);
        corgiUserDao.updateUser(userDetail);
//        if (redisTemplate.opsForValue().setIfAbsent("2", "2", 24l, TimeUnit.HOURS)) {
//            corgiUserDao.addIndex();
//        }
    }
}
