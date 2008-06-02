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

ZENJMX_HOME=$(PWD)/ZenPacks/zenoss/ZenJMX
LIB_DIR=$(ZENJMX_HOME)/lib
TARGET=$(ZENJMX_HOME)/target

default: egg


egg:
    # setup.py will call 'make build' before creating the egg
	python setup.py bdist_egg


build:
	mvn assembly:assembly

	mkdir -p ${LIB_DIR} ; \
	cd ${LIB_DIR} ; \
	tar xzf ${TARGET}/*-bin.tar.gz

	cp ${ZENJMX_HOME}/zenjmx.conf ${LIB_DIR}


clean:
	rm -rf build dist ${LIB_DIR} ${TARGET}
	rm -rf *.egg-info
	find . -name *.pyc | xargs rm