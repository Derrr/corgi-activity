package com.corgi.entity;

import com.corgi.user.entity.UserDetail;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;


/**
 * @author tairanliu
 */
@Data
@Document(collection = "UserDetail")
@NoArgsConstructor
public class UserDetailMongo extends UserMongoBase {

    @MongoId
    private ObjectId id;

    private GeoJsonPoint location;

    private Long time;

    public UserDetailMongo(UserDetail detail) {
        this.location = new GeoJsonPoint(detail.getLng(), detail.getLat());
        this.time = System.currentTimeMillis();
    }

}
