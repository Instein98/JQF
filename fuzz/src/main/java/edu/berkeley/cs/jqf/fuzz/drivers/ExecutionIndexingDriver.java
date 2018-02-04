/*
 * Copyright (c) 2017, University of California, Berkeley
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package edu.berkeley.cs.jqf.fuzz.drivers;

import java.io.File;

import edu.berkeley.cs.jqf.fuzz.guidance.ExecutionIndexingGuidance;
import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;

/**
 * @author Rohan Padhye
 */
public class ExecutionIndexingDriver {

    public static void main(String[] args) {
        if (args.length < 2){
            System.err.println("Usage: java " + ExecutionIndexingDriver.class + " TEST_CLASS TEST_METHOD [MAX_TRIALS [SEEDS...]]");
            System.exit(1);
        }

        String testClassName  = args[0];
        String testMethodName = args[1];
        Long maxTrials = args.length > 2 ? Long.parseLong(args[2]) : Long.MAX_VALUE;
        File[] seedFiles = null;
        if (args.length > 3) {
            seedFiles = new File[args.length-3];
            for (int i = 3; i < args.length; i++) {
                seedFiles[i-3] = new File(args[i]);
            }
        }

        try {
            // Load the guidance
            ExecutionIndexingGuidance guidance = seedFiles != null ?
                    new ExecutionIndexingGuidance(maxTrials, seedFiles) :
                    new ExecutionIndexingGuidance(maxTrials);

            // Run the Junit test
            GuidedFuzzing.run(testClassName, testMethodName, guidance, System.out);
            if (Boolean.getBoolean("jqf.logCoverage")) {
                System.out.println(String.format("Covered %d edges.",
                        guidance.getTotalCoverage().getNonZeroCount()));
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }

    }
}
