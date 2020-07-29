package com.corgi.entity;

import com.corgi.activity.entity.CorgiActivity;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.lang.reflect.Method;


/**
 * @author tairanliu
 */
@Data
@Document(collection = "CorgiActivity")
@CompoundIndexes(
        {
                @CompoundIndex(name = "activity_location", def = "{'location':'2dsphere'}"),
                @CompoundIndex(name = "activity_creator", def = "{'userId':1}")
        })
public class ActivityMongo extends CorgiActivity {
    public static final String SIGN_UP_TIME = "signUpTime";

    @MongoId
    private ObjectId mongoId;

    private GeoJsonPoint location;

    public ActivityMongo() {
        super();
    }

    public ActivityMongo(CorgiActivity activity) {
        this.location = new GeoJsonPoint(activity.getLng(), activity.getLat());
    }

    public CorgiActivity getActivity() {
        CorgiActivity activity = new CorgiActivity();
        BeanUtils.copyProperties(this, activity);
        if (this.mongoId != null) {
            activity.setId(this.mongoId.toString());
        }
        String signUpTime = activity.getSignUpTime();
        if (signUpTime != null && signUpTime.length() > 10 && !signUpTime.contains(" ")) {
            signUpTime = signUpTime.substring(0, 10) + " " + signUpTime.substring(10);
            activity.setSignUpTime(signUpTime);
        }
        return activity;
    }

}
