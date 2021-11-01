package edu.berkeley.cs.jqf.fuzz.junit.javabytecode;

import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomInputStream {
    private static final int MAX_SIZE = 10240;
    private static final int RANDOM_RANGE = 256;
    public List<Integer> randomList = new ArrayList<>();
    private Random random = new Random();
    public RandomInputStream() {
        for (int i = 0; i < MAX_SIZE; i ++) {
            randomList.add(random.nextInt(RANDOM_RANGE));
        }
    }
    public RandomInputStream(List<Integer> current) {
        randomList = current;
    }

    public RandomInputStream fuzz(Random other) {
        List<Integer> newInput = new ArrayList<>();
        for (Integer integer : randomList) {
            newInput.add(integer);
        }
        int numMutations = ZestGuidance.Input.sampleGeometric(other, 8.0);
        boolean setToZero = other.nextDouble() < 0.1;
        for (int mutation = 1; mutation <= numMutations; mutation++) {

            // Select a random offset and size
            int offset = other.nextInt(newInput.size());
            int mutationSize = ZestGuidance.Input.sampleGeometric(other, 8.0);

            // desc += String.format(":%d@%d", mutationSize, idx);

            // Mutate a contiguous set of bytes from offset
            for (int i = offset; i < offset + mutationSize; i++) {
                // Don't go past end of list
                if (i >= newInput.size()) {
                    break;
                }

                // Otherwise, apply a random mutation
                int mutatedValue = setToZero ? 0 : other.nextInt(RANDOM_RANGE);
                newInput.set(i, mutatedValue);
            }
        }
        return new RandomInputStream(newInput);
    }

    public InputStream getRawRandomStream() {
        // Return an input stream that reads bytes from a linear array
        return new InputStream() {
            int bytesRead = 0;

            @Override
            public int read() throws IOException {

                // Attempt to get a value from the list, or else generate a random value
                if (bytesRead >= randomList.size()) {
                    return -1;
                }
                return randomList.get(bytesRead ++);
            }
        };
    }

}
