/**
 * Copyright (C) [2010-2015], Pivotal Software, Inc.
 *
 *  This is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package com.vmware.vfabric.hyperic.plugin.vfgf.cache;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.MapMaker;

public class MetricCache {

    /** Last time when cache was updated. */
    private long metricCacheLastUpdate;

    /** Cache for all metric values. */
    private ConcurrentMap<String, Double> metricCache;

    /** Cache for tracked metrics. */
    private ConcurrentMap<String, Double> trackCache;

    /** Flag telling if member is visible. */
    private boolean memberOnline = false;
    
    /** Track cache lock*/
    private Object trackLock = new Object();

    public MetricCache() {
        metricCacheLastUpdate = 0;

        metricCache = new MapMaker()
        .makeMap();

        // Let unneeded metrics to stay under collection for 1 hour
        trackCache = new MapMaker()
        .expireAfterAccess(3600, TimeUnit.SECONDS)
        .makeMap();		

    }

    public long getMetricCacheLastUpdate() {
        return metricCacheLastUpdate;
    }

    public void setMetricCacheLastUpdate(long metricCacheLastUpdate) {
        this.metricCacheLastUpdate = metricCacheLastUpdate;
    }

    public boolean isMemberOnline() {
        return memberOnline;
    }

    public void setMemberOnline(boolean memberOnline) {
        this.memberOnline = memberOnline;
    }

    public ConcurrentMap<String, Double> getMetricCache() {
        return metricCache;
    }

    public String[] getTrackKeySet() {
        synchronized (trackLock) {
            // XXX: for some reason there has been NoSuchElementException
            //      coming out from guava libs when toArray is executed.
            //      can't reproduce, so is case of that, return empty map
            //      which then falls back to full item retrieval.
            //      (remove this if possible bug can be re-produced)
            try {
                return trackCache.keySet().toArray(new String[0]);
            } catch (Exception e) {
            }
            return new String[0];
        }
    }
    
    public void putToTrackCache(String alias, Double value) {
        synchronized (trackLock) {
            trackCache.put(alias, value);            
        }
    }

}
