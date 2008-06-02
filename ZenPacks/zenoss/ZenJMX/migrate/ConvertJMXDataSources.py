######################################################################
#
# Copyright 2008 Zenoss, Inc.  All Rights Reserved.
#
######################################################################

import Globals
from Products.ZenModel.migrate.Migrate import Version
from Products.ZenModel.ZenPack import ZenPack, ZenPackDataSourceMigrateBase
from ZenPacks.zenoss.ZenJMX.datasources.JMXDataSource import ZenJMXDataSource


class ConvertZenJMXDataSources(ZenPackDataSourceMigrateBase):
    version = Version(2, 0, 0)
    
    # These provide for conversion of datasource instances to the new class
    dsClass = ZenJMXDataSource
    oldDsModuleName = 'Products.ZenJMX.datasources.ZenJMXDataSource'
    oldDsClassName = 'ZenJMXDataSource'
    
    # Reindex all applicable datasource instances
    reIndex = True
            
