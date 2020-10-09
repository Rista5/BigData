package com.spark.comparators;

import java.io.Serializable;
import java.util.Comparator;
import com.spark.TrafficAccidentData;

public class StartTimeComparator implements Comparator<TrafficAccidentData>, Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 819490992751238010L;

    @Override
    public int compare(TrafficAccidentData o1, TrafficAccidentData o2) {
        return o1.getStartTime().compareTo(o2.getStartTime());
    }
    
}
