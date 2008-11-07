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
package com.zenoss.zenpacks.zenjmx.call;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.zenoss.zenpacks.zenjmx.call.JMXTestData.NestedDataRow;

public class ZenJMXTest implements ZenJMXTestMXBean {

    public static String lastCall = "None";
    public static Object[] lastArgs = null;
    public static Object lastReturn = "None";
    private AtomicInteger count = new AtomicInteger(0);

    public static String mbeanObjectNameStr = "com.zenoss:type=ZenJMXTest";

    public static void main(String[] args) throws Exception
        {

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        registerMbean(mbs);
        // sleep for a really long time - not ideal but this is only test code
        while (true)
            {
            Thread.sleep(Long.MAX_VALUE);
            }
        }

    public static void registerMbean(MBeanServer mbs) throws Exception
        {

        String mbeanClassName = ZenJMXTest.class.getName();
        ObjectName mbeanObjectName = ObjectName
                .getInstance("com.zenoss:type=ZenJMXTest");
        System.out.println("creating mbean " + mbeanObjectName);
        mbs.createMBean(mbeanClassName, mbeanObjectName);

        }

    public Integer increment(Integer arg1)
        {
        int value = count.getAndAdd(arg1);
        if ( value == Integer.MAX_VALUE ) count = new AtomicInteger(0);
        return value;
        }

    public JMXTestData getCompositeTestData()
        {

        return new JMXTestData();
        }

    public Map<String, NestedDataRow> getTabularTestData()
        {

        Map<String, NestedDataRow> result = new HashMap<String, NestedDataRow>();

        result.put("rowOne", new NestedDataRow(654, 384, 938));
        result.put("rowTwo", new NestedDataRow(1, 2, 3));

        return result;
        }

    public Map<String, Integer> getSimpleTabularTestData()
        {

        Map<String, Integer> result = new HashMap<String, Integer>();

        result.put("rowOne", 5);
        result.put("rowTwo", 833);

        return result;
        }

    public Map<Integer, Integer> getIndexedTabularTestData()
        {

        Map<Integer, Integer> result = new HashMap<Integer, Integer>();

        result.put(1, 5);
        result.put(2, 833);

        return result;
        }

}
