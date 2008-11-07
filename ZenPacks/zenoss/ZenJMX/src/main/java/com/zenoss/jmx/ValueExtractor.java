///////////////////////////////////////////////////////////////////////////
//
//Copyright 2008 Zenoss Inc 
//Licensed under the Apache License, Version 2.0 (the "License"); 
//you may not use this file except in compliance with the License. 
//You may obtain a copy of the License at 
//    http://www.apache.org/licenses/LICENSE-2.0 
//Unless required by applicable law or agreed to in writing, software 
//distributed under the License is distributed on an "AS IS" BASIS, 
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
//See the License for the specific language governing permissions and 
//limitations under the License.
//
///////////////////////////////////////////////////////////////////////////
package com.zenoss.jmx;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.InvalidKeyException;
import javax.management.openmbean.TabularData;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ValueExtractor {

    private static final Log _logger = LogFactory.getLog(ValueExtractor.class);

    /**
     * Traverses a TabularData or CompositeData structure to return nested data
     * 
     * e.g. To get the the used perm gen memory before last garbage collection
     * value from the result of the lastGcInfo attribute of the
     * java.lang:type=GarbageCollector,name=Copy mbean the path used would be
     * 
     * memoryUsageBeforeGc.[Perm Gen].used
     * 
     * In general the non bracketed values are keys into CompositeData and the
     * bracketed values are indexes into TabularData. For TabularData indexes
     * with more than one value a comma separated list without spaces should be
     * used, spaces are treated as part of the values in the index array.
     * 
     * e.g. [key1,key2]
     * 
     * The brackets aren't mandatory for indexes but are included for clarity.
     * 
     * @param obj
     *            TabularData or CompositeData
     * @param path
     *            dot separated string that represents a path through the object
     * @return Object the value at the end of the path
     * @throws JmxException
     *             if a path element doesn't exist
     */
    public static Object getDataValue(final Object obj, String path)
            throws JmxException
        {
        if ( !(obj instanceof TabularData) && !(obj instanceof CompositeData) )
            {

            throw new IllegalArgumentException("Cannot process object of type "
                    + obj.getClass().getName());

            }

        _logger.debug("getDataValue: path is " + path);

        String[] pathArray = path.split("\\.");
        List<String> pathList = Arrays.asList(pathArray);
        pathList = new LinkedList<String>(pathList);
        _logger.debug("getDataValue: pathList " + pathList);

        Object currentObj = obj;
        Iterator<String> pathElements = pathList.iterator();
        try
            {
            while (pathElements.hasNext())
                {
                _logger.debug("getDataValue: current object is " + obj);
                String currentKey = pathElements.next();
                pathElements.remove();
                _logger.debug("getDataValue: currentKey: " + currentKey);

                if ( currentObj instanceof TabularData )
                    {
                    _logger.debug("getDataValue: dealing with tabularData");
                    TabularData tData = (TabularData) currentObj;
                    String tableIndex = null;
                    // String rowValueName = null;
                    // check for explicit table index
                    if ( currentKey.startsWith("[") && currentKey.endsWith("]") )
                        {
                        _logger
                                .debug("getDataValue: looks like an explicit index: "
                                        + currentKey);
                        tableIndex = currentKey;
                        }
                    else
                        {
                        _logger.debug("getDataValue: no explicit index: "
                                + currentKey);
                        // no explicit table index,
                        // assume index is the same as the name of the value
                        tableIndex = "[" + currentKey + "]";
                        _logger.debug("getDataValue: create implied index "
                                + tableIndex);
                        // rowValueName = currentKey;
                        // _logger.debug("getDataValue: row name is "
                        // + rowValueName);
                        }
                    String[] index = createTableIndex(tableIndex);
                    currentObj = getDataByTableIndex(tData, index);
                    // if ( rowValueName != null )
                    // {
                    CompositeData cData = (CompositeData) currentObj;
                    currentObj = getTableRowData(cData, index);
                    // }
                    }
                else if ( currentObj instanceof CompositeData )
                    {
                    _logger.debug("getDataValue: dealing with CompositeData");
                    CompositeData cData = (CompositeData) currentObj;
                    currentObj = getData(cData, currentKey);
                    }
                else
                    {
                    // we still have a path but the object isn't composite or
                    // tabluar
                    String remainingPath = currentKey;
                    for (String val : pathList)
                        {
                        remainingPath += ".";
                        remainingPath += val;

                        }
                    _logger.warn("getDataValue: we still have a path but the "
                            + "object isn't composite or tabluar");
                    _logger.warn("getDataValue: remaining path is "
                            + remainingPath);
                    throw new JmxException("we still have a path but the "
                            + "object isn't composite or tabluar, remaining "
                            + "" + "path is " + remainingPath);

                    }

                }
            }
        catch (Exception e)
            {
            _logger.warn("could not get object for path " + path, e);
            throw new JmxException("could not get object for path " + path
                    + "; " + e.getMessage(), e);
            }
        return currentObj;
        }

    private static Object getData(CompositeData cData, String key)
        {
        _logger.debug("composite data is: " + cData);
        _logger.debug("getting " + key + " from composite data");
        Object result = cData.get(key);
        _logger.debug("value from composite data is " + result);
        return result;
        }

    private static String[] createTableIndex(String index)
        {
        _logger.debug("creating index for " + index);
        // remove first and last char - should be brackets
        index = index.substring(1, index.length() - 1);
        _logger.debug("spliting " + index + " for index ");
        String[] indexValues = index.split(",");

        _logger.debug("index is " + Arrays.toString(indexValues));

        return indexValues;
        }

    private static Object getDataByTableIndex(TabularData tData,
            String[] tableIndex) throws JmxException
        {
        _logger.debug("TablularData is: " + tData);

        _logger.debug("extracting composite data from tabulardata with index "
                + Arrays.toString(tableIndex));
        CompositeData composite = (CompositeData) tData.get(tableIndex);
        if ( composite == null )
            {
            throw new JmxException(Arrays.toString(tableIndex)
                    + " is not an existing Index for this tabular data ");
            }
        _logger.debug("extracted composite data: " + composite);
        return composite;
        }

    private static Object getTableRowData(CompositeData cData, String[] index)
            throws JmxException
        {
        Object result = null;
        // This gets the data with a key not in index
        Set<String> keys = new HashSet<String>(Arrays.asList(index));

        for (String key : keys)
            {
            if ( !cData.values().contains(key) )
                {
                _logger.warn(key
                        + " not found in composite data row for tabular data");
                throw new JmxException(key
                        + " not found in composite data row for tabular data");

                }
            }

        // find the first value that isn't a part of the index
        for (Object value : cData.values())
            {
            if ( !keys.contains(value) )
                {
                result = value;
                }
            }
        return result;
        }

}
