package com.cuteforce.crossword;

import com.google.common.io.Resources;

import com.cuteforce.crossword.Crossword;

import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

public class TestCrossword {

    @Test
    public void testCrossword() throws IOException {
        int size = 7;
        Crossword cw = new Crossword(size, new File(Resources.getResource(TestCrossword.class, "lemmatization.txt").getFile()));
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
