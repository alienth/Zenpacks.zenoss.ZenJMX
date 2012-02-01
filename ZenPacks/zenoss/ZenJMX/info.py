###########################################################################
#
# This program is part of Zenoss Core, an open source monitoring platform.
# Copyright (C) 2010, Zenoss Inc.
#
# This program is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 or (at your
# option) any later version as published by the Free Software Foundation.
#
# For complete information please visit: http://www.zenoss.com/oss/
#
###########################################################################
from zope.interface import implements
from zope.schema.vocabulary import SimpleVocabulary
from Products.Zuul.infos import ProxyProperty
from Products.Zuul.infos.template import RRDDataSourceInfo
from ZenPacks.zenoss.ZenJMX.interfaces import IJMXDataSourceInfo
from ZenPacks.zenoss.ZenJMX.datasources.JMXDataSource import JMXDataSource

def jmxProtocolVocabulary(context):
    return SimpleVocabulary.fromValues(JMXDataSource.protocolTypes)

class JMXDataSourceInfo(RRDDataSourceInfo):
    implements(IJMXDataSourceInfo)
    
    # JMX RMI
    jmxPort = ProxyProperty('jmxPort')
    jmxProtocol = ProxyProperty('jmxProtocol')
    jmxRawService = ProxyProperty('jmxRawService')
    rmiContext = ProxyProperty('rmiContext')
    objectName = ProxyProperty('objectName')
    
    # Authentication
    authenticate = ProxyProperty('authenticate')
    username = ProxyProperty('username')
    password = ProxyProperty('password')
    attributeName = ProxyProperty('attributeName')
    attributePath = ProxyProperty('attributePath')
    # Operation
    operationName = ProxyProperty('operationName')
    operationParamValues = ProxyProperty('operationParamValues')
    operationParamTypes = ProxyProperty('operationParamTypes')
    
    @property
    def testable(self):
        """
        We can NOT test this datsource against a specific device
        """
        return False
    


