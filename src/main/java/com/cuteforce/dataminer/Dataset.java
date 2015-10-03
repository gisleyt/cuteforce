package com.cuteforce.dataminer;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public class Dataset {

    public final Instances instances;
    public final ListMultimap<String, String> nominalValues;

    public enum Type {
        NOMINAL, FLOAT, IGNORE
    }

    private Dataset(Instances instances, ListMultimap<String, String> nominalValues) {
        this.instances = instances;
        this.nominalValues = nominalValues;
    }

    /**
     * Create Weka Instances and multimap of all nominal attributes for a dataset.
     *
     * @param files Dataset of comma-separated attributes
     * @param nameAndType List of entries containing attribute name and data type. Must be ordered similar to the data file.
     */
    public static Dataset create(List<String> files, List<Entry<String, Type>> nameAndType) throws IOException {
        ImmutableListMultimap<String, String> nominalAttributes = getNominalAttributesMultiMap(files, nameAndType);
        Instances instances = getEmptyInstances(nameAndType, nominalAttributes);
        insertInstances(files, nameAndType, instances);
        return new Dataset(instances, nominalAttributes);
    }

    /**
     * Insert attribute values, aka feature vector, for all lines in dataset.
     */
    private static void insertInstances(List<String> files, List<Entry<String, Type>> nameAndType, Instances instances) throws IOException {
        for (String filePath : files) {
            for (String line : Files.readAllLines(new File(filePath).toPath(), Charsets.UTF_8)) {
                List<String> attrList = Splitter.on(",").trimResults().splitToList(line);
                Preconditions.checkState(attrList.size() == nameAndType.size(), "Invalid input in line: " + line);
                Instance instance = new DenseInstance(instances.numAttributes());
                for (int i = 0; i < attrList.size(); i++) {
                    String attrValue = attrList.get(i);
                    Entry<String, Type> entry = nameAndType.get(i);
                    Attribute attribute = instances.attribute(entry.getKey());
                    if (entry.getValue() == Type.NOMINAL) {
                        instance.setValue(attribute, attrValue);
                    } else if (entry.getValue() == Type.FLOAT) {
                        instance.setValue(attribute, Double.parseDouble(attrValue));
                    }
                }
                instances.add(instance);
            }
        }
        instances.setClass(instances.attribute(Iterables.getLast(nameAndType).getKey()));
    }

    /**
     * Get the empty Instances template, including attribute information.
     */
    private static Instances getEmptyInstances(List<Entry<String, Type>> nameAndType, ImmutableListMultimap<String, String> nominalAttributes) {
        List<Attribute> attributes = Lists.newArrayList();
        for (Entry<String, Type> entry : nameAndType) {
            if (entry.getValue() == Type.FLOAT) {
                attributes.add(new Attribute(entry.getKey()));
            } else if (entry.getValue() == Type.NOMINAL) {
                attributes.add(new Attribute(entry.getKey(), nominalAttributes.get(entry.getKey())));
            }
        }
        Instances instances = new Instances("dataset", (ArrayList<Attribute>)  attributes, 0);
        return instances;
    }

    /**
     * Get all values for a nominal attribute.
     */
    private static ImmutableListMultimap<String, String> getNominalAttributesMultiMap(List<String> files, List<Entry<String, Type>> nameAndType) throws IOException {
        ImmutableSetMultimap.Builder<String, String> nominalAttributesBuilder = ImmutableSetMultimap.builder();
        for (String filePath : files) {
            for (String line : Files.readAllLines(new File(filePath).toPath(), Charsets.UTF_8)) {
                List<String> attrList = Splitter.on(",").trimResults().splitToList(line);
                Preconditions.checkState(attrList.size() == nameAndType.size(), "Invalid input in line: " + line);
                for (int i = 0; i < attrList.size(); i++) {
                    Entry<String, Type> attributeEntry = nameAndType.get(i);
                    if (attributeEntry.getValue() == Type.NOMINAL) {
                        nominalAttributesBuilder.put(nameAndType.get(i).getKey(), attrList.get(i));
                    }
                }
            }
        }

        SetMultimap<String, String> setMultiMap = nominalAttributesBuilder.build();
        ImmutableListMultimap.Builder<String, String> listMapBuilder = ImmutableListMultimap.builder();
        for (String key : setMultiMap.keySet()) {
            listMapBuilder.putAll(key, setMultiMap.get(key));
        }
        return listMapBuilder.build();
    }
}
