#! /usr/bin/env python
# -*- coding: utf-8 -*-
# ##########################################################################
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
# ##########################################################################

__doc__ = """Monitor Java Management eXtension (JMX) mbeans

Dispatches calls to a java server process to collect JMX values for a device.
"""

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

DEFAULT_HEARTBEAT_TIME = 5 * 60

WARNING_EVENT = dict(eventClass='/Status/JMX', component='JMX',
                     device=socket.getfqdn(), severity=Event.Warning)


class ZenJMX(RRDDaemon):
    """
    Java Management eXtentions (JMX) class that connects to zenhub, and links
    to another class which calls a Java app that actually gathers JMX data.
    """

    initialServices = RRDDaemon.initialServices\
         + ['ZenPacks.zenoss.ZenJMX.services.ZenJMXConfigService']

    def __init__(self, noopts=False):
        RRDDaemon.__init__(self, 'zenjmx', noopts)

        # map of deviceId -> JMXDeviceConfig
        self.deviceConfigs = {}
        self.running = False

        # map of server connection keys -> boolean
        # previous connection state to device's mbean server
        # if no entry then previous state is unknown (usually at startup)
        self.jmxConnUp = {}
        self.cycleSeconds = 300


    def connected(self):
        """
        Twisted routine called when connected to zenhub, allowing
        us to begin initialization.
        """
        def configTask(driver):
            """
            Gather our configuration from zenhub.
            A generator function.
            
            @param driver: driver
            @type driver: string
            """

            startTime = time.time()
            self.log.debug('configTask(): fetching config')
            yield self.fetchConfig()
            driver.next()
            configTime = time.time() - startTime
            self.rrdStats.gauge('configTime', self.configCycleInterval
                                 * 60, configTime)
            self.log.debug('configTask(): daemon stats config time is %s'
                            % configTime)
            driveLater(self.configCycleInterval * 60, configTask)

        def startZenjmx(result):
            """
            Start up
            
            @param result: result
            @type result: string
            """
            self.log.info('startZenjmx(): %s' % result)
            drive(configTask).addCallbacks(self.runCollection,
                    self.errorStop)

        self.log.debug('connected(): zenjmxjavaport is %s'
                        % self.options.zenjmxjavaport)
        args = None
        if self.options.configfile:
            args = ('--configfile', self.options.configfile)
        if self.options.zenjmxjavaport:
            args = args + ('-zenjmxjavaport',
                           str(self.options.zenjmxjavaport))
        if self.options.logseverity:
            args = args + ('-v', str(self.options.logseverity))
        if self.options.concurrentJMXCalls:
            args = args + ('-concurrentJMXCalls', )
        self.log.debug('connected(): cycletime is %s'
                        % self.options.cycletime)
        self.cycleSeconds = self.options.cycletime
        self.heartbeatTimeout = self.cycleSeconds * 3
        self.javaProcess = ZenJmxJavaClient(args, self,
                self.options.cycle)
        running = self.javaProcess.run()
        self.log.debug('connected(): launched process, waiting on callback'
                       )
        running.addCallback(startZenjmx)
        running.addErrback(self.errorStop)


    def remote_deleteDevice(self, deviceId):
        """
        A deleteDevice function that can be called from zenhub
        
        @param deviceId: deviceId
        @type deviceId: string
        """
        self.log.debug('Async delete device %s' % deviceId)
        self.deviceConfigs.pop(deviceId, None)


    def remote_updateDeviceConfig(self, config):
        """
        An updateDeviceConfig function that can be called from zenhub
        
        @param config: config
        @type config: string
        """
        self.log.debug('Async device update')
        self.updateConfig(config)


    def updateConfig(self, jmxDeviceConfig):
        """
        update device configurations to be collected
        jmxDeviceConfig should be a JMXDeviceConfig
        
        @param jmxDeviceConfig: jmxDeviceConfig
        @type jmxDeviceConfig: string
        """
        key = jmxDeviceConfig.deviceId

        # if device option specified only deal with configs for that device
        if self.options.device and self.options.device != key:
            msg = \
                'device option enabled for %s; rejecting config for %s'
            self.log.info(msg % (self.options.device, key))
            return
        self.log.debug('updateConfig(): updating config for device %s'
                        % key)
        for dataSources in \
            jmxDeviceConfig.jmxDataSourceConfigs.values():
            for dataSource in dataSources:
                self.log.debug('updateConfig(): datasource %s device %s'
                                % (dataSource.datasourceId, key))
                self.thresholds.updateList(dataSource.thresholds)

        self.deviceConfigs[key] = jmxDeviceConfig


    def fetchConfig(self):
        """
        Get configuration values from ZenHub
        
        @return:
        @rtype:
        """
        def inner(driver):
            """
            Generator function to gather configs from zenhub
            
            @param driver: driver
            @type driver: string
            @return:
            @rtype:
            """
            self.log.debug('fetchConfig(): Fetching config from zenhub')
            yield self.model().callRemote('getDefaultRRDCreateCommand')
            createCommand = driver.next()

            yield self.model().callRemote('propertyItems')
            self.setPropertyItems(driver.next())
            self.rrd = RRDUtil(createCommand, self.cycleSeconds)

            yield self.model().callRemote('getThresholdClasses')
            self.remote_updateThresholdClasses(driver.next())

            yield self.model().callRemote('getCollectorThresholds')
            self.rrdStats.config(self.options.monitor, self.name,
                                 driver.next(), createCommand)
            devices = []
            if self.options.device:
                devices = [self.options.device]
            yield self.model().callRemote('getDeviceConfigs', devices)
            configs = driver.next()
            if len(configs) == 0:
                self.log.info('fetchConfig(): No configs returned from zenhub'
                              )
            else:
                for jmxDeviceConfig in configs:
                    self.updateConfig(jmxDeviceConfig)
            self.log.debug('fetchConfig(): Done Fetching config from zenhub'
                           )

        return drive(inner)


    def collectJmx(self, dsConfigList):
        """
        Call Java JMX process to collect JMX values
        
        @param dsConfigList: DataSource configuration
        @type dsConfigList: list of JMXDataSourceConfig
        @return: Twisted deferred object
        @rtype: Twisted deferred object
        """
        def toDict(config):
            """
            Marshall the fields from the datasource into a dictionary and
            ignore everything that is not a primitive
            
            @param config: dictionary of results
            @type config: string
            @return: results from remote device
            @rtype: dictionary
            """
            vals = {}
            for (key, val) in config.__dict__.items():
                if key != 'rrdConfig' and type(val)\
                     in XmlRpcService.PRIMITIVES:
                    vals[key] = val

            rrdConfigs = config.rrdConfig.values()
            rrdConfigs.sort(lambda x, y: cmp(x.dataPointId,
                            y.dataPointId))

            vals['dps'] = []
            vals['dptypes'] = []
            for rrdConfig in rrdConfigs:
                vals['dps'].append(rrdConfig.dataPointId)
                vals['dptypes'].append(rrdConfig.rrdType)

            return vals

        def rpcCall(driver):
            """
            Communicate with our local JMX process to collect results.
            This is a generator function
            
            @param driver: generator
            @type driver: string
            """

            port = self.options.zenjmxjavaport
            xmlRpcProxy = xmlrpc.Proxy('http://localhost:%s/' % port)
            yield xmlRpcProxy.callRemote('zenjmx.collect', configMaps)
            results = driver.next()
            self.log.debug('rpcCall(): result is %s' % results)
            processResults(results)

        def processResults(jmxResults):
            """
            Given the results from JMX, store them or send events.
            
            @param jmxResults: jmxResults
            @type jmxResults: string
            """
            result = {}
            for result in jmxResults:
                evtSummary = result.get('summary')
                deviceId = result.get('device')
                evt = self.createEvent(result)
                if not evtSummary:
                    dsId = result.get('datasourceId')
                    dpId = result.get('dpId')
                    value = result.get('value')
                    try:
                        self.storeRRD(deviceId, dsId, dpId, value)
                    except ValueError:
                        pass
                    if not self.jmxConnUp.get(mbeanServerKey, False):
                        self.sendEvent({}, severity=Event.Clear,
                                eventClass='/Status/JMX/Connection',
                                summary='Connection is up',
                                device=deviceId)
                    self.jmxConnUp[mbeanServerKey] = True
                    self.sendEvent(evt, severity=Event.Clear)

                else:
                    # send event
                    self.log.debug('processResults(): '
                                    + 'jmx error, sending event for %s'
                                    % result)

                    self.sendEvent(evt, severity=Event.Error)
                    if evt.get('eventClass')\
                         == '/Status/JMX/Connection':
                        self.jmxConnUp[mbeanServerKey] = False

        mbeanServerKey = ''
        connectionComponentKey = ''
        configMaps = []
        for config in dsConfigList:
            connectionComponentKey = config.getConnectionPropsKey()
            mbeanServerKey = config.getJmxServerKey()
            configMaps.append(toDict(config))
        self.log.info('collectJmx(): for %s %s' % (config.device,
                      connectionComponentKey))
        return drive(rpcCall)


    def createEvent(self, errorMap, component=None):
        """
        Given an event dictionary, copy it and return the event
        
        @param errorMap: errorMap
        @type errorMap: s dictionarytring
        @param component: component name
        @type component: string
        @return: updated event
        @rtype: dictionary
        """
        event = errorMap.copy()
        if component:
            event['component'] = component
        if event.get('datasourceId') and not event.get('eventKey'):
            event['eventKey'] = event.get('datasourceId')
        return event


    def storeRRD(
        self,
        deviceId,
        dataSourceId,
        dataPointId,
        dpValue,
        ):
        """
        Store a value into an RRD file
        
        @param deviceId: name of the remote device
        @type deviceId: string
        @param dataSourceId: name of the data source
        @type dataSourceId: string
        @param dataPointId: name of the data point
        @type dataPointId: string
        @param dpValue: dpValue
        @type dpValue: number
        """
        deviceConfig = self.deviceConfigs.get(deviceId)
        if not deviceConfig:
            self.log.info( 'No configuration for device %s found'
                           % deviceId)
            return
        dsConfig = deviceConfig.findDataSource(dataSourceId)
        if not dsConfig:
            self.log.info(
                  'No data source config found for device %s datasource %s' \
                  % (deviceId, dataSourceId))
            return
        rrdConf = dsConfig.rrdConfig.get(dataPointId)

        if not rrdConf:
            self.log.info(
                'No RRD config found for device %s datasource %s datapoint %s' \
                % (deviceId, dataSourceId, dataPointId))
            return

        devicePath = deviceConfig.path
        dpPath = '/'.join((devicePath, rrdConf.dpName))
        value = self.rrd.save(dpPath, dpValue, rrdConf.rrdType,
                              rrdConf.command)

        for ev in self.thresholds.check(dpPath, time.time(), value):
            eventKey = dsConfig.eventKey
            if ev.has_key('eventKey'):
                ev['eventKey'] = '%s|%s' % (eventKey, ev['eventKey'])
            else:
                ev['eventKey'] = eventKey
            ev['component'] = dsConfig.component
            self.sendThresholdEvent(**ev)


    def runCollection(self, result=None):
        """
        Gather performance stats via the external process
        
        @param result: result
        @type result: string
        @return: Twisted deferred object
        @rtype: Twisted deferred object
        """
        def doCollection(driver):
            """
            Twisted generator function
            
            @param driver: driver
            @type driver: string
            @return: Twisted deferred object
            @rtype: Twisted deferred object
            """
            if self.options.cycle:
                self.heartbeat()

            # Schedule for later
            self.log.debug('doCollection(): starting collection cycle')
            reactor.callLater(self.cycleSeconds, self.runCollection)
            startTime = time.time()
            if self.running:
                self.log.error('last zenjmx collection is still running'
                               )
                return
            self.running = True

            # defensive copy in case the config gets updated. Is this possible?
            configCopy = self.deviceConfigs.values()[:]
            dataSourceConfigs = []
            for config in configCopy:
                deviceId = config.deviceId
                self.log.info('doCollection(): running collection for %s'
                               % deviceId)
                for dsConfigList in \
                    config.jmxDataSourceConfigs.values():
                    dataSourceConfigs.append(dsConfigList)
            jobs = NJobs(self.options.parallel, self.collectJmx,
                         dataSourceConfigs)
            yield jobs.start()
            driver.next()
            self.log.debug('doCollection(): exiting collection cycle')
            cycleTime = time.time() - startTime
            endCycle = self.rrd.endCycle()
            dataPoints = self.rrd.dataPoints
            events = []
            events += self.rrdStats.gauge('cycleTime',
                    self.cycleSeconds, cycleTime)
            events += self.rrdStats.counter('dataPoints',
                    self.heartbeatTimeout, dataPoints)
            events += self.rrdStats.gauge('cyclePoints',
                    self.heartbeatTimeout, endCycle)
            self.sendEvents(events)
            self.log.debug('doCollection(): daemon stats cycle time is %s'
                            % cycleTime)
            self.log.debug('doCollection(): daemon stats data points is %s'
                            % dataPoints)
            self.log.debug('doCollection(): daemon stats end cycle is %s'
                            % endCycle)


        def handleFinish(results):
            """
            Normal collection return
            
            @param results: results
            @type results: string
            """
            self.running = False
            for result in results:
                if isinstance(result, failure.Failure):
                    self.log.error('handleFinish():Failure: %s'
                                    % result)
                    result.printDetailedTraceback()
                elif isinstance(result, Exception):
                    self.log.error('handleFinish():Exception: %s'
                                    % result)
                else:
                    self.log.debug('handleFinish(): success %s'
                                    % result)
            if not self.options.cycle:
                self.stop()


        def handleError(error):
            """
            Abnormal collection return with error data
            
            @param error: error
            @type error: string
            """
            self.running = False
            self.log.error('handleError():Error running doCollection: %s'
                            % error)
            if not self.options.cycle:
                self.stop()

        d = drive(doCollection)
        d.addCallback(handleFinish)
        d.addErrback(handleError)
        return d


    def buildOptions(self):
        """
        Create command-line options to parse out.
        """
        RRDDaemon.buildOptions(self)
        self.parser.add_option(
            '-j',
            '--zenjmxjavaport',
            dest='zenjmxjavaport',
            default=9988,
            type='int',
            help='Port for zenjmxjava process',
            )
        self.parser.add_option('--cycletime', dest='cycletime',
                               default=300, type='int',
                               help='Cycle time, in seconds, to run collection'
                               )
        self.parser.add_option('--concurrentJMXCalls',
                               dest='concurrentJMXCalls',
                               action='store_true', default=False,
                               help='Enable concurrent calls to a JMX server'
                               )
        self.parser.add_option('--parallel', dest='parallel',
                               default=200, type='int',
                               help='Number of devices to collect from at one time'
                               )

    def stop(self):
        """
        Twisted reactor function called when we are shutting down.
        """
        if self.javaProcess:
            self.javaProcess.stop()
            self.javaProcess = None
        RRDDaemon.stop(self)


class ZenJmxJavaClient(ProcessProtocol):
    """
    Protocol to control the zenjmxjava process
    """

    def __init__(
        self,
        args,
        zenjmx,
        cycle=True,
        ):
        """
        Initializer
        
        @param args: argument list for zenjmx
        @type args: list of strings
        @param zenjmx: back-end object reference
        @type zenjmx: ZenJMX object
        @param cycle: cycle time in minutes to collect data
        @type cycle: number
        """
        self.deferred = Deferred()
        self.stopCalled = False
        self.process = None
        self.outReceived = sys.stdout.write
        self.errReceived = sys.stderr.write
        self.log = logging.getLogger('zen.ZenJmxJavaClient')
        self.args = args
        self.cycle = cycle
        self.zenjmx = zenjmx


    def processEnded(self, reason):
        """
        Twisted reactor function called when the process ends.
        
        @param reason: message from the process
        @type reason: string
        """
        self.process = None
        if not self.stopCalled:
            procEndEvent = {
                'eventClass': '/Status/JMX',
                'summary': 'zenjmxjava ended unexpectedly: %s'\
                     % reason.getErrorMessage(),
                'severity': Event.Warning,
                'component': self.zenjmx.name,
                'device': self.zenjmx.options.monitor,
                }
            self.zenjmx.sendEvent(procEndEvent)
            self.log.warn('processEnded():zenjmxjava process ended %s'
                           % reason)
            if self.deferred:
                self.deferred.errback(reason)
            self.deferred = None
            self.log.info('processEnded():restarting zenjmxjava')
            reactor.callLater(1, self.run)


    def stop(self):
        """
        Twisted reactor function called when we are shutting down.
        """
        import signal
        self.log.info('stop():stopping zenjmxjava')
        self.stopCalled = True
        if not self.process:
            self.log.debug('stop():no zenjmxjava process to stop')
            return
        try:
            self.process.signalProcess(signal.SIGKILL)
        except error.ProcessExitedAlready:
            self.log.info('stop():zenjmxjava process already exited')
            pass
        try:
            self.process.loseConnection()
        except Exception:
            pass
        self.process = None


    def connectionMade(self):
        """
        Called when the Twisted reactor starts up
        """
        self.log.debug('connectionMade():zenjmxjava started')

        def doCallback():
            """
            doCallback
            """
            msg = \
                'doCallback(): callback on deferred zenjmxjava proc is up'
            self.log.debug(msg)
            if self.deferred:
                self.deferred.callback('zenjmx java started')
            if self.process:
                procStartEvent = {
                    'eventClass': '/Status/JMX',
                    'summary': 'zenjmxjava started',
                    'severity': Event.Clear,
                    'component': self.zenjmx.name,
                    'device': self.zenjmx.options.monitor,
                    }
                self.zenjmx.sendEvent(procStartEvent)
            self.deferred = None

        if self.deferred:
            self.log.debug('connectionMade():scheduling callback')

            # give the java service a chance to startup
            reactor.callLater(3, doCallback)
        self.log.debug('connectionMade(): done')


    def run(self):
        """
        Twisted function called when started
        """
        if self.stopCalled:
            return
        self.log.info('run():starting zenjmxjava')
        zenjmxjavacmd = os.path.join(ZenPacks.zenoss.ZenJMX.binDir,
                'zenjmxjava')
        if self.cycle:
            args = ('runjmxenabled', )
        else:
            # don't want to start up with jmx server to avoid port conflicts
            args = ('run', )
        if self.args:
            args = args + self.args
        cmd = (zenjmxjavacmd, ) + args
        self.log.debug('run():spawn process %s' % (cmd, ))
        self.deferred = Deferred()
        self.process = reactor.spawnProcess(self, zenjmxjavacmd, cmd,
                env=None)
        return self.deferred


if __name__ == '__main__':
    zjmx = ZenJMX()
    zjmx.run()
