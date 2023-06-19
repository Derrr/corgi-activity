package com.corgi.entity;

import com.corgi.user.entity.UserDetail;
import com.corgi.user.entity.UserExtra;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


/**
 * @author tairanliu
 */
@Data
public class UserMongoBase extends UserDetail {


    private String aim;
    private String profession;
    private String education;
    private String xp;
    private String income;
    private List<String> interestList = new ArrayList<>();
    private List<String> tagList = new ArrayList<>();


    private Date createAt = new Date();

    public UserMongoBase() {
        super();
    }

    public void setUserExtra(UserExtra userExtra) {
        BeanUtils.copyProperties(this, userExtra);
        if (StringUtils.isEmpty(userExtra.getInterests()) && !"[]".equals(userExtra.getInterests())) {
            this.interestList = Arrays.asList(userExtra.getInterests().replaceAll("[\\]\\[\"'\\s]", "").split(","));
        }
        if (StringUtils.isEmpty(userExtra.getTags()) && !"[]".equals(userExtra.getTags())) {
            this.tagList = Arrays.asList(userExtra.getTags().replaceAll("[\\]\\[\"'\\s]", "").split(","));
        }
    }

    public UserDetail getUserDetail() {
        UserDetail detail = new UserDetail();
        BeanUtils.copyProperties(this, detail);
        return detail;
    }

}
