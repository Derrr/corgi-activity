package com.corgi.entity;

import com.corgi.activity.entity.CorgiActivity;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;


/**
 * @author tairanliu
 */
@Data
@Document(collection = "CorgiActivity")
public class ActivityMongo extends CorgiActivity {
    @Id
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
        this.setEnlistTime(activity.getEnlistTime());
        this.setLat(activity.getLat());
        this.setLng(activity.getLng());
        this.setPayType(activity.getPayType());
        this.setPeopleCount(activity.getPeopleCount());
        this.setTitle(activity.getTitle());
        this.setPics(activity.getPics());
        this.location = new GeoJsonPoint(activity.getLng(), activity.getLat());
    }

    @Override
    public String getId() {
        return super.getId();
    }

    @Override
    public void setId(String id) {
        super.setId(id);
    }


}
