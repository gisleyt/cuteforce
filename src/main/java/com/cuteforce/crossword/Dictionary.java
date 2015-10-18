package com.cuteforce.crossword;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

        /**
         * Root node.
         */
        public Node() {
            this.letter = null;
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
                return (daughterNode == null) ? 0 : daughterNode.getFrequency(prefix.substring(1));
            }
        }

        public Node getNode(String prefix) {
            Node daughter = this.daughters.get(prefix.charAt(0));
            prefix = prefix.substring(1);
            return  (prefix.isEmpty() || daughter == null) ? daughter : daughter.getNode(prefix);
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
        this.root = new Node();
        Files.lines(dictionaryName.toPath(), Charsets.UTF_8)
            .flatMap(Pattern.compile("\\.")::splitAsStream)
            .map(String::toLowerCase)
            .distinct()
            .filter(word -> word.length() == size)
            .forEach(word -> this.root.inject(word));
    }

    public Map<Character, Double> getProb(String prefix) {
        Node node = prefix.equals("") ? this.root : this.root.getNode(prefix);
        if (node != null) {
            double totalFreq = node.daughters.entrySet()
                    .stream()
                    .mapToInt(entry -> entry.getValue().daughterFreq.get())
                    .sum();

            return node.daughters.entrySet()
                .stream()
                .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().daughterFreq.get() / totalFreq));
        }
        return ImmutableMap.of();
    }
}
