package org.kanonizo.algorithms.faultprediction;

import java.util.List;

public class SchwaClass {
    private int authors;
    private List<SchwaMethod> children;
    private String name;
    private int fixes;
    private double prob;
    private double size;
    private String type;
    private int revisions;
    private String path;

    public int getAuthors() {
        return authors;
    }

    public List<SchwaMethod> getChildren() {
        return children;
    }

    public String getName() {
        return name;
    }

    public int getFixes() {
        return fixes;
    }

    public double getProb() {
        return prob;
    }

    public double getSize() {
        return size;
    }

    public String getType() {
        return type;
    }

    public int getRevisions() {
        return revisions;
    }

    public String getPath() {
        return path;
    }
}


