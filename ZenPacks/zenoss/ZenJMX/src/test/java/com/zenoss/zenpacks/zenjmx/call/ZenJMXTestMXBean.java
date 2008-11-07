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

import java.util.Map;

import com.zenoss.zenpacks.zenjmx.call.JMXTestData.NestedDataRow;

public interface ZenJMXTestMXBean {

    public JMXTestData getCompositeTestData();
    public Map<String, NestedDataRow> getTabularTestData();
    public Map<String, Integer> getSimpleTabularTestData();
    public Map<Integer, Integer> getIndexedTabularTestData();


}
