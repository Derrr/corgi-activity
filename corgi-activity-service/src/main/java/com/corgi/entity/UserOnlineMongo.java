package com.corgi.entity;

import com.corgi.user.entity.UserDetail;
import com.corgi.user.entity.UserExtra;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.Date;


/**
 * @author tairanliu
 */
@Data
@Document(collection = "UserOnline")
@NoArgsConstructor
public class UserOnlineMongo extends UserMongoBase {

    @MongoId
    private ObjectId id;

    private GeoJsonPoint location;

    public UserOnlineMongo(UserDetail detail) {
        this.location = new GeoJsonPoint(detail.getLng(), detail.getLat());
    }

}
