package com.cuteforce.crossword;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import com.cuteforce.crossword.Crossword.LetterProb;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Grid {

    private final int size;
    private boolean moveHorizontal = true;
    private int stepsBeforeTurning = 0;
    private int h = 0;
    private int v = 0;
    private List<List<Character>> rows;
    private BloomFilter<String> failedPaths = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_8), 10000, 0.02);
    private Dictionary dictionary;

    public Grid(int size, Dictionary dictionary) {
        this.size = size;
        this.rows = Lists.newArrayListWithCapacity(size);
        for (int i = 0; i < size; i++) {
            List<Character> row = Lists.newArrayListWithCapacity(size);
            for (int j = 0; j < size; j++) {
                row.add('#');
            }
            this.rows.add(row);
        }
        this.dictionary = dictionary;
    }

    public void prettyPrint() {
        this.rows.stream()
            .forEach(list -> System.err.println(Joiner.on("").join(list)));
    }

    public String solve() {
        while (this.h < this.size && this.v < this.size) {
            Character next = getNext();
            if (next == null) {
                this.failedPaths.put(getCurrentLetters());
                if (backtrack() == false) {
                    return null;
                }
            } else {
                inject(next);
                if (!advance()) {
                    return getCurrentLetters();
                }
            }
        }
        return null;
    }

    private void inject(Character next) {
        this.rows.get(this.v).set(this.h, next);
    }

    private boolean advance() {
        if (this.moveHorizontal) {
            if (this.h < this.stepsBeforeTurning) {
                this.h++;
            } else {
                this.v = 0;
                this.h = this.stepsBeforeTurning + 1;
                this.moveHorizontal = false;
            }
        } else {
            if (this.v < this.stepsBeforeTurning) {
                this.v++;
            } else {
                this.v++;
                this.h = 0;
                this.stepsBeforeTurning++;
                this.moveHorizontal = true;
            }
        }
        return (this.h < this.size && this.v < this.size);
    }

    private boolean backtrack() {
        if (this.moveHorizontal) {
            if (this.h > 0) {
                this.h--;
            } else {
                this.h = this.stepsBeforeTurning;
                this.v--;
                this.moveHorizontal = false;
                this.stepsBeforeTurning--;
            }
        } else {
            if (this.v > 0) {
                this.v--;
            } else {
                this.v = this.stepsBeforeTurning;
                this.h = this.v;
                moveHorizontal = true;
            }
        }
        if (this.h >= 0 && this.v >= 0) {
            inject('#');
            return true;
        } else {
            return false;
        }
    }

    private String getCurrentLetters() {
        List<Character> letters = Lists.newArrayList();
        this.rows.stream()
            .forEach(list -> letters.addAll(list));
        return Joiner.on("").join(letters);
    }

    private Character getNext() {
        List<Character> row = this.rows.get(this.v);
        Map<Character, Double> horizontalProbs = this.dictionary.getProb(Joiner.on("").join(row.subList(0, h)), this.size);
        String vPrefix = this.rows.stream()
                .limit(this.v)
                .map(list -> list.get(this.h).toString())
                .reduce("", (first, second) -> first + second);
        Map<Character, Double> verticalProbs = this.dictionary.getProb(vPrefix, this.size);

        List<LetterProb> letterProbabilities = horizontalProbs.keySet()
                .stream()
                .map(letter -> new LetterProb(horizontalProbs.get(letter) * MoreObjects.firstNonNull(verticalProbs.get(letter), 0.0), letter))
                .filter(letterProb -> letterProb.probability > 0.0)
                .collect(Collectors.toList());

        Collections.sort(letterProbabilities, LetterProb.LETTERPROB_COMPARATOR);
        for (LetterProb lp : letterProbabilities) {
            inject(lp.character);
            String currentLetters = getCurrentLetters();
            inject('#');
            if (!this.failedPaths.mightContain(currentLetters + lp.character.toString())) {
                return lp.character;
            }
        }
        return null;
    }
}
