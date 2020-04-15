package com.sri.yices;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assume.assumeTrue;

public class TestVersion {
    @Test
    public void testVersion() {
        assumeTrue(TestAssumptions.IS_YICES_INSTALLED);
        
        System.out.println("Loaded Yices version " + Yices.version());
        System.out.println("Yices version ordinal " + Yices.versionOrdinal());
        System.out.println("Built for " + Yices.buildArch());
        System.out.println("Build mode: " + Yices.buildMode());
        System.out.println("Build date: " + Yices.buildDate());
        System.out.println("MCSat supported: " + Yices.hasMcsat());
        System.out.println();
    }
}
