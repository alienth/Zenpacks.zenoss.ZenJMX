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

import Globals
from Products.ZenModel.ZenPack import ZenPackMigration
from Products.ZenModel.migrate.Migrate import Version
from Products.ZenModel.migrate.MigrateUtils import migratePropertyType

class PasswordType(ZenPackMigration):
    version = Version(3, 2, 0)
    
    def migrate(self, pack):
        """
        change zJmxPassword to be of type 'password' and run transformer
        against it.
        """
        migratePropertyType(pack.Devices, 'zenJmxPassword', 'password')
        
