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
pack = 'ZenJMX'
__doc__ = '%s ZenPack.  Adds JMX support to Zenoss' % pack

import Globals
import os
import sys
from os.path import join
from Products.ZenModel.ZenPack import ZenPackBase
from Products.CMFCore.DirectoryView import registerDirectory

skinsDir = os.path.join(os.path.dirname(__file__), 'skins')
if os.path.isdir(skinsDir):
    registerDirectory(skinsDir, globals())

libDir = os.path.join(os.path.dirname(__file__), 'lib')
if os.path.isdir(libDir):
    sys.path.append(libDir)

binDir = os.path.join(os.path.dirname(__file__), 'bin')

class ZenPack(ZenPackBase):
    "ZenPack Loader that loads zProperties used by ZenJMX"
    packZProperties = [
        ('zJmxManagementPort', 12345, 'int'),
        ('zJmxAuthenticate', False, 'boolean'),
        ('zJmxUsername', 'admin', 'string'),
        ('zJmxPassword', 'admin', 'string'),
        ]

