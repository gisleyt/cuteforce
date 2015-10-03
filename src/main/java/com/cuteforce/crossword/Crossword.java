package com.cuteforce.crossword;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;

import com.cuteforce.crossword.Dictionary.Node;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Crossword {

    private int size;
    private Dictionary dictionary;

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

    public Crossword(int size, File dictionary) throws IOException {
        this.size = size;
        this.dictionary = new Dictionary(size, dictionary);
    }

    /**
     * Find a crossword of the given size.
     */
    public String solve() {
        Node failedPaths = new Node();
        String letters = "";
        while (letters.length() < this.size * this.size) {
            Character nextLetter = getNextLetter(letters, failedPaths);
            if (nextLetter == null) {
                failedPaths.inject(letters.toString());
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

    private Character getNextLetter(String letters, Node failedPaths) {
        int prefix = letters.length() % this.size;
        Map<Character, Double> horizontalProbs = this.dictionary.getProb(letters.substring(letters.length() - prefix), this.size);
        String vertical = "";
        int currentIdx = prefix;
        while (currentIdx < letters.length()) {
            vertical += letters.charAt(currentIdx);
            currentIdx += this.size;
        }

        Map<Character, Double> verticalProbs = this.dictionary.getProb(vertical, this.size);
        List<LetterProb> letterProbabilities = horizontalProbs.keySet()
            .stream()
            .map(letter -> new LetterProb(horizontalProbs.get(letter) * MoreObjects.firstNonNull(verticalProbs.get(letter), 0.0), letter))
            .filter(letterProb -> letterProb.probability > 0.0)
            .collect(Collectors.toList());

        Collections.sort(letterProbabilities, LetterProb.LETTERPROB_COMPARATOR);
        for (LetterProb lp : letterProbabilities) {
            if (failedPaths.getNode(letters + lp.character.toString()) == null) {
                return lp.character;
            }
        }
        return null;
    }
}
