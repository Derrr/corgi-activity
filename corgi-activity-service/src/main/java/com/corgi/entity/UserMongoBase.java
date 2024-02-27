package com.corgi.entity;

import com.corgi.user.entity.UserDetail;
import com.corgi.user.entity.UserExtra;
import com.corgi.user.entity.UserQuery;
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
    private String income;
    private List<String> xpList;
    private List<String> interestList = new ArrayList<>();
    private List<String> tagList = new ArrayList<>();
    private UserQuery query;

    private Date createAt = new Date();

    public UserMongoBase() {
        super();
    }

    public void setUserExtra(UserExtra userExtra) {
        this.income = userExtra.getIncome();
        this.profession = userExtra.getProfession();
        this.aim = userExtra.getAim();
        this.education = userExtra.getEducation();
        if (!StringUtils.isEmpty(userExtra.getInterests())) {
            this.interestList = Arrays.asList(userExtra.getInterests().split(","));
        }
        if (!StringUtils.isEmpty(userExtra.getTags())) {
            this.tagList = Arrays.asList(userExtra.getTags().split(","));
        }
        if (!StringUtils.isEmpty(userExtra.getXp())) {
            this.xpList = Arrays.asList(userExtra.getXp().split(","));
        }

    }

    public UserDetail getUserDetail() {
        UserDetail detail = new UserDetail();
        BeanUtils.copyProperties(this, detail);
        return detail;
    }

}
