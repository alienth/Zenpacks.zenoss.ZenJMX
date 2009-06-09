###########################################################################
#
# This program is part of Zenoss Core, an open source monitoring platform.
# Copyright (C) 2008, Zenoss Inc.
#
# This program is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 as published by
# the Free Software Foundation.
#
# For complete information please visit: http://www.zenoss.com/oss/
#
###########################################################################
import md5

import Globals

from Products.ZenHub.services.PerformanceConfig import PerformanceConfig

from Products.ZenUtils.ZenTales import talesEval


from twisted.spread import pb
class RRDConfig(pb.Copyable, pb.RemoteCopy):
    """
    RRD configuration for a datapoint.
    Contains the create command and the min and max 
    values for a datapoint
    """
    def __init__(self, dp):
        self.dpName = dp.name()
        self.command = dp.createCmd
        self.dataPointId = dp.id
        self.min = dp.rrdmin
        self.max = dp.rrdmax
        self.rrdType = dp.rrdtype

pb.setUnjellyableForClass(RRDConfig, RRDConfig)
    

class JMXDeviceConfig(pb.Copyable, pb.RemoteCopy):
    """
    Represents the configuration for a device.
    Contains a list of JMXDataSourceConfig objects
    """
    
    def __init__(self, device):
        self.deviceId = device.id
        #Each JMXDataSourceConfig in the list represents a zenjmx datasource
        #from a template on a device.
        #map of jmxserverkey to JMXDataSourceConfig list
        self.jmxDataSourceConfigs = {}
        self.path = device.rrdPath()
        
        
    def findDataSource(self, dataSourceId):
        dsList = []
        for subList in self.jmxDataSourceConfigs.values():
            for dsConfig in subList:
                if(dsConfig.datasourceId == dataSourceId):
                    return dsConfig
        return None
        
    def add(self, jmxDataSourceConfig):
        """
        add a JMXDataSourceConfig to the device configuration
        """
        key = jmxDataSourceConfig.getJmxServerKey()
        configs = self.jmxDataSourceConfigs.get(key)
        if(not configs):
            configs = []
            self.jmxDataSourceConfigs[key] = configs
        configs.append(jmxDataSourceConfig)
        

pb.setUnjellyableForClass(JMXDeviceConfig, JMXDeviceConfig)

class JMXDataSourceConfig(pb.Copyable, pb.RemoteCopy):
    """
    Represents a JMX datasource configuration on a device. 
    """

    def __init__(self, device, template, datasource):
        self.device = device.id
        self.manageIp = device.manageIp
        self.datasourceId = datasource.id
        self.copyProperties(device, datasource)
        #dictionary of datapoint name to RRDConfig
        self.rrdConfig = {}
        for dp in datasource.datapoints():
            self.rrdConfig[dp.id] = RRDConfig(dp)
        self.thresholds = []
        for thresh in template.thresholds():
            self.thresholds.append(thresh.createThresholdInstance(device))


    def copyProperties(self, device, ds):
        """
        copy the properties from the datasouce and set them
        as attributes
        """
        for propName in [prop['id'] for prop in ds._properties]:
            value = getattr(ds, propName)
            if str(value).find('$') >= 0:
                value = talesEval('string:%s' % (value,), device)
            setattr(self, propName, value)


    def key(self):
        return self.device, self.datasourceId

    def getJmxServerKey(self):
        """
        string which represents the jmx server  and connection props. 
        Can be compared to determine if datasources configurations point to the
        same jmx server
        """
        return self.device + self.manageIp + self.getConnectionPropsKey()
               
    def getConnectionPropsKey(self):
        """
        string key which represents the connection properties that make up 
        the connection properties for the datasource.  
        """
        # raw service URL is being used
        if self.jmxRawService:
            return self.jmxRawService

        components = [self.jmxProtocol]
        if self.jmxProtocol == "RMI":
            components.append(self.rmiContext)
        components.append(self.jmxPort)
        if (self.authenticate):
            creds = self.username + self.password
            components.append( md5.new(creds).hexdigest() );
        
            
        return ":".join(components)
    
    def update(self, value):
        self.__dict__.update(value.__dict__)

pb.setUnjellyableForClass(JMXDataSourceConfig, JMXDataSourceConfig)


class ZenJMXConfigService(PerformanceConfig):
    """ZenHub service for getting ZenJMX configurations 
       from the object database"""

    def getDeviceConfig(self, device):
        """
        override method from PerformanceConfig
        Returns a JMXDeviceConfig object if the device has
        templates that have ZenJMX Datasource.
        returns None if the device does not have any JMX datsources 
        """
        deviceConfig = None
        if not device.monitorDevice():
            return None
        
        for template in device.getRRDTemplates():
            for ds in template.getRRDDataSources('JMX'):
                if ds.enabled:
                    if not deviceConfig:
                        deviceConfig = JMXDeviceConfig(device)
                    deviceConfig.add(
                         JMXDataSourceConfig(device, template, ds))
        return deviceConfig


    def sendDeviceConfig(self, listener, config):
        """
        override method from PerformanceConfig.
        Sends a JMXDeviceConfig for a device down to the daemon.
        """
        return listener.callRemote('updateDeviceConfig', config)


    def remote_getDeviceConfigs(self, devices=None):
        """
        returns a list JMXDeviceConfig bound to the monitor
        devices is a list of the device ids
        If devices is None all jmx device confings bound to the monitor will be
        returned, otherwise it will only return jmx device configs in the devices list
        """
        result = []
        #loop over the device bound to the monitor
        for device in self.config.devices():
            if not devices or device.id in devices:
                config = self.getDeviceConfig(device.primaryAq())
                if config:
                    result.append(config)
        return result

#
#    def remote_getStatus(self):
#        """Return devices with Mail problems."""
#        where = "eventClass = '%s'" % (Status_Mail)
#        issues = self.zem.getDeviceIssues(where=where, severity=3)
#        return [d
#                for d, count, total in issues
#                if getattr(self.config.devices, d, None)]

#if __name__ == '__main__':
#    from Products.ZenUtils.ZCmdBase import ZCmdBase
#    dmd = ZCmdBase().dmd
#    c = ConfigService(dmd, 'localhost')
#    print c.remote_getStatus()
#    print c.remote_getConfig()
