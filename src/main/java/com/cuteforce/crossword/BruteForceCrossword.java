package com.cuteforce.crossword;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class BruteForceCrossword {

    private int size;
    private Dictionary dictionary;
    private AtomicLong deadends = new AtomicLong();
    private AtomicBoolean foundSolution = new AtomicBoolean(false);
    private long start;

    public BruteForceCrossword(int size, File dictionary) throws IOException {
          this.size = size;
          this.dictionary = new Dictionary(size, dictionary);
    }

    public String solve(boolean debug) {
        this.start = System.currentTimeMillis();
        String solution = solve("", debug);
        System.err.println("Finished computing in " + (System.currentTimeMillis() - this.start) / 1000 + " seconds");
        return solution;
    }

    /**
     * Find a crossword of the given size.
     */
    private String solve(String letters, boolean debug) {
        if (this.foundSolution.get()) {
            return null;
        } else if (letters.length() < this.size * this.size) {
            List<Character> nextLetters = this.dictionary.getPlausibleNextChars(getHorizontal(letters), getVertical(letters));
            Stream<Character> stream = letters.length() == 0 ? nextLetters.parallelStream() : nextLetters.stream();
            Optional<String> solution = stream
                .map(letter -> solve(letters + letter, debug))
                .filter(letter -> letter != null)
                .findAny();
            if (solution.isPresent()) {
                this.foundSolution.set(true);
                return solution.get();
            } else {
                if (debug && this.deadends.incrementAndGet() % 10000000 == 0) {
                    long now = System.currentTimeMillis();
                    System.err.println("Total deadends " + deadends + ". " + (double) deadends.get() / ((now - start) / 1000.0) + " deadends per second.");
                    System.err.println("Failed path " + letters);
                }
                return null;
            }
        } else {
            return letters;
        }
    }

    private String getVertical(String letters) {
        int prefix = letters.length() % this.size;
        String vertical = "";
        int currentIdx = prefix;
        while (currentIdx < letters.length()) {
            vertical += letters.charAt(currentIdx);
            currentIdx += this.size;
        }
        return vertical;
    }

    private String getHorizontal(String letters) {
        return letters.substring(letters.length() - letters.length() % this.size);
    }

    public static void main(String[] args) throws IOException {
        int size = Integer.parseInt(args[1]);
        BruteForceCrossword cw = new BruteForceCrossword(size, new File(args[0]));
        String grid = cw.solve(true);
        if (grid != null) {
            for (int i = 0; i < grid.length(); i++) {
                if (i % size == 0) {
                    System.out.print("\n");
                }
                System.out.print(grid.charAt(i));
            }
            System.out.print("\n");
        } else {
            System.err.println("No solution found.");
        }
    }
}
