######################################################################
#
# Copyright 2008 Zenoss, Inc.  All Rights Reserved.
#
######################################################################

import Globals
from Products.ZenModel.migrate.Migrate import Version
from Products.ZenModel.ZenPack import ZenPack, ZenPackDataSourceMigrateBase
from ZenPacks.zenoss.ZenJMX.datasources.JMXDataSource import JMXDataSource


class ConvertJMXDataSources(ZenPackDataSourceMigrateBase):
    version = Version(2, 1, 1)
    
    # These provide for conversion of datasource instances to the new class
    dsClass = JMXDataSource
    oldDsModuleName = 'Products.ZenJMX.datasources.JMXDataSource'
    oldDsClassName = 'JMXDataSource'
    
    # Reindex all applicable datasource instances
    reIndex = True
            
