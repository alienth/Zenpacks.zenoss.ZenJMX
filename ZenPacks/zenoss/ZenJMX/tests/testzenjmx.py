###########################################################################
#
# This program is part of Zenoss Core, an open source monitoring platform.
# Copyright (C) 2007, Zenoss Inc.
#
# This program is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 as published by
# the Free Software Foundation.
#
# For complete information please visit: http://www.zenoss.com/oss/
#
###########################################################################

import os
import os.path
import logging
from exceptions import *
from Products.ZenTestCase.BaseTestCase import BaseTestCase
from ZenPacks.zenoss.ZenJMX.zenjmx import ZenJMX
from Products.ZenRRD.RRDUtil import RRDUtil
from Products.ZenModel.Exceptions import *


class FakeDevice(object):
    def __init__( self, name, **kwargs ):
        self.id= name
        for propertyName, value in kwargs.items():
            setattr(self, propertyName, value )

class FakeConfig(object):
    def __init__(self, data_source):
        self.ds = data_source

    def findDataSource( self, ds ):
        return self.ds

class FakeDSConfig(object):
    def __init__(self, dp, rrd_conf):
        self.rrdConfig = { dp:rrd_conf }

class FakeRRDConfig(object): pass

class FakeOptions: pass

class zem: pass


class TestZenJMX(BaseTestCase):

    def setUp(self):
        BaseTestCase.setUp(self)

        # Trap n toss all output to make the test prettier
        # Otherwise it will drive you insane, yelling
        # "SHUT UP! SHUT UP!" to your monitor.
        # Since no good will come of that...
#        logging.disable(logging.INFO)
#        logging.disable(logging.WARN)
#        logging.disable(logging.ERROR)
#        logging.disable(logging.CRITICAL)
#
        # Note: the docs (http://docs.python.org/library/logging.html#logging-levels)
        #       imply that we can override the above behaviour by passing
        #       a handler object to logging.getLogger().addHandler(handler),
        #       but that doesn't seem to work.

        # Make a valid test device
        testdev = str(self.__class__.__name__)
        self.name = testdev

        # name, path, dataStorageType, rrdCreateCommand, minmax
        self.path= os.path.join( "tests", testdev )

        self.dev = self.dmd.Devices.createInstance(testdev)
        self.zem = zem()
        self.zem.sendEvent = self.sendEvent
        

        # We're not connected to zenhub so the following
        # always will be None
        perfServer = self.dev.getPerformanceServer()
        if perfServer:
            defrrdcmd= perfServer.getDefaultRRDCreateCommand()
        else:
            # We will always use this :(
            defrrdcmd= 'RRA:AVERAGE:0.5:1:600\nRRA:AVERAGE:0.5:6:600\nRRA:AVERAGE:0.5:24:600\nRRA:AVERAGE:0.5:288:600\nRRA:MAX:0.5:6:600\nRRA:MAX:0.5:24:600\nRRA:MAX:0.5:288:600'

        self.rrdcmd = defrrdcmd
        self.zjmx = ZenJMX( noopts=True )
        # default RRD create command, cycle interval
        self.zjmx.rrd= RRDUtil( defrrdcmd, 60 )

        # Fake out some options for sending alerts
        self.zjmx.options = FakeOptions()
        self.zjmx.options.monitor = testdev

        # Save the following info for our tearDown() script
        self.perfpath= self.zjmx.rrd.performancePath( "tests" )
        self.zjmx.rrd.performancePath= lambda(x): os.path.join( self.perfpath, x )


    def sendEvent(self, evt):
        "Fakeout sendEvent() method"
        self.sent = evt


    def rrdsave_setup(self, oid, data ):
        class FakeProxy: pass
        proxy = FakeProxy()
        proxy.oidMap = {}
        proxy.oidMap[ oid ] = data
        self.zjmx.proxies[ self.name ]= proxy


    def testGoodRRDSave(self):
        """
        Sanity check to make sure that RRD stores work
        """
        ds = 'jmxDS'
        dp = 'jmxdp'
        results = [ {
             'device': self.name,
             'datasourceId': ds,
             'dpId': dp,
             'value': 1.0,

        } ]

        rrdConf = FakeRRDConfig()
        rrdConf.rrdType = 'COUNTER'
        rrdConf.command = self.rrdcmd
        rrdConf.dpName = dp
        dsConfig = FakeDSConfig( dp, rrdConf )
        deviceConfig = FakeConfig( dsConfig )
        deviceConfig.path = self.perfpath
        self.zjmx.deviceConfigs = { self.name: deviceConfig }

        self.zjmx.storeRRD( self.name, ds, dp, 666.0 )


    def testUnableToWrite(self):
        """
        Can't write to disk
        """
        # Verify that we're not root first...
        if os.geteuid() == 0:
            print "Can't run testUnableToWrite check if running as root"
            return


        self.zjmx.rrd.performancePath= lambda(x): "/"

        # Fake out sendEvent
        evts = []
        def append(evt):
            if evt['severity'] != 0:
                evts.append(evt)
        self.zjmx.sendEvent = append

        ds = 'jmxDS'
        dp = 'jmxdp'
        results = [ {
             'device': self.name,
             'datasourceId': ds,
             'dpId': dp,
             'value': 1.0,
        } ]

        rrdConf = FakeRRDConfig()
        rrdConf.rrdType = 'COUNTER'
        rrdConf.command = self.rrdcmd
        rrdConf.dpName = dp
        dsConfig = FakeDSConfig( dp, rrdConf )
        deviceConfig = FakeConfig( dsConfig )
        # This will try to create a /.rrd file, which should fail
        deviceConfig.path = '/'
        self.zjmx.deviceConfigs = { self.name: deviceConfig }

        self.assertRaises( Exception, self.zjmx.storeRRD, self.name, ds, dp, 666.0 )

        # When fixed no error should be raised
        #self.zjmx.storeRRD( self.name, ds, dp, 666.0 )
        #self.assertNotEquals( len(evts), 0 )




    def tearDown(self):
        """
        Clean up after our tests
        """
        import shutil
        try:
            shutil.rmtree( self.perfpath )
        except:
            pass

        BaseTestCase.tearDown(self)



def test_suite():
    from unittest import TestSuite, makeSuite
    suite = TestSuite()
    suite.addTest(makeSuite(TestZenJMX))
    return suite

