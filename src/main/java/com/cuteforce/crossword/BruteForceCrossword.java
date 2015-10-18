package com.cuteforce.crossword;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class BruteForceCrossword {

    private int size;
    private Dictionary dictionary;
    private int backtracks = 0;
    private long start;

    public BruteForceCrossword(int size, File dictionary) throws IOException {
          this.size = size;
          this.dictionary = new Dictionary(size, dictionary);
    }

    public String solve(boolean debug) {
        this.start = System.currentTimeMillis();
        return solve("", debug);
    }

    /**
     * Find a crossword of the given size.
     */
    private String solve(String letters, boolean debug) {
        if (letters.length() < this.size * this.size) {
            List<Character> nextLetters = this.dictionary.getPlausibleNextChars(getHorizontal(letters), getVertical(letters));
            Optional<String> solution = nextLetters.stream()
                .map(letter -> solve(letters + letter, debug))
                .filter(letter -> letter != null)
                .findFirst();
                if (solution.isPresent()) {
                    return solution.get();
                } else {
                    if (debug && ++this.backtracks % 1000000 == 0) {
                        long now = System.currentTimeMillis();
                        System.err.println("Total backtracks " + backtracks + ". " + (double) backtracks / ((now - start) / 1000.0) + " backtracks per second.");
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
