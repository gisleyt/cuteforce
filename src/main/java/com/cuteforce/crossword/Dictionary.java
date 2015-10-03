package com.cuteforce.crossword;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class Dictionary {

    public static class Node {
        public final AtomicInteger daughterFreq;
        public final Map<Character, Node> daughters;
        public final Character letter;

        public Node(Character letter) {
            this.letter = letter;
            this.daughters = Maps.newHashMap();
            this.daughterFreq = new AtomicInteger(1);
        }

        public void inject(String suffix) {
            this.daughterFreq.incrementAndGet();
            if (suffix.isEmpty()) {
                return;
            } else {
                Character next = suffix.charAt(0);
                String tail = suffix.substring(1);
                Node daughterNode = this.daughters.get(next);
                if (daughterNode == null) {
                    daughterNode = new Node(next);
                    this.daughters.put(next, daughterNode);
                }
                daughterNode.inject(tail);
            }
        }

        public int getFrequency(String prefix) {
            if (prefix.isEmpty()) {
                return this.daughterFreq.get();
            } else {
                Node daughterNode = this.daughters.get(prefix.charAt(0));
                if (daughterNode == null) {
                    return 0;
                } else {
                    return daughterNode.getFrequency(prefix.substring(1));
                }
            }
        }

        public Node getNode(String prefix) {
            Node daughter = this.daughters.get(prefix.charAt(0));
            prefix = prefix.substring(1);
            if (prefix.isEmpty()) {
                return daughter;
            } else {
                return daughter.getNode(prefix);
            }
        }
    }

    private Node root;

    /**
     * Assumes a dictionary file of type
     * cars.car
     * men.man
     * etc
     */
    public Dictionary(int size, File dictionaryName) throws IOException {
        this.root = new Node('!');
        Files.lines(dictionaryName.toPath(), Charsets.UTF_8)
            .flatMap(Pattern.compile("\\.")::splitAsStream)
            .map(String::toLowerCase)
            .distinct()
            .filter(word -> word.length() == size)
            .forEach(word -> this.root.inject(word));
    }

    public Map<Character, Double> getProb(String prefix, int length) {
        Map<Character, Double> probMap = Maps.newHashMap();
        Node node;
        if (prefix.equals("")) {
            node = this.root;
        } else {
            node = this.root.getNode(prefix);
        }
        if (node != null) {
            double totalFreq = node.daughters.entrySet()
                    .stream()
                    .map(entry -> entry.getValue().daughterFreq.get())
                    .reduce(0, (first, second) -> first + second);

            node.daughters.entrySet()
            .stream()
            .forEach(entry -> probMap.put(entry.getKey(), entry.getValue().daughterFreq.get() / totalFreq));
        }
        return probMap;
    }
}