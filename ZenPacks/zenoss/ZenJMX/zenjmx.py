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
__doc__ = '''zenjmx

monitor jmx mbeans

dispatches calls to a java server process to collect jmx values for a device.

'''

import os
import socket
import time
import Globals
import sys
import logging

from twisted.internet.defer import Deferred
from twisted.web import xmlrpc
from twisted.python import failure
from twisted.internet.protocol import ProcessProtocol
from twisted.python.failure import Failure
from twisted.internet import defer, reactor

from Products.ZenEvents import Event
from Products.ZenHub.XmlRpcService import XmlRpcService
from Products.ZenModel.RRDDataPoint import SEPARATOR
from Products.ZenRRD.RRDDaemon import RRDDaemon
from Products.ZenRRD.RRDUtil import RRDUtil
from Products.ZenRRD.Thresholds import Thresholds
from Products.ZenUtils.NJobs import NJobs
from Products.ZenUtils.Utils import binPath
from Products.ZenUtils.Driver import drive, driveLater
import ZenPacks.zenoss.ZenJMX


from ZenPacks.zenoss.ZenJMX.services.ZenJMXConfigService import JMXDataSourceConfig


import time

DEFAULT_HEARTBEAT_TIME = 5*60

WARNING_EVENT = dict(eventClass="/Status/JMX",
                     component="JMX",
                     device=socket.getfqdn(),
                     severity=Event.Warning)

class ZenJMX(RRDDaemon):
    initialServices = RRDDaemon.initialServices + [
        'ZenPacks.zenoss.ZenJMX.services.ZenJMXConfigService'
        ]
    def __init__(self):
        RRDDaemon.__init__(self, 'zenjmx')
        #map of deviceId -> JMXDeviceConfig
        self.deviceConfigs = {}
        self.running = False
        #map of server connection keys -> boolean
        #previous connection state to device's mbean server
        #if no entry then previous state is unknown (usually at startup)
        self.jmxConnUp = {}
        self.cycleSeconds = 300

    def connected(self):
        def configTask(driver):
            self.log.debug("configTask(): fetching config")
            yield self.fetchConfig()
            driver.next()
            driveLater(self.configCycleInterval, configTask)
        
        def startZenjmx(result):
            self.log.debug("startZenjmx(): %s" % result)
            drive(configTask).addCallbacks(self.runCollection, self.errorStop)
        
        self.log.debug("connected(): zenjmxjavaport is %s" % \
                       self.options.zenjmxjavaport)
        args = None
        if self.options.configfile:
            args = ("--configfile", self.options.configfile)
        if self.options.zenjmxjavaport:
            args = args + ("-zenjmxjavaport", str(self.options.zenjmxjavaport))
        if self.options.logseverity:
            args = args + ("-v", str(self.options.logseverity))
        self.log.debug("connected(): cycletime is %s" % self.options.cycletime)
        self.cycleSeconds = self.options.cycletime * 60
        self.heartbeatTimeout = self.cycleSeconds* 3
        self.javaProcess = ZenJmxJavaClient(args)
        running = self.javaProcess.run()
        self.log.debug("connected(): launched process, waiting on callback")
        running.addCallback(startZenjmx)
        running.addErrback(self.errorStop)
        
    def remote_deleteDevice(self, deviceId):
        self.log.debug("Async delete device %s" % deviceId)
        self.deviceConfigs.pop(deviceId, None)


    def remote_updateDeviceConfig(self, config):
        self.log.debug("Async device update")
        self.updateConfig(config)


    def updateConfig(self, jmxDeviceConfig):
        """
        update device configurations to be collected
        jmxDeviceConfig should be a JMXDeviceConfig
        """
        key = jmxDeviceConfig.deviceId
        self.log.debug("updateConfig(): updating config for device %s" % key)
        for dataSources in jmxDeviceConfig.jmxDataSourceConfigs.values():
            for dataSource in dataSources:
                self.log.debug("updateConfig(): datasource %s device %s" 
                               % (dataSource.datasourceId, key))
                self.thresholds.updateList(dataSource.thresholds)
        
        self.deviceConfigs[key] = jmxDeviceConfig
        

    def fetchConfig(self):
        """
        Get configuration values from ZenHub
        """
        def inner(driver):
            self.log.debug("fetchConfig(): Fetching config from zenhub")
            yield self.model().callRemote('getDefaultRRDCreateCommand')
            createCommand = driver.next()
     
            yield self.model().callRemote('propertyItems')
            self.setPropertyItems(driver.next())
            self.rrd = RRDUtil(createCommand, self.cycleSeconds)
     
            yield self.model().callRemote('getThresholdClasses')
            self.remote_updateThresholdClasses(driver.next())
     
            yield self.model().callRemote('getCollectorThresholds')
            self.rrdStats.config(self.options.monitor,
                                  self.name,
                                  driver.next(),
                                  createCommand)
            devices = []
            if self.options.device:
                devices = [self.options.device]
            yield self.model().callRemote('getDeviceConfigs', devices)
            configs = driver.next()
            if len(configs) == 0:
                self.log.info("fetchConfig(): No configs returned from zenhub")
            else:
                for jmxDeviceConfig in configs:
                    self.updateConfig(jmxDeviceConfig)
            self.log.debug("fetchConfig(): Done Fetching config from zenhub")
        return drive(inner)
    
    
    def collectJmx(self, dsConfigList):
        """
        call java jmx process to collect jmx values
        dsConfigList: list of JMXDataSourceConfig
        """
        def toDict( config ):
            '''marshall the fields from the datasource into a dictionary and
            ignore everything that is not a primitive'''
            vals = {}

            for key, val in config.__dict__.items():
                if key != "rrdConfig" and type(val) in XmlRpcService.PRIMITIVES:
                    vals[key] = val

            vals['dps']=[]
            vals['dptypes']=[]
            for rrdConfig in config.rrdConfig.values():
                vals['dps'].append(rrdConfig.dataPointId)
                vals['dptypes'].append(rrdConfig.rrdType)

            return vals

        def rpcCall( driver ):
            port = self.options.zenjmxjavaport
            xmlRpcProxy = xmlrpc.Proxy("http://localhost:%s/" % port)
            yield xmlRpcProxy.callRemote("zenjmx.collect", configMaps)
            results = driver.next()
            self.log.debug("rpcCall(): result is %s" % results)
            processResults(results)
            
        def processResults(jmxResults):
            result = {}
            for result in jmxResults:
                evtSummary = result.get("summary")
                deviceId = result.get("device")
                if not evtSummary:
                    dsId = result.get("datasourceId")
                    dpId = result.get("dpId")
                    value = result.get("value")
                    self.storeRRD(deviceId, dsId, dpId, value)
                    if not self.jmxConnUp.get(mbeanServerKey, False):
                        self.sendEvent({},
                                       severity = Event.Clear,
                                       component = connectionComponentKey,
                                       eventClass = "/Status/JMX/Connection",
                                       summary = "Connection is up",
                                       device = deviceId)
                    self.jmxConnUp[mbeanServerKey] = True
                else:
                    #send event
                    self.log.error("processResults(): "+\
                                   "jmx error, sending event for %s" 
                                   % result)
                    #default component to use
                    evt = self.createEvent(result, connectionComponentKey)
                    self.sendEvent(evt, severity=Event.Warning)
                    self.jmxConnUp[mbeanServerKey] = False
            
        mbeanServerKey = ""
        connectionComponentKey = ""
        configMaps = []
        for config in dsConfigList:
            connectionComponentKey = config.getConnectionPropsKey()
            mbeanServerKey = config.getJmxServerKey()
            configMaps.append(toDict(config))
        self.log.info("collectJmx(): for %s" % connectionComponentKey)
        return drive(rpcCall)
    
    def createEvent(self, errorMap, component=None):
        event = errorMap.copy();
        if(component):
            event["component"] = component
        event.update(errorMap)
        return event
    
    def storeRRD(self, deviceId, dataSourceId, dataPointId, dpValue):
        """
        store a value into an RRD file
        """
        deviceConfig = self.deviceConfigs.get(deviceId)
        if not deviceConfig:
            self.log.info("storeRRD(): " + \
                          "no configuration for device %s found" 
                          % deviceId)
            return
        dsConfig = deviceConfig.findDataSource(dataSourceId)
        if not dsConfig:
            self.log.info("storeRRD(): "+\
                          "no data source config found for device %s "+\
                          "datasource %s" % deviceId, dataSourceId)
            return
        rrdConf = dsConfig.rrdConfig.get(dataPointId)
        if not dsConfig:
            self.log.info("storeRRD(): "+\
                          "no rrd config found for device %s "+\
                          "datasource %s datapoint %s" % \
                          deviceId, dataSourceId, dataPointId)
            return
        
        devicePath = deviceConfig.path
        dpPath = "/".join((devicePath, rrdConf.dpName))
        value = self.rrd.save(dpPath,
                              dpValue,
                              rrdConf.rrdType,
                              rrdConf.command)

        for ev in self.thresholds.check(dpPath, time.time(), value):
            eventKey = dsConfig.eventKey
            if ev.has_key('eventKey'):
                ev['eventKey'] = '%s|%s' % (eventKey, ev['eventKey'])
            else:
                ev['eventKey'] = eventKey
            ev['component'] = dsConfig.component
            self.sendThresholdEvent(**ev)
    
    
    
    def runCollection(self, result = None):
        
        def doCollection(driver):
            self.heartbeat()
            #Schedule for later
            self.log.debug("doCollection(): starting collection cycle")
            reactor.callLater(self.cycleSeconds, self.runCollection)
            if not self.options.cycle:
                self.stop()
            if self.running:
                self.log.error("last zenjmx collection is still running")
                return
            self.running = True
            #defensive copy in case the config gets updated. Is this possible?
            configCopy = self.deviceConfigs.values()[:]
            dataSourceConfigs = []
            for config in configCopy:
                deviceId = config.deviceId
                self.log.info("doCollection(): running collection for %s" 
                              % deviceId)
                for dsConfigList in config.jmxDataSourceConfigs.values():
                    dataSourceConfigs.append(dsConfigList)
                
            jobs = NJobs(200,
                         self.collectJmx,
                         dataSourceConfigs)
            yield jobs.start()
            driver.next()
            self.log.debug("doCollection(): exiting collection cycle")

        def handleFinish(results):
            self.running = False
            for result in results:
                if isinstance(result,failure.Failure):
                    self.log.error("handleFinish():Failure: %s"
                                   % result)
                    result.printDetailedTraceback()
                elif isinstance(result , Exception):
                    self.log.error("handleFinish():Exception: %s"
                                   % result)
                else:
                    self.log.debug("handleFinish(): success %s"
                                  % result)
                    
        def handleError(error):
            self.running = False
            self.log.error("handleError():Error running doCollection: %s"
                           % error.printTraceback())
        d = drive(doCollection)
        d.addCallback(handleFinish)
        d.addErrback(handleError)
        return d
        
        
    def buildOptions(self):
        RRDDaemon.buildOptions(self)
        self.parser.add_option('-j', '--zenjmxjavaport',
                               dest='zenjmxjavaport',
                               default=9988,
                               type='int',
                               help="Port for zenjmxjava process")
        self.parser.add_option('--cycletime',
                               dest='cycletime',
                               default=5,
                               type='int',
                               help="Cycle time, in minutes, to run collection")
        
    def stop(self):
        if self.javaProcess:
            self.javaProcess.stop()
            self.javaProcess = None
        RRDDaemon.stop(self)

class ZenJmxJavaClient(ProcessProtocol):
    """"
    protocol to control the zenjmxjava process
    """

    def __init__(self, args):
        self.deferred = Deferred()
        self.stopCalled = False
        self.process = None
        self.outReceived = sys.stdout.write
        self.errReceived = sys.stderr.write
        self.log = logging.getLogger("zen.ZenJmxJavaClient")
        self.args = args
        

    def processEnded(self, reason):
        self.log.debug("processEnded():zenjmxjava process ended %s" % reason)
        self.process = None
        if not self.stopCalled:
            if(self.deferred):
                self.deferred.errback(reason)
            self.deferred = None
            self.log.info("processEnded():restarting zenjmxjava")
            reactor.callLater(1, self.run)
            

    def stop(self):
        import signal
        self.log.info("stop():stopping zenjmxjava")
        self.stopCalled = True
        if not self.process:
            self.log.debug("stop():no zenjmxjava process to stop")
            return
        try:
            self.process.signalProcess(signal.SIGKILL)
        except error.ProcessExitedAlready:
            self.log.info("stop():zenjmxjava process already exited")
            pass
        try:
            self.process.loseConnection()
        except Exception:
            pass
        self.process = None
        

    def connectionMade(self):
        self.log.debug("connectionMade():")
        def doCallback():
            msg = "doCallback(): callback on deferred zenjmxjava proc is up"
            self.log.debug(msg)
            if(self.deferred):
                self.deferred.callback("zenjmx java started")
            self.deferred = None
        if self.deferred:
            #give the java service a chance to startup
            reactor.callLater(2, doCallback)
        self.log.debug("connectionMade(): done")
            

    def run(self):
        if(self.stopCalled):
            return
        self.log.info("run():starting zenjmxjava")
        zenjmxjavacmd = os.path.join(
                        ZenPacks.zenoss.ZenJMX.binDir, 'zenjmxjava')
        args = ("runjmxenabled",)
        if(self.args):
            args = args + self.args
        cmd =(zenjmxjavacmd,)+args
        self.log.debug("run():spawn process %s" % (cmd,))
        self.deferred = Deferred()
        self.process = reactor.spawnProcess(self, zenjmxjavacmd, cmd, env=None)
        return self.deferred


if __name__ == '__main__':
    zjmx = ZenJMX()
    zjmx.run()
