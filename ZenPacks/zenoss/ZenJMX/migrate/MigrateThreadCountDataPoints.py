###########################################################################
#
# This program is part of Zenoss Core, an open source monitoring platform.
# Copyright (C) 2009, Zenoss Inc.
#
# This program is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 as published by
# the Free Software Foundation.
#
# For complete information please visit: http://www.zenoss.com/oss/
#
###########################################################################

from Products.ZenModel.migrate.Migrate import Version
from Products.ZenModel.ZenPack import ZenPackMigration
import logging
log = logging.getLogger("zen")

class MigrateThreadCountDataPoints(ZenPackMigration):
    version = Version(3, 1, 4)
    
    def migrate(self, pack):
        log.info("MigrateThreadCountDataPoints migrate")
        #find devices with either the java or zenjmx templat
        #and delete the rrd file for the threadcount datapoint
        
        for d in pack.dmd.Devices.getSubDevices():
            log.debug("MigrateThreadCountDataPoints device %s" % d.id)

            for template in d.getRRDTemplates():

                templateId = template.getPrimaryDmdId()
                log.debug("MigrateThreadCountDataPoints template %s" % templateId)

                dpName = None
                if  templateId == '/Devices/rrdTemplates/Java':
                    dpName = 'Thread Count_ThreadCount'
                elif templateId == '/Devices/rrdTemplates/ZenJMX':
                    dpName = 'ZenJMX Thread Count_ThreadCount'
                
                log.warn("MigrateThreadCountDataPoints dpName %s" % dpName)
                if dpName:
                    perfConf = d.getPerformanceServer()
                    log.debug("MigrateThreadCountDataPoints perfConf %s" % perfConf.id)
                    perfConf.deleteRRDFiles(device=d.id, datapoint=dpName)
