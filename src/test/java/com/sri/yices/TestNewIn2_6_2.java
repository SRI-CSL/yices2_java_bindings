package com.sri.yices;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assume.assumeTrue;

public class TestNewIn2_6_2 {
    @Test
    public void testLoad() {
        assumeTrue(TestAssumptions.IS_YICES_INSTALLED);

        System.out.println("Loaded Yices version " + Yices.version());
        System.out.println("Built for " + Yices.buildArch());
        System.out.println("Build mode: " + Yices.buildMode());
        System.out.println("Build date: " + Yices.buildDate());
        System.out.println("MCSat supported: " + Yices.hasMcsat());
        Yices.resetError();
        System.out.println("Yices error: " + Yices.errorString());

        System.out.println("Has cadical as a delegate: " + Yices.hasDelegate("cadical"));
        System.out.println("Has cryptominisat as a delegate: " + Yices.hasDelegate("cryptominisat"));
        System.out.println("Has y2sat as a delegate: " + Yices.hasDelegate("y2sat"));
        System.out.println();
    }




}
