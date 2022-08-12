package com.corgi.entity;

import com.corgi.user.entity.UserDetail;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;


/**
 * @author tairanliu
 */
@Data
@Document(collection = "MatchView")
public class MatchViewMongo {

    @MongoId
    private ObjectId id;

    private String userId;

    private String matchId;

    private Long time;

    public MatchViewMongo(String userId, String matchId) {
        this.userId = userId;
        this.matchId = matchId;
        this.time = System.currentTimeMillis();
    }

}
