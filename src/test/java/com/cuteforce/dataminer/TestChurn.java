package com.cuteforce.dataminer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import com.google.common.math.DoubleMath;

import com.cuteforce.dataminer.churn.AttributeStatistics;
import com.cuteforce.dataminer.Dataset.Type;


import org.testng.annotations.Test;

import weka.core.Attribute;
import weka.core.AttributeStats;
import weka.core.Instance;
import weka.core.converters.XRFFSaver;
import weka.experiment.Stats;

import java.io.File;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class TestChurn {

    private static Map<String, Type> typeMap = ImmutableMap.<String, Type>builder()
            .put("state", Type.NOMINAL)
            .put("accountLength", Type.FLOAT)
            .put("areaCode", Type.NOMINAL)
            .put("phoneNumber", Type.IGNORE)
            .put("internationalPlan",Type.NOMINAL)
            .put("voicemailplan", Type.NOMINAL)
            .put("numbervmailmessages", Type.FLOAT)
            .put("totaldayminutes",Type.FLOAT)
            .put("totalday calls", Type.FLOAT)
            .put("totaldaycharge", Type.FLOAT)
            .put("totaleveminutes", Type.FLOAT)
            .put("totalevecalls", Type.FLOAT)
            .put("totalevecharge", Type.FLOAT)
            .put("totalnightminutes", Type.FLOAT)
            .put("totalnightcalls", Type.FLOAT)
            .put("totalnightcharge", Type.FLOAT)
            .put("totalintlminutes", Type.FLOAT)
            .put("totalintlcalls", Type.FLOAT)
            .put("totalintlcharge", Type.FLOAT)
            .put("numbercustomerservicecalls", Type.FLOAT)
            .put("outcome", Type.NOMINAL)
            .build();

    @Test
    public void createWekaFiles() throws Exception {

        List<Entry<String, Type>> attributeTypes = Lists.newArrayList(typeMap.entrySet());
        Dataset train = Dataset.create(ImmutableList.of(Resources.getResource(TestChurn.class, "churn.data.txt").getFile()), attributeTypes);

        XRFFSaver saver = new XRFFSaver();
        saver.setFile(new File("/tmp/churn.data.xrff"));
        saver.setInstances(train.instances);
        saver.writeBatch();

        Dataset test = Dataset.create(ImmutableList.of(Resources.getResource(TestChurn.class, "churn.test.txt").getFile()), Lists.newArrayList(typeMap.entrySet()));
        saver = new XRFFSaver();
        saver.setFile(new File("/tmp/churn.test.xrff"));
        saver.setInstances(test.instances);
        saver.writeBatch();
    }

    @Test
    public void printChurnForAttributesAboveX() throws Exception {

        List<Entry<String, Type>> attributeTypes = Lists.newArrayList(typeMap.entrySet());
        Dataset train = Dataset.create(ImmutableList.of(Resources.getResource(TestChurn.class, "churn.data.txt").getFile()), attributeTypes);
        Map<String, AttributeStatistics> statistics = getAttributeStatistics(attributeTypes, train);

        for (AttributeStatistics stats : statistics.values()) {
            stats.printChurn(25.0);
        }
    }

    private Map<String, AttributeStatistics> getAttributeStatistics(List<Entry<String, Type>> attributeTypes, Dataset train) {
        Map<String, AttributeStatistics> statistics = Maps.newHashMap();
        attributeTypes = removeIgnored(attributeTypes);

        // Initialize maps.
        for (int i = 0; i < train.instances.numAttributes(); i++) {
            AttributeStats stats = train.instances.attributeStats(i);
            if (attributeTypes.get(i).getValue() == Type.FLOAT) {
                Stats numStats = stats.numericStats;
                double currentInterval = numStats.min;
                Map<Object, Integer> trueFrequency = Maps.newHashMap();
                Map<Object, Integer> falseFrequency = Maps.newHashMap();
                while (currentInterval <= numStats.max) {
                    trueFrequency.put(DoubleMath.roundToInt(currentInterval, RoundingMode.CEILING), 0);
                    falseFrequency.put(DoubleMath.roundToInt(currentInterval, RoundingMode.CEILING), 0);
                    // Use std dev as interval steps.
                    currentInterval += numStats.stdDev;
                }
                statistics.put(attributeTypes.get(i).getKey(), new AttributeStatistics(attributeTypes.get(i).getKey(), trueFrequency, falseFrequency, Type.FLOAT));
            } else if (attributeTypes.get(i).getValue() == Type.NOMINAL) {
                statistics.put(attributeTypes.get(i).getKey(), new AttributeStatistics(attributeTypes.get(i).getKey(), Maps.<Object, Integer>newHashMap(), Maps.<Object, Integer>newHashMap(), Type.NOMINAL));
            }
        }

        // Update statistics.
        for (Instance instance : train.instances) {
            for (int i = 0; i < train.instances.numAttributes(); i++) {
                Attribute attribute = instance.attribute(i);
                AttributeStatistics attStatistics = statistics.get(attribute.name());
                attStatistics.update(instance.toString(attribute), instance.toString(train.instances.classAttribute()).toLowerCase().startsWith("true"));
            }
        }
        return statistics;
    }

    private List<Entry<String, Type>> removeIgnored(List<Entry<String, Type>> attributeTypes) {
        ImmutableList.Builder<Entry<String, Type>> builder = ImmutableList.builder();
        for (Entry<String, Type> entry : attributeTypes) {
            if (entry.getValue() != Type.IGNORE) {
                builder.add(entry);
            }
        }
        return builder.build();
    }
}
