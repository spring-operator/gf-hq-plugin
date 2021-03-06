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
package com.vmware.vfabric.hyperic.plugin.vfgf.detector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.jmx.MxQuery;
import org.hyperic.hq.product.jmx.MxServerDetector;
import org.hyperic.hq.product.jmx.MxUtil;
import org.hyperic.util.config.ConfigResponse;

import com.vmware.vfabric.hyperic.plugin.vfgf.GFProductPlugin;

public class GF7ServerDetector
    extends MxServerDetector {
    
    private static final Log log = LogFactory.getLog(GF7ServerDetector.class);
    
    private static String PROP_LOCATORS = "locators";
    private static String PROP_MEMBER = "member";
    private static String PROP_PORT = "port";
    private static String PROP_JMXURL = "jmx.url";
    private static String PROP_REGIONNAME = "name";
    private static String ERR_LOCATORS_NOT_DEFINED = 
        "No Locators defined. Please configure in the platform's Configuration Properties";  // Used often
    
    @Override    
    public List<ServerResource> getServerResources(ConfigResponse platformConfig) throws PluginException {
        log.debug("[getServerRsources] platformConfig = " + platformConfig);
        List<ServerResource> servers = new ArrayList<ServerResource>();
        String locators = platformConfig.getValue(PROP_LOCATORS);

        if (locators == null || locators.isEmpty()) {
            log.debug("[getServerResources] " + ERR_LOCATORS_NOT_DEFINED);
            return servers;
        }
        
        String url = GFProductPlugin.getJmxUrl(locators);
        if (url.isEmpty()) {
            return servers;
        }
        platformConfig.setValue("jmx.url", url);
        log.debug("[getServerResources] platformConfig=" + platformConfig);
        log.debug("[getServerResources] url="+url);
        
        JMXConnector connector = null;
        MBeanServerConnection mServer;
    
        try {
            connector = MxUtil.getMBeanConnector(platformConfig.toProperties());
            mServer = connector.getMBeanServerConnection();
        } catch (IOException e) {
            GFProductPlugin.resetJmxUrl();
            throw new PluginException(e.getMessage(), e);
        } catch (Exception e) {
            MxUtil.close(connector);
            throw new PluginException(e.getMessage(), e);
        }

        String objName = getTypeProperty(MxQuery.PROP_OBJECT_NAME);
        log.debug("[getServerResources] objName="+objName);

        Set<ObjectName> names = new HashSet<ObjectName>();
        try {
            names = mServer.queryNames(new ObjectName(objName), null);
        } catch (MalformedObjectNameException e) {
            log.debug("[getServerResources] " + e.getMessage(), e);    
        } catch (IOException e) {
            log.debug("[getServerResources] " + e.getMessage(), e);
        }
        getLog().debug("[getServerResources] discovered " + names.size() + " servers");

        for (Iterator<ObjectName> it = names.iterator(); it.hasNext();) {
            ObjectName name = (ObjectName)it.next();
            ServerResource server = createServerResource(name.toString());
            
            ConfigResponse config = new ConfigResponse();
            ConfigResponse cprops = new ConfigResponse();
            ConfigResponse controlConfig = new ConfigResponse();
            
            String port = name.getKeyProperty(PROP_PORT);
            log.debug("[getServerResources] port=" + port);
            
            // Locators don't have port defined.
            if (port != null) {
                config.setValue(PROP_PORT, port);
            }
            
            String regionName = name.getKeyProperty(PROP_REGIONNAME);
            if (regionName != null) {
                config.setValue(PROP_REGIONNAME, regionName);
            }

            config.setValue(PROP_LOCATORS, locators);
            // For regions
            String memberName = name.getKeyProperty(PROP_MEMBER);
            if (memberName == null && regionName != null) {
                memberName = regionName;
                config.setValue(PROP_NAME, regionName);
            } else {
                // CacheServer and Locators
                config.setValue(PROP_MEMBER, memberName);
            }
            
            server.setName(getPlatformName() + " " + getTypeInfo().getName() + " " + memberName);
            server.setControlConfig(controlConfig);
            setProductConfig(server, config);
            setMeasurementConfig(server, new ConfigResponse());
            server.setCustomProperties(cprops);

            servers.add(server);
        }
        return servers;
    }
    
    @Override
    protected List discoverServices(ConfigResponse serverConfig)
        throws PluginException {
        String locators = serverConfig.getValue(PROP_LOCATORS);
        
        if (locators == null || locators.isEmpty()) {
            throw new PluginException(ERR_LOCATORS_NOT_DEFINED);
        }
        
        String url = GFProductPlugin.getJmxUrl(locators);
        if (url.isEmpty()) {
            throw new PluginException("Unable to determine jmx url from locators");
        }
        serverConfig.setValue(PROP_JMXURL, url);
        return super.discoverServices(serverConfig);
    }
}
