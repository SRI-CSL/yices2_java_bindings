package com.sri.yices;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assume.assumeTrue;

public class TestThreads {

    public static final int THREAD_COUNT = 10;

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

    private void threadMain(){
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
                 //Status status =




             }
         }
    }

    private Thread makeThread(){
        Runnable runnable = new Runnable(){
                public void run(){
                    threadMain();
                }
            };
            return new Thread(runnable);
    }


    @Test
    public void testThreads() {
        assumeTrue(TestAssumptions.IS_YICES_INSTALLED);
        assumeTrue(Yices.isThreadSafe());

        Thread[] threads = new Thread[THREAD_COUNT];

        for(int i = 0; i < THREAD_COUNT; i++){
            threads[i] = makeThread();
        }

        for(int i = 0; i < THREAD_COUNT; i++){
            threads[i].start();
        }

        try {
            for(int i = 0; i < THREAD_COUNT; i++){
                threads[i].join();
            }
        } catch (InterruptedException error){
            System.out.println(error.getMessage());
        }
    }


}
