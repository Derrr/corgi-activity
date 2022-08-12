package com.corgi.entity;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;


/**
 * @author tairanliu
 */
@Data
@Document(collection = "UserMatch")
public class UserMatchMongo {

    @MongoId
    private ObjectId id;

    private String userId;

    private String matchId;

    private Long time;

    public UserMatchMongo(String userId, String matchId) {
        this.userId = userId;
        this.matchId = matchId;
        this.time = System.currentTimeMillis();
    }

}
