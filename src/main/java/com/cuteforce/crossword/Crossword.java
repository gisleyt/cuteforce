package com.cuteforce.crossword;

import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

public class Crossword {

    private int size;
    private Dictionary dictionary;
    private long bloomfiltersize;

    public static class LetterProb {
        public final double probability;
        public final Character character;

        public LetterProb(double probability, Character character) {
            this.probability = probability;
            this.character = character;
        }

        public static final Comparator<LetterProb> LETTERPROB_COMPARATOR = new Comparator<LetterProb>() {
            @Override
            public int compare(LetterProb l1, LetterProb l2) {
                return ComparisonChain.start()
                        .compare(l2.probability, l1.probability)
                        .compare(l1.character, l2.character)
                        .result();
            }
        };
    }

    public Crossword(int size, long bloomfilterSize,  File dictionary) throws IOException {
        this.size = size;
        this.dictionary = new Dictionary(size, dictionary);
        this.bloomfiltersize = bloomfilterSize;
    }

    /**
     * Find a crossword of the given size.
     */
    public String solve(boolean debug) {
        BloomFilter<String> failedPaths = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_8), this.bloomfiltersize, 0.002);
        String letters = "";
        int backtracks = 0;
        long start = System.currentTimeMillis();
        while (letters.length() < this.size * this.size) {
            Character nextLetter = getNextLetter(letters, failedPaths);
            if (nextLetter == null) {
                if (debug && ++backtracks % 100000 == 0) {
                    long now = System.currentTimeMillis();
                    System.err.println("Total backtracks " + backtracks + ". " + (double) backtracks / ((now - start) / 1000.0) + " backtracks per second.");
                    System.err.println("Failed path " + letters);
                }
                failedPaths.put(letters.toString());
                if (letters.length() > 0) {
                    letters = letters.substring(0, letters.length() - 1);
                    continue;
                } else {
                    return null;
                }
            } else {
                letters += nextLetter;
            }
        }
        return letters;
    }

    private Character getNextLetter(final String letters, BloomFilter<String> failedPaths) {
        int prefix = letters.length() % this.size;
        Map<Character, Double> horizontalProbs = this.dictionary.getProb(letters.substring(letters.length() - prefix));
        String vertical = "";
        int currentIdx = prefix;
        while (currentIdx < letters.length()) {
            vertical += letters.charAt(currentIdx);
            currentIdx += this.size;
        }

        Map<Character, Double> verticalProbs = this.dictionary.getProb(vertical);
        Optional<LetterProb> letterIfFound = horizontalProbs.keySet()
            .stream()
            .map(letter -> new LetterProb(horizontalProbs.get(letter) * MoreObjects.firstNonNull(verticalProbs.get(letter), 0.0), letter))
            .filter(letterProb -> letterProb.probability > 0.0)
            .sorted(LetterProb.LETTERPROB_COMPARATOR)
            .filter(letterProb -> !failedPaths.mightContain(letters + letterProb.character))
            .findFirst();
        return (letterIfFound.isPresent() ? letterIfFound.get().character : null);
    }

    public static void main(String[] args) throws IOException {
        int size = Integer.parseInt(args[1]);
        long bloomfilterSize = Long.parseLong(args[2]);
        Crossword cw = new Crossword(size, bloomfilterSize, new File(args[0]));
        String grid = cw.solve(true);
        for (int i = 0; i < grid.length(); i++) {
            if (i % size == 0) {
                System.out.print("\n");
            }
            System.out.print(grid.charAt(i));
        }
        System.out.print("\n");
    }
}
