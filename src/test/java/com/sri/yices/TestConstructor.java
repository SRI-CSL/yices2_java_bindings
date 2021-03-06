package com.sri.yices;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assume.assumeTrue;

public class TestConstructor {
    @Test
    public void testConstructors() {
        // JUnit runner treats tests with failing assumptions as ignored
        assumeTrue(TestAssumptions.IS_YICES_INSTALLED);

        for (int i = 0; i<Constructor.NUM_CONSTRUCTORS; i++) {
            Constructor c = Constructor.idToConstructor(i);
            Assert.assertEquals(c.getIndex(), i);
            System.out.println("index " + i + ": constructor = " + c);
        }
        Constructor error = Constructor.idToConstructor(1000);
        Assert.assertEquals(error, Constructor.CONSTRUCTOR_ERROR);
        System.out.println("index 1000: constructor = " + error);

        error = Constructor.idToConstructor(-20);
        Assert.assertEquals(error, Constructor.CONSTRUCTOR_ERROR);
        System.out.println("index 1000: constructor = " + error);
    }
}
