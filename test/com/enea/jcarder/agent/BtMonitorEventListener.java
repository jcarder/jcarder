package com.enea.jcarder.agent;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;

import com.enea.jcarder.agent.instrument.ClassTransformer;
import com.enea.jcarder.agent.instrument.InstrumentConfig;
import com.enea.jcarder.agent.instrument.TransformClassLoader;
import com.enea.jcarder.common.contexts.ContextMemory;
import com.enea.jcarder.common.events.LockEventListenerIfc;
import com.enea.jcarder.testclasses.agent.ComparableAlternativeSynchronizationRoutes;
import com.enea.jcarder.testclasses.agent.RepeatMostRecentlySynchronization;
import com.enea.jcarder.testclasses.agent.RepeatOlderSynchronization;
import com.enea.jcarder.testclasses.agent.SingleSynchronization;
import com.enea.jcarder.testclasses.agent.SynchronizationTestIfc;
import com.enea.jcarder.testclasses.agent.TwoThreadSynchronization;

/**
 * The purpose of this junit class is to test the classes:
 *   - MonitorEventListener
 *   - ThreadLocalEnteredMonitors
 *
 * But it serves also as an integration test with the MonitorEventListener and
 * the com.enea.jcarder.agent.instrument package. One interesting aspect of the
 * integration is that MonitorEventListener/EnteredMonitor depends on that
 * the monitor events are sent BEFORE instead of AFTER the monitor is entered.
 */
public final class BtMonitorEventListener implements LockEventListenerIfc {
    private ContextMemory mContextMemory;
    private final TransformClassLoader mClassLoader;
    private final LinkedList<LockEvent> mEvents = new LinkedList<LockEvent>();

    public BtMonitorEventListener() {
        ClassTransformer transformer =
            new ClassTransformer(new InstrumentConfig());
        mClassLoader = new TransformClassLoader(transformer);
    }

    @Before
    public void setUp() throws Exception {
        mContextMemory = new ContextMemory();
        StaticEventListener.setListener(new EventListener(this,
                                                          mContextMemory));
    }

    private void testClass(Class clazz) throws Exception {
        SynchronizationTestIfc test = transformAsSynchronizationTest(clazz);
        test.go();

//        String actual = Arrays.deepToString(mEvents.toArray());
//        System.out.println("  Actual: " + actual);
//        String expected = Arrays.deepToString(test.getExpectedLockEvents());
//        System.out.println("Expected: " + expected);

        assertEquals(test.getExpectedLockEvents(),
                     mEvents.toArray());
    }

    private SynchronizationTestIfc transformAsSynchronizationTest(Class clazz)
    throws Exception {
        Class c = mClassLoader.transform(clazz);
        return (SynchronizationTestIfc) c.newInstance();
    }

    public void onLockEvent(int lockId,
                            int lockingContextId,
                            int lastTakenLockId,
                            int lastTakenLockingContextId,
                            long threadId)
    throws IOException {
        ContextMemory cm = mContextMemory;
        LockEvent event =
            new LockEvent(cm.readLock(lockId),
                          cm.readContext(lockingContextId),
                          cm.readLock(lastTakenLockId),
                          cm.readContext(lastTakenLockingContextId));
        mEvents.add(event);
    }

    @Test
    public void testRepeatMostRecentlySynchronization() throws Exception {
        testClass(RepeatMostRecentlySynchronization.class);
    }

    @Test
    public void testRepeatOlderSynchronization() throws Exception {
        testClass(RepeatOlderSynchronization.class);
    }

    @Test
    public void testComparableAlternativeSynchronizationRoutes()
    throws Exception {
        testClass(ComparableAlternativeSynchronizationRoutes.class);
    }

    @Test
    public void testSingleSynchronization() throws Exception {
        testClass(SingleSynchronization.class);
    }

    @Test
    public void testTwoThreadSynchronization() throws Exception {
        testClass(TwoThreadSynchronization.class);
    }
}
