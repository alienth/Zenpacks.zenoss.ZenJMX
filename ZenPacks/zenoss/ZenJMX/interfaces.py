###########################################################################
#
# This program is part of Zenoss Core, an open source monitoring platform.
# Copyright (C) 2010, Zenoss Inc.
#
# This program is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 as published by
# the Free Software Foundation.
#
# For complete information please visit: http://www.zenoss.com/oss/
#
###########################################################################
from Products.Zuul.interfaces import IRRDDataSourceInfo
from Products.Zuul.form import schema
from Products.Zuul.utils import ZuulMessageFactory as _t


class IJMXDataSourceInfo(IRRDDataSourceInfo):
    # Connection Info
    jmxPort = schema.Text(title=_t(u'Management Port'), group=_t(u'JMX Connection and Metadata Infomation'))
    jmxProtocol = schema.Choice(title=_t(u'Protocol'), group=_t(u'JMX Connection and Metadata Infomation'),
                                vocabulary='jmxProtocolVocabulary')
    jmxRawService = schema.Text(title=_t(u'Raw Service URL (advanced users only)'), group=_t(u'JMX Connection and Metadata Infomation'))
    rmiContext = schema.Text(title=_t(u'RMI Context (URL context when using RMI Protocol)'), group=_t(u'JMX Connection and Metadata Infomation'))
    objectName = schema.Text(title=_t(u'Object Name'), group=_t(u'JMX Connection and Metadata Infomation'))

    # Authentication
    username = schema.Text(title=_t(u'Username'), group=_t(u'JMX Remote Authentication Information'))
    authenticate = schema.Text(title=_t(u'Auth Enabled'), group=_t(u'JMX Remote Authentication Information'))
    password = schema.Password(title=_t(u'Password'), group=_t(u'JMX Remote Authentication Information'))
    
    # Operation
    attributeName = schema.Text(title=_t(u'Attribute Name'), group=_t(u'JMX Attribute and Operation Configuration'))
    attributePath = schema.Text(title=_t(u'Attribute Path'), group=_t(u'JMX Attribute and Operation Configuration'))
    operationParamValues = schema.Text(title=_t(u'Parameter Values'), group=_t(u'JMX Attribute and Operation Configuration'))
    operationName = schema.Text(title=_t(u'Operation Name'), group=_t(u'JMX Attribute and Operation Configuration'))
    operationParamTypes = schema.Text(title=_t(u'Parameter Types'), group=_t(u'JMX Attribute and Operation Configuration'))
    
    

