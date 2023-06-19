package com.corgi.entity;

import com.corgi.user.entity.UserDetail;
import com.corgi.user.entity.UserExtra;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.Date;


/**
 * @author tairanliu
 */
@Data
public class UserMongoBase extends UserDetail {

    private UserExtra userExtra;

    private Date createAt = new Date();

    public UserMongoBase() {
        super();
    }

    public UserDetail getUserDetail() {
        UserDetail detail = new UserDetail();
        BeanUtils.copyProperties(this, detail);
        return detail;
    }

}
