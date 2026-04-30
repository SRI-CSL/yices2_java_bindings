package com.sri.yices;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assume.assumeTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class TestThreads {

    public static final int THREAD_COUNT = Integer.getInteger("com.sri.yices.testThreads.count", 32);
    public static final long THREAD_TIMEOUT_SECONDS = Long.getLong("com.sri.yices.testThreads.timeoutSeconds", 60L);

    public static final String COUNTER_PREFIX  = "c@";
    public static final String CHOICE_PREFIX = "i@";

    @Test
    public void testVersion() {
        assumeTrue(TestAssumptions.IS_YICES_INSTALLED);

        System.out.println("Loaded Yices version " + Yices.version());
        System.out.println("Yices version ordinal " + Yices.versionOrdinal());
        System.out.println("Built for " + Yices.buildArch());
        System.out.println("Build mode: " + Yices.buildMode());
        System.out.println("Build date: " + Yices.buildDate());
        System.out.println("MCSat supported: " + Yices.hasMcsat());
        System.out.println("Yices is thread safe: " + Yices.isThreadSafe());
        System.out.println();

        Assert.assertTrue(Yices.versionOrdinal() >= Yices.versionOrdinal(2, 6, 1));

    }

    private int namedVariable(int tau, String prefix, int suffix){
        return Terms.newUninterpretedTerm(prefix + suffix, tau);
    }


    private int makeConstraint(int index, int increment, int selector){
        int t1 = namedVariable(Types.INT, COUNTER_PREFIX, index);
        int t2 = namedVariable(Types.INT, COUNTER_PREFIX, index - 1);
        return Terms.and(selector, Terms.eq(t1, Terms.add(t2, Terms.intConst(increment))));
    }

    private void threadMain(int index, Status[] answers, CountDownLatch readyGate, CountDownLatch startGate) throws InterruptedException {
        readyGate.countDown();
        Assert.assertTrue("timed out waiting to start worker threads", startGate.await(THREAD_TIMEOUT_SECONDS, TimeUnit.SECONDS));

        try (Config cfg = new Config()) {
             cfg.set("solver-type", "dpllt");
             cfg.set("mode", "push-pop");
             try (Context context = new Context(cfg)) {
                 context.disableOption("var-elim");
                 int base = Terms.eq(namedVariable(Types.INT, COUNTER_PREFIX, 0), Terms.ZERO);
                 int lastIndex = 0;
                 int expected = 0;
                 int i = 1;
                 int selector = namedVariable(Types.BOOL, CHOICE_PREFIX, i);
                 int form1 = Terms.or(makeConstraint(i, 2, selector), makeConstraint(i, 2, Terms.not(selector)));
                 int form2 = Terms.or(makeConstraint(i + 1, 2, Terms.not(selector)), makeConstraint(i + 1, 2, selector));
                 lastIndex = i + 1;
                 expected += 5;
                 i += 2;
                 int selector2 = namedVariable(Types.BOOL, CHOICE_PREFIX, i);
                 int form3 = Terms.or(makeConstraint(i, 2, selector2), makeConstraint(i, 2, Terms.not(selector2)));
                 int form4 = Terms.or(makeConstraint(i + 1, 2, Terms.not(selector2)), makeConstraint(i + 1, 2, selector2));
                 lastIndex = i + 1;
                 expected += 5;
                 int form5 = Terms.arithGt(namedVariable(Types.INT, COUNTER_PREFIX, lastIndex), Terms.intConst(expected));
                 int[] allConstraints = { base, form1, form2, form3, form4, form5 };
                 answers[index]  = context.checkWithAssumptions(null, allConstraints);
                 System.out.println(String.format("context.checkWithAssumptions[%d] = %s", index, answers[index]));
             }
         }
    }

    private Thread makeThread(final int index, final Status[] answers, final CountDownLatch readyGate,
                              final CountDownLatch startGate, final AtomicReference<Throwable> failure){
        Runnable runnable = new Runnable(){
                public void run(){
                    try {
                        threadMain(index, answers, readyGate, startGate);
                    } catch (Throwable error) {
                        failure.compareAndSet(null, error);
                    }
                }
            };
            Thread thread = new Thread(runnable, "yices-test-thread-" + index);
            thread.setDaemon(true);
            return thread;
    }


    @Test(timeout = 120000)
    public void testThreads() {
        assumeTrue(TestAssumptions.IS_YICES_INSTALLED);
        assumeTrue(Yices.isThreadSafe());

        Thread[] threads = new Thread[THREAD_COUNT];
        Status[] answers = new Status[THREAD_COUNT];
        CountDownLatch readyGate = new CountDownLatch(THREAD_COUNT);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<Throwable>();

        for(int i = 0; i < THREAD_COUNT; i++){
            threads[i] = makeThread(i, answers, readyGate, startGate, failure);
        }

        for(int i = 0; i < THREAD_COUNT; i++){
            threads[i].start();
        }

        try {
            Assert.assertTrue("timed out waiting for worker threads to become ready",
                    readyGate.await(THREAD_TIMEOUT_SECONDS, TimeUnit.SECONDS));
            startGate.countDown();
            for(int i = 0; i < THREAD_COUNT; i++){
                threads[i].join(TimeUnit.SECONDS.toMillis(THREAD_TIMEOUT_SECONDS));
                Assert.assertFalse("worker thread did not finish: " + threads[i].getName(), threads[i].isAlive());
            }
        } catch (InterruptedException error){
            Thread.currentThread().interrupt();
            Assert.fail(error.getMessage());
        }

        if (failure.get() != null) {
            AssertionError error = new AssertionError("worker thread failed");
            error.initCause(failure.get());
            throw error;
        }

        for (int i = 0; i < THREAD_COUNT; i++){
            Assert.assertEquals(Status.SAT, answers[i]);
        }


    }


}
