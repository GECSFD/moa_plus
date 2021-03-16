//Vitor e Igor

package moa.classifiers.trees.iadem.SSL;

import java.util.ArrayList;

class Attribute {
    private String name;
    private String type;
    private int count;
    private double sum;
    private ArrayList values;
    private ArrayList appearances;
    private int appearancesCount;

    public Attribute(final String name, final String type, final int count, final double sum, ArrayList appearances) {
        this.name = name;
        this.type = type;
        this.count = count;
        this.sum = sum;
        this.values = new ArrayList<>();
        this.appearances = appearances;
        this.appearancesCount = 0;
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

    public int getCount() {
        return this.count;
    }

    public double getSum() {
        return this.sum;
    }

    public int getAppearancesCount() {
        return appearancesCount;
    }

    public void setAppearancesCount(int appearancesCount) {
        this.appearancesCount = appearancesCount;
    }

    public ArrayList getAppearances() {
        return appearances;
    }

    public ArrayList getValues() {
        return values;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSum(double sum) {
        this.sum = sum;
    }

    public void setValues(ArrayList values) {
        this.values = values;
    }

    public void setAppearances(ArrayList appearances) {
        this.appearances = appearances;
    }
}
