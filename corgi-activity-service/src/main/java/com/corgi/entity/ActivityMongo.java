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
    @MongoId
    private ObjectId mongoId;

    private GeoJsonPoint location;

    public ActivityMongo() {
        super();
    }

    public ActivityMongo(CorgiActivity activity) {

//        this.setActivityType(activity.getActivityType());
//        this.setAddress(activity.getAddress());
//        this.setBudget(activity.getBudget());
//        this.setContent(activity.getContent());
//        this.setSignUpTime(activity.getSignUpTime());
//        this.setLat(activity.getLat());
//        this.setLng(activity.getLng());
//        this.setPayType(activity.getPayType());
//        this.setPeopleCount(activity.getPeopleCount());
//        this.setTitle(activity.getTitle());
//        this.setPics(activity.getPics());
//        this.setCreateTime(activity.getCreateTime());
        this.location = new GeoJsonPoint(activity.getLng(), activity.getLat());
    }

    public CorgiActivity getActivity() {
        CorgiActivity activity = new CorgiActivity();
        BeanUtils.copyProperties(this, activity);
        if (this.mongoId != null) {
            activity.setId(this.mongoId.toString());
        }
        return activity;
    }

}
