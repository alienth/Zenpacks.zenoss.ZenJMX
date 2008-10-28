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

ZENJMX_HOME=$(PWD)
LIB_DIR=$(ZENJMX_HOME)/lib
TARGET=$(ZENJMX_HOME)/target

default: install


install:
	mvn assembly:assembly

	mkdir -p ${LIB_DIR} ; \
	cd ${LIB_DIR} ; \
	tar xzf ${TARGET}/*-bin.tar.gz

	cp ${ZENJMX_HOME}/jmxremote_optional.jar ${LIB_DIR}
	cp ${ZENJMX_HOME}/src/main/resources/log4j.properties ${LIB_DIR}
	rm -rf ${TARGET}
	rm -f ${LIB_DIR}/zenjmx-1.0-SNAPSHOT/junit-3.8.1.jar
	rm -f ${LIB_DIR}/zenjmx-1.0-SNAPSHOT/xml-apis-1.0.b2.jar


clean:
	rm -rf ${LIB_DIR}
	rm -rf ${TARGET}
