package com.corgi.activity.api;

import com.corgi.activity.entity.CorgiActivity;

import java.util.List;

/**
 * @author tairanliu
 */
public interface CorgiActivityService {
    CorgiActivity addCorgiActivity(CorgiActivity corgiActivity);

    List<CorgiActivity> getCorgiActivityByRange(double lng,double lat,double range);
}
