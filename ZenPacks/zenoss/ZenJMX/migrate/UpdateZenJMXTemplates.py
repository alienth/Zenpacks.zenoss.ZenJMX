###########################################################################
#
# This program is part of Zenoss Core, an open source monitoring platform.
# Copyright (C) 2008, Zenoss Inc.
#
# This program is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 or (at your
# option) any later version as published by the Free Software Foundation.
#
# For complete information please visit: http://www.zenoss.com/oss/
#
###########################################################################

import Globals
from Products.ZenModel.migrate.Migrate import Version
from Products.ZenModel.ZenPack import ZenPack, ZenPackMigration
from ZenPacks.zenoss.ZenJMX.datasources.JMXDataSource import JMXDataSource
import logging
log = logging.getLogger("zen")

class UpdateZenJMXTemplates(ZenPackMigration):
    version = Version(3, 1, 2)
    
    def migrate(self, pack):        
        log.debug("UpdateZenJMXTemplates migrate")
        zenJMXTemplate = pack.dmd.Devices.rrdTemplates.ZenJMX
        #delete old datasources
        def deleteDataSource(dsId):
            
            log.debug("deleteDataSource for %s", dsId)
            ds = None
            try:
                log.debug("looking up datasource")
                ds = zenJMXTemplate.datasources._getOb(dsId)
                log.debug("found datasource")
            except AttributeError:
                log.debug("error looking up datasource")
                log.debug("%s datasource does not exist", dsId)
                return
            pack.manage_deletePackable([ds.getPrimaryUrlPath()])
            zenJMXTemplate.manage_deleteRRDDataSources([dsId])
            

        deleteDataSource('Non-Heap Memory')
        deleteDataSource('Heap Memory')
        deleteDataSource('Open File Descriptors')
        deleteDataSource('Thread Count')
        
        #remote graph datapoints that were in the deleted datasources 
        def deleteGraphPoint(graphId, dpId):
            log.debug("deleteGraphPoint for %s", dpId)
            graph = None
            try:
                graph = zenJMXTemplate.graphDefs._getOb(graphId)
            except AttributeError:
                log.debug("%s graph def does not exist", graphId)
                return
            
            gp = None
            try:
                gp = graph.graphPoints._getOb(dpId)
            except AttributeError:
                log.debug("%s graph point does not exist on graph %s", 
                         dpId, graphId)
                return
            pack.manage_deletePackable([gp.getPrimaryUrlPath()])
            graph.manage_deleteGraphPoints([dpId])            
        
        deleteGraphPoint('ZenJMX Non-Heap Memory','Non-Heap Memory_committed')
        
        deleteGraphPoint('ZenJMX Non-Heap Memory','Non-Heap Memory_used')
        
        deleteGraphPoint('ZenJMX Heap Memory','Heap Memory_used')
        
        deleteGraphPoint('ZenJMX Heap Memory','Heap Memory_committed')
        
        deleteGraphPoint('ZenJMX Open File Descriptors',
                         'Open File Descriptors_OpenFileDescriptorCount')
        
        deleteGraphPoint('ZenJMX Thread Count','Thread Count_ThreadCount')

