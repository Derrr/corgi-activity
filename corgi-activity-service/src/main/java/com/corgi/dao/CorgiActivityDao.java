package com.corgi.dao;


import com.corgi.activity.entity.CorgiActivity;
import com.corgi.entity.ActivityMongo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * @author tairanliu
 */
@Component
public class CorgiActivityDao {
    @Autowired
    MongoTemplate mongoTemplate;

    public CorgiActivity addActivity(CorgiActivity corgiActivity) {
        return mongoTemplate.insert(new ActivityMongo(corgiActivity));
    }
}
