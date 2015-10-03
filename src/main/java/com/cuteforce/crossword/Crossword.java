package com.cuteforce.crossword;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class Crossword {

    private int size;
    private Dictionary dictionary;

    public Crossword(int size, File dictionary) throws IOException {
        this.size = size;
        this.dictionary = new Dictionary(size, dictionary);
    }

    /**
     * Find a crossword of the given size.
     */
    public String solve() {
        Set<String> failedPaths = Sets.newHashSet();
        String letters = "";
        while (letters.length() < this.size * this.size) {
            Character nextLetter = getNextLetter(letters, failedPaths);
            if (nextLetter == null) {
                Preconditions.checkState(failedPaths.add(letters.toString()));
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

    private Character getNextLetter(String letters, Set<String> failedPaths) {
        int prefix = letters.length() % this.size;
        Map<Character, Double> horizontalProbs = this.dictionary.getProb(letters.substring(letters.length() - prefix), this.size);
        String vertical = "";
        int currentIdx = prefix;
        while (currentIdx < letters.length()) {
            vertical += letters.charAt(currentIdx);
            currentIdx += this.size;
        }

        Map<Character, Double> verticalProbs = this.dictionary.getProb(vertical, this.size);
        double maxProb = 0.0;
        Character maxLetter = null;
        for (Character letter : horizontalProbs.keySet()) {
            double prob = horizontalProbs.get(letter) * MoreObjects.firstNonNull(verticalProbs.get(letter), 0.0);
            if (maxProb < prob && !failedPaths.contains(letters + letter.toString())) {
                maxLetter = letter;
                maxProb = prob;
            }
        }
        return maxLetter;
    }
}
