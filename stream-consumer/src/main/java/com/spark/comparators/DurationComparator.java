package com.spark.comparators;

import java.io.Serializable;
import java.util.Comparator;
import com.spark.TrafficAccidentData;

public class DurationComparator implements Comparator<TrafficAccidentData>, Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 6864561826885961522L;

    @Override
    public int compare(TrafficAccidentData o1, TrafficAccidentData o2) {
        return Long.compare(o1.getDuration(), o2.getDuration());
    }
    
}
