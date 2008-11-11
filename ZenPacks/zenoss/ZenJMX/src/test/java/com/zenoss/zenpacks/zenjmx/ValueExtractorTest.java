package com.zenoss.zenpacks.zenjmx;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryManagerMXBean;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;

import junit.framework.TestCase;

import com.zenoss.jmx.JmxException;
import com.zenoss.jmx.ValueExtractor;
import com.zenoss.zenpacks.zenjmx.call.ZenJMXTest;

public class ValueExtractorTest extends TestCase {

    private MBeanServer mbs = null;
    private CompositeData testComposite = null;
    private TabularData testTabular = null;
    private TabularData testSimpleTabular = null;

    Object gcObject = null;
    ObjectName testObjectName = null;
    ObjectName gcObjectName = null;
    boolean isOneSix = false;

    @Override
    protected void setUp() throws Exception
        {

        String version = System.getProperty("java.version").substring(0, 3);

        if ( Float.parseFloat(version) > 1.5 ) isOneSix = true;

        mbs = ManagementFactory.getPlatformMBeanServer();
        // objectName = new

        testObjectName = new ObjectName(ZenJMXTest.mbeanObjectNameStr);
        if ( isOneSix )
            {
            // some of the tests can not be run unless using java 1.6
            // because ZenJMXTest is an MXBean.
            // MXBeans are 1.6 only and are used for testing because they
            // automatically convert return values to composite or tabular data
            System.out.println("Running 1.6 compatible tests");
            if ( !mbs.isRegistered(testObjectName) )
                {
                ZenJMXTest.registerMbean(mbs);
                }

            Object o = mbs.getAttribute(testObjectName, "CompositeTestData");
            testComposite = (CompositeData) o;

            o = mbs.getAttribute(testObjectName, "TabularTestData");
            testTabular = (TabularData) o;

            o = mbs.getAttribute(testObjectName, "SimpleTabularTestData");
            testSimpleTabular = (TabularData) o;
            }

        // lets find a registered gc mbean
        for (MemoryManagerMXBean gc : ManagementFactory
                .getMemoryManagerMXBeans())
            {
            String name = gc.getName();
            gcObjectName = new ObjectName(
                    "java.lang:type=GarbageCollector,name=" + name);
            if ( mbs.isRegistered(gcObjectName) )
                {
                gcObject = mbs.getAttribute(gcObjectName, "LastGcInfo");
                break;
                }
            }

        }

    public void testSimpleTabularPath() throws Exception
        {
        if ( !isOneSix )
            {
            return;
            }
        Object o = ValueExtractor.getDataValue(testSimpleTabular, "rowOne");
        assertEquals(5, o);
        }

    public void testSimpleTabularPathWithIndex() throws Exception
        {
        if ( !isOneSix )
            {
            return;
            }
        Object o = ValueExtractor.getDataValue(testSimpleTabular, "[rowOne]");
        assertEquals(5, o);
        }

    public void testSimpleTabularBadPath() throws Exception
        {
        if ( !isOneSix )
            {
            return;
            }
        try
            {
            ValueExtractor.getDataValue(testSimpleTabular, "[rowOnse]");
            }
        catch (JmxException e)
            {
            return;
            }
        fail("expected an exception");
        }

    public void testSimpleCompositePath() throws Exception
        {
        if ( !isOneSix )
            {
            return;
            }
        Object o = ValueExtractor.getDataValue(testComposite, "stringValue");
        assertEquals("123", o);
        }

    public void testSimpleCompositeBadPath() throws Exception
        {
        if ( !isOneSix )
            {
            return;
            }
        try
            {
            ValueExtractor.getDataValue(testComposite, "[rowOnse]");
            }
        catch (JmxException e)
            {
            return;
            }
        fail("expected an exception");
        }

    public void testTabularPath() throws Exception
        {
        if ( !isOneSix )
            {
            return;
            }
        Object o = ValueExtractor.getDataValue(testTabular,
                "rowOne.anotherRowValue");
        assertEquals(654, o);
        }

    public void testTabularPathWithIndex() throws Exception
        {
        if ( !isOneSix )
            {
            return;
            }
        Object o = ValueExtractor.getDataValue(testTabular,
                "[rowOne].differentRowValue");
        assertEquals(384, o);
        }

    public void testTabularBadPath() throws Exception
        {
        if ( !isOneSix )
            {
            return;
            }
        try
            {
            ValueExtractor.getDataValue(testTabular, "rowOne.blam");
            }
        catch (JmxException e)
            {
            return;
            }
        fail("expected an exception");
        }

    public void testGetGcThreadCount() throws Exception
        {

        Object result = ValueExtractor.getDataValue(gcObject, "GcThreadCount");
        assertEquals(Integer.class, result.getClass());

        }

    public void testGetMemoryUsageAfterGc() throws Exception
        {

        Object result = ValueExtractor.getDataValue(gcObject,
                "memoryUsageAfterGc");
        assertEquals(TabularDataSupport.class, result.getClass());

        }

    public void testGetMemoryUsageAfterGcEdenSpace() throws Exception
        {

        Object result = ValueExtractor.getDataValue(gcObject,
                "memoryUsageAfterGc.Code Cache");
        assertEquals(CompositeDataSupport.class, result.getClass());

        }

    public void testGetMemoryUsageAfterGcEdenSpaceWithIndex() throws Exception
        {

        Object result = ValueExtractor.getDataValue(gcObject,
                "memoryUsageAfterGc.[Code Cache]");
        assertEquals(CompositeDataSupport.class, result.getClass());

        }

    public void testGetMemoryUsageAfterGcEdenSpaceWithBadIndex()
            throws Exception
        {
        try
            {

            ValueExtractor.getDataValue(gcObject,
                    "memoryUsageAfterGc.[Code Cache, Committed]");
            }
        catch (JmxException e)
            {
            return;
            }
        fail("expected an exception");

        }

    public void testGetMemoryUsageAfterGcEdenSpaceCommitted() throws Exception
        {

        Object result = ValueExtractor.getDataValue(gcObject,
                "memoryUsageAfterGc.Code Cache.committed");
        assertEquals(Long.class, result.getClass());

        }

    public void testGetMemoryUsageAfterGcEdenSpaceCommittedWithIndex()
            throws Exception
        {

        Object result = ValueExtractor.getDataValue(gcObject,
                "memoryUsageAfterGc.[Code Cache].committed");
        assertEquals(Long.class, result.getClass());

        }

    public void testGetMemoryUsageAfterGcEdenSpaceCommittedWithIndexAndColumn()
            throws Exception
        {

        Object result = ValueExtractor.getDataValue(gcObject,
                "memoryUsageAfterGc.[Code Cache].{value}.committed");
        assertEquals(Long.class, result.getClass());

        }

    public void testGetMemoryUsageAfterGcEdenSpaceCommittedWithExtraPath()
            throws Exception
        {
        try
            {
            ValueExtractor.getDataValue(gcObject,
                    "endTime.[Code Cache].committed.pop");
            }
        catch (JmxException e)
            {
            return;
            }
        fail("expected an exception");

        }

}
