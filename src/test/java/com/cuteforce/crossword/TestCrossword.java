package com.cuteforce.crossword;

import com.google.common.io.Resources;

import com.cuteforce.crossword.Crossword;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

public class TestCrossword {

    @Test
    public void testCrossword() throws IOException {
        Crossword cw = new Crossword(4, 1000000, new File(Resources.getResource(TestCrossword.class, "lemmatization.txt").getFile()));
        String grid = cw.solve(true);
        Assert.assertEquals(grid, "sarsaloeroseseen");
        cw = new Crossword(5, 1000000, new File(Resources.getResource(TestCrossword.class, "lemmatization.txt").getFile()));
        grid = cw.solve(true);
        Assert.assertEquals(grid, "starstarotantueagutitosen");
    }


    @Test
    public void testgrid() throws IOException {
        Grid grid = new Grid(5, new Dictionary(5, new File(Resources.getResource(TestCrossword.class, "lemmatization.txt").getFile())));
        if (grid.solve() != null) {
            grid.prettyPrint();
        }
    }

    /**
     * Valid crossword:
     *
     * abstehst
     * bleirote
     * sellerie
     * tilsiter
     * ereifere
     * horteten
     * stierend
     * teerende
     */
    @Test
    public void testSolve() throws IOException {
        int size = 8;
        Crossword cw = new Crossword(size, 100000000, new File(Resources.getResource(TestCrossword.class, "lemmatization.txt").getFile()));
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
