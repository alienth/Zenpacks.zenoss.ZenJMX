///////////////////////////////////////////////////////////////////////////
//
// This program is part of Zenoss Core, an open source monitoring platform.
// Copyright (C) 2008, Zenoss Inc.
//
// This program is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License version 2 as published by
// the Free Software Foundation.
//
// For complete information please visit: http://www.zenoss.com/oss/
//
///////////////////////////////////////////////////////////////////////////
package com.zenoss.zenpacks.zenjmx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zenoss.jmx.JmxClient;
import com.zenoss.jmx.JmxException;
import com.zenoss.zenpacks.zenjmx.call.CallFactory;
import com.zenoss.zenpacks.zenjmx.call.ConfigurationException;
import com.zenoss.zenpacks.zenjmx.call.JmxCall;
import com.zenoss.zenpacks.zenjmx.call.Summary;
import com.zenoss.zenpacks.zenjmx.call.Utility;

public class ZenJmxService {
    private static final Log _logger = LogFactory.getLog(ZenJmxService.class);


    public Object collect(List<Map> dsConfigs) throws Exception {

        _logger.debug("processing "+ dsConfigs.size() +" datasources");
        JMXCollector collector = new JMXCollector(dsConfigs);
        List<Map<String, String>> result = collector.collect();
        _logger.debug("finished processing "+ dsConfigs.size() +" datasources");
        return result;
    }

    public static class JMXCollector {
        private static final String SUMMARY = "summary";
        // logger
        private static final Log _logger = LogFactory
                .getLog(JMXCollector.class);
        boolean _authenticate;
        String _username;
        String _password;
        String _deviceId;
        ConfigAdapter _config;
        List<ConfigAdapter> _configs = new ArrayList<ConfigAdapter>();

        public JMXCollector(List<Map> dataSourceConfigs) {

            ConfigAdapter config = null;
            for (Map configMap : dataSourceConfigs) {
                config = new ConfigAdapter(configMap);
                _configs.add(config);
            }
            // we assume all configs are to the same device
            // and to the same jmx server with same credentials
            if (config != null) {
                _authenticate = config.authenticate();
                _username = config.getUsername();
                _password = config.getPassword();
                _config = config;
                _deviceId = config.getDevice();
            }

        }

        /**
         * collects jmx values and returns a list of results.
         * 
         * @param dsConfigs
         * @return
         */
        public List<Map<String, String>> collect() {

            JmxClient client = null;

            try {
                client = createJmxClient();
                client.connect();
                // TODO: Create success event?
            } catch (Exception e) {
                Map<String, String> error = createConnectionError(_deviceId,
                        "error connecting to server", e);
                return Collections.singletonList(error);

            }

            List<Map<String, String>> result = doCollect(client);
            if (client != null) {
                try {
                    client.close();
                } catch (JmxException e) {
                    Map<String, String> error = createConnectionError(_config
                            .getDevice(), "error closing connection to server",
                            e);
                    result.add(error);
                }
            }
            return result;
        }

        private List<Map<String, String>> doCollect(JmxClient client) {
            // all calls should be to same server with same credentials
            List<Map<String, String>> results = new ArrayList<Map<String, String>>(
                    _configs.size());

            for (ConfigAdapter config : _configs) {
                try {

                    JmxCall call = CallFactory.createCall(config);
                    Summary summary = call.getSummary();
                    try {
                        call.call(client);
                        results.addAll(createResult(summary));
                    } catch (JmxException e) {
                        results.add(createError(summary, config, e));
                    }
                } catch (ConfigurationException e) {
                    Map<String, String> err = createError(config, e);
                    results.add(err);
                }

            }
            return results;
        }

        private JmxClient createJmxClient() throws ConfigurationException {
            JmxClient jmxClient = null;
            String url = Utility.getUrl(_config);
            jmxClient = new JmxClient(url);
            if (_authenticate) {
                String[] creds = new String[] { _username, _password };
                jmxClient.setCredentials(creds);
            }

            return jmxClient;
        }

        private List<Map<String, String>> createResult(Summary summary) {

            if(_logger.isDebugEnabled())
                {
                _logger.debug(summary.toString());
                }
            List<Map<String, String>> results = new ArrayList<Map<String, String>>();

            Map<String, Object> values = summary.getResults();

            for (String key : values.keySet()) {
                Object value = values.get(key);
                if (value == null) {
                    _logger.warn("(" + summary.getCallId()
                            + "): null value for data point: " + key);
                    continue;
                }
                HashMap<String, String> result = new HashMap<String, String>();
                results.add(result);
                result.put(ConfigAdapter.DEVICE, summary.getDeviceId());
                result.put(ConfigAdapter.DATASOURCE_ID, summary
                        .getDataSourceId());
                result.put("value", value.toString());
                result.put("dpId", key);
            }
            return results;
        }

        private Map<String, String> createError(Summary summary,
                ConfigAdapter config, Exception e) {
            String msg = "DataSource %1$s; Error calling mbean %2$s: Exception: %3$s";
            msg = String.format(msg, config.getDatasourceId(), summary
                    .getObjectName(), e.getMessage());
            Map<String, String> error = createError(config, e);
            error.put(SUMMARY, msg);
            return error;
        }

        private HashMap<String, String> createError(String deviceId, String msg) {
            HashMap<String, String> error = new HashMap<String, String>();

            error.put(ConfigAdapter.DEVICE, deviceId);

            error.put(SUMMARY, msg);

            return error;
        }

        private Map<String, String> createConnectionError(String deviceId,
                String msg, Exception e) {
            HashMap<String, String> error = createError(deviceId, msg + ":"
                    + e.getMessage());
            error.put(ConfigAdapter.EVENT_CLASS, "/Status/JMX/Connection");
            return error;

        }

        private Map<String, String> createError(ConfigAdapter config,
                Exception e) {
            HashMap<String, String> error = createError(config.getDevice(), e
                    .getMessage());
            error.put(ConfigAdapter.DATASOURCE_ID, config.getDatasourceId());
            error.put(ConfigAdapter.EVENT_CLASS, config.getEventClass());
            error.put(ConfigAdapter.EVENT_KEY, config.getEventKey());
            return error;
        }
    }

}
