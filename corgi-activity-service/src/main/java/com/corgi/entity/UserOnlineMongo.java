package com.corgi.entity;

import com.corgi.user.entity.UserDetail;
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
@Document(collection = "UserOnline")
public class UserOnlineMongo extends UserDetail {

    @MongoId
    private ObjectId id;

    private GeoJsonPoint location;

    private Date createAt = new Date();

    public UserOnlineMongo() {
        super();
    }

    public UserOnlineMongo(UserDetail detail) {
        this.location = new GeoJsonPoint(detail.getLng(), detail.getLat());
    }

    public UserDetail getUserDetail() {
        UserDetail detail = new UserDetail();
        BeanUtils.copyProperties(this, detail);
        return detail;
    }

}
