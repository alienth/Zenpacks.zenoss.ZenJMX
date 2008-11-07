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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class JMXTestData implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 4003588084046125117L;

    public int valueOne = 989;
    public String stringValue = "123";

    public int getValueOne()
        {
        return valueOne;
        }

    public String getStringValue()
        {
        return stringValue;
        }

    public NestedData getNested()
        {
        return new NestedData();
        }

    public static class NestedData {

        public String getNestedValue()
            {
            return nestedValue;
            }

        public Map<String, NestedDataRow> getRows()
        {
        Map<String, NestedDataRow> result = new HashMap<String, NestedDataRow>();
        
        result.put("rowOne", new NestedDataRow(654,384,938));
        result.put("rowTwo", new NestedDataRow(1,2,3));

        return result;
            
        }

        public String nestedValue = "321";
    }

    public static class NestedDataRow {

    
        public NestedDataRow(int anotherRowValue, int differntRowValue,
                int rowValue)
            {
            super();
            this.anotherRowValue = anotherRowValue;
            this.differntRowValue = differntRowValue;
            this.rowValue = rowValue;
            }

        public int rowValue = 654;
        public int anotherRowValue = 938;
        public int differntRowValue = 384;

        public int getRowValue()
            {
            return rowValue;
            }

        public int getAnotherRowValue()
            {
            return anotherRowValue;
            }

        public int getDifferentRowValue()
            {
            return differntRowValue;
            }
    }
}
