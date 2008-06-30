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

__doc__='''MailTxDataSource.py

Defines datasource for JMX collection.  Using this data source you can
define MBean objects and attributes as data sources and data points.

Part of ZenJMX zenpack.
'''

from Products.ZenModel.RRDDataSource import RRDDataSource
from AccessControl import ClassSecurityInfo, Permissions
from Products.ZenModel.ZenPackPersistence import ZenPackPersistence

Base = RRDDataSource
class JMXDataSource(ZenPackPersistence, Base):
    
    ZENPACKID = 'ZenPacks.zenoss.ZenJMX'
    
    URL = 'service:jmx:rmi:///jndi/rmi://%(manageIp)s:%(jmxPort)s/jmxrmi'

    JMX = 'JMX'

    sourcetypes = (JMX,)
    sourcetype = JMX

    timeout = 15
    eventClass = '/Status/JMX'
    component = JMX

    hostname = '${dev/id}'
    expectedIpAddress = ''

    jmxPort = ''
    jmxProtocol = 'RMI'
    objectName = ''
    username = ''
    password = ''
    authenticate = False

    attributeName = ''
    operationName = ''
    operationParamValues = ''
    operationParamTypes = ''

    eventClass = '/App/Java/JMX'

    _properties = Base._properties + (
        {'id':'jmxPort', 'type':'string', 'mode':'w'},
        {'id':'jmxProtocol', 'type':'string', 'mode':'w'},
        {'id':'objectName', 'type':'string', 'mode':'w'},

        {'id':'authenticate', 'type':'string', 'mode':'w'},
        {'id':'username', 'type':'string', 'mode':'w'},
        {'id':'password', 'type':'string', 'mode':'w'},

        {'id':'attributeName', 'type':'string', 'mode':'w'},

        {'id':'operationName', 'type':'string', 'mode':'w'},
        {'id':'operationParamValues', 'type':'string', 'mode':'w'},
        {'id':'operationParamTypes', 'type':'string', 'mode':'w'},
        )

    _relations = Base._relations + (
        )

    factory_type_information = ( 
    { 
        'immediate_view' : 'editJMXDataSource',
        'actions'        :
        ( 
            { 'id'            : 'edit',
              'name'          : 'Data Source',
              'action'        : 'editJMXDataSource',
              'permissions'   : ( Permissions.view, ),
            },
        )
    },
    )

    security = ClassSecurityInfo()

    def __init__(self, id, title=None, buildRelations=True):
        Base.__init__(self, id, title, buildRelations)

    def getDescription(self):
        if self.sourcetype == self.JMX:
            return self.hostname
        return RRDDataSource.getDescription(self)


    def getProtocols(self):
        """return list of supported JMX protocols"""
        return ['RMI', 'JMXMP']


    def zmanage_editProperties(self, REQUEST=None):
        '''validation, etc'''
        return Base.zmanage_editProperties(self, REQUEST)


