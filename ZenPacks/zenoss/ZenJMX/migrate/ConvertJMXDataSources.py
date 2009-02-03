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

import Globals
from Products.ZenModel.migrate.Migrate import Version
from Products.ZenModel.ZenPack import ZenPack, ZenPackDataSourceMigrateBase
from ZenPacks.zenoss.ZenJMX.datasources.JMXDataSource import JMXDataSource

class ConvertJMXDataSources(ZenPackDataSourceMigrateBase):
    version = Version(3, 1, 2)
    
    # These provide for conversion of datasource instances to the new class
    dsClass = JMXDataSource
    oldDsModuleName = 'Products.ZenJMX.datasources.JMXDataSource'
    oldDsClassName = 'JMXDataSource'
    
    # Reindex all applicable datasource instances
    reIndex = True

