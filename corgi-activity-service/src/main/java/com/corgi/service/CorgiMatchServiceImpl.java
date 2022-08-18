package com.corgi.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.corgi.activity.api.CorgiMatchService;
import com.corgi.dao.CorgiUserDao;
import com.corgi.user.entity.UserDetail;
import com.corgi.user.entity.UserMatchItem;
import com.corgi.user.entity.UserQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;

import java.util.List;


/**
 * @author tairanliu
 */
@Slf4j
@Service(interfaceClass = CorgiMatchService.class)
@Component
public class CorgiMatchServiceImpl implements CorgiMatchService {
    @Autowired
    private CorgiUserDao corgiUserDao;

    @Override
    public void updateUser(UserDetail userDetail) {
        corgiUserDao.updateUser(userDetail);
    }

    @Override
    public List<UserMatchItem> getMatchItems(UserQuery userQuery) {
        return corgiUserDao.findUser(userQuery);
    }

    @Override
    public void deleteUser(String userId) {
        corgiUserDao.deleteUser(userId);
    }
}
