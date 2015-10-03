package com.cuteforce.dataminer.churn;


import com.google.common.base.MoreObjects;

import com.cuteforce.dataminer.Dataset.Type;

import java.util.Map;
import java.util.Map.Entry;

public class AttributeStatistics {

    public final String attributeName;
    public final Type type;
    public final Map<Object, Integer> frequenciesFalse;
    public final Map<Object, Integer> frequenciesTrue;
    private int totalTrue;
    private int totalFalse;

    public AttributeStatistics(String name, Map<Object, Integer> trueFrequency, Map<Object, Integer> falseFrequency, Type type) {
        this.attributeName = name;
        this.frequenciesFalse = falseFrequency;
        this.frequenciesTrue = trueFrequency;
        this.type = type;
        this.totalTrue = 0;
        this.totalFalse = 0;
    }

    public void update(String value, boolean isTrue) {
        if (isTrue) {
            this.totalTrue++;
        } else {
            this.totalFalse++;
        }
        if (this.type == Type.FLOAT) {
            double valueFloat = Double.parseDouble(value);
            if (isTrue) {
                updateFrequencyMap(this.frequenciesTrue, valueFloat);
            } else {
                updateFrequencyMap(this.frequenciesFalse, valueFloat);
            }
        } else {
            if (isTrue) {
                Integer currentFreq = MoreObjects.firstNonNull(this.frequenciesTrue.get(value), 0);
                this.frequenciesTrue.put(value, ++currentFreq);
            } else {
                Integer currentFreq = MoreObjects.firstNonNull(this.frequenciesFalse.get(value), 0);
                this.frequenciesFalse.put(value, ++currentFreq);
            }
        }
    }

    private void updateFrequencyMap(Map<Object, Integer> frequencyMap, double valueFloat) {
        for (Entry<Object, Integer> entry : frequencyMap.entrySet()) {
            Integer interval = (Integer) entry.getKey();
            if (valueFloat > interval) {
                frequencyMap.put(interval, frequencyMap.get(entry.getKey()) + 1);
            }
        }
    }

    public void printChurn(double minimumChurn) {
        for (Entry<Object, Integer> entry : this.frequenciesTrue.entrySet()) {
            Integer falseFrequency = MoreObjects.firstNonNull(this.frequenciesFalse.get(entry.getKey()), 0);
            int totaltNumberInGroup = entry.getValue() + falseFrequency;
            double churn = (double) entry.getValue() / (totaltNumberInGroup) * 100;
            if (churn >= minimumChurn) {
                System.err.println(this.attributeName + (this.type == Type.FLOAT ?  " more than " : " ")
                        + entry.getKey() + " has churn " + churn + " amoung customers in total: " + totaltNumberInGroup
                        + " / " + (double) totaltNumberInGroup / (this.totalFalse + this.totalTrue) * 100 + "%");
            }
        }
    }
}
