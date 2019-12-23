package com.corgi.entity;

import com.corgi.activity.entity.CorgiActivity;
import lombok.Data;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;


/**
 * @author tairanliu
 */
@Data
@Document(collection = "CorgiActivity")
@CompoundIndexes(
        {
                @CompoundIndex(name = "activity_location",def = "{'location':'2dsphere'}"),
                @CompoundIndex(name = "activity_creator", def = "{'userId':1}")
        })
public class ActivityMongo extends CorgiActivity {
    @MongoId
    private String id;

    private GeoJsonPoint location;

    public ActivityMongo() {
        super();
    }

    public ActivityMongo(CorgiActivity activity) {

        this.setActivityType(activity.getActivityType());
        this.setAddress(activity.getAddress());
        this.setBudget(activity.getBudget());
        this.setContent(activity.getContent());
        this.setSignUpTime(activity.getSignUpTime());
        this.setLat(activity.getLat());
        this.setLng(activity.getLng());
        this.setPayType(activity.getPayType());
        this.setPeopleCount(activity.getPeopleCount());
        this.setTitle(activity.getTitle());
        this.setPics(activity.getPics());
        this.location = new GeoJsonPoint(activity.getLng(), activity.getLat());
    }

    public ActivityMongo initId(){
        super.setId(id);
        return this;
    }

}
