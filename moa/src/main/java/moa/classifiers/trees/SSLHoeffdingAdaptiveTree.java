/*
 *    SSLHoeffdingAdaptiveTree.java
 *    Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 *    @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package moa.classifiers.trees;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.classifiers.bayes.NaiveBayes;
import moa.classifiers.core.conditionaltests.InstanceConditionalTest;
import moa.classifiers.core.driftdetection.ADWIN;
import moa.classifiers.trees.iadem.SSL.*;
import moa.core.DoubleVector;
import moa.core.MiscUtils;
import moa.core.Utils;

//import java.io.File;
//import java.io.FileWriter;
//import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Hoeffding Adaptive Tree for evolving data streams.
 *
 * <p>This adaptive Hoeffding Tree uses ADWIN to monitor performance of
 * branches on the tree and to replace them with new branches when their
 * accuracy decreases if the new branches are more accurate.</p>
 * See details in:</p>
 * <p>Adaptive Learning from Evolving Data Streams. Albert Bifet, Ricard Gavaldà.
 * IDA 2009</p>
 *
 * <ul>
 * <li> Same parameters as <code>HoeffdingTreeNBAdaptive</code></li>
 * <li> -l : Leaf prediction to use: MajorityClass (MC), Naive Bayes (NB) or NaiveBayes
 * adaptive (NBAdaptive).
 * </ul>
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */



public class SSLHoeffdingAdaptiveTree extends HoeffdingTree {

    private static final long serialVersionUID = 1L;
    protected int alternateTrees;

    // KENNY
    // Made classesDistribution and totallabeled public
    int totalLabeled;
    int totalUnlabeled;
    ArrayList classesDistribution = new ArrayList();
    ArrayList<Attribute> attributes = new ArrayList<Attribute>();

    int correct = 0;
    int wrong = 0;
    int total = 0;

    ArrayList trueClass = new ArrayList();
    ArrayList predictedClass = new ArrayList();

    int c0 = 0;
    int c1 = 0;

    boolean rcEnabled = true; // FALSE

    double removeChance = 0.1;  // O.9

    int unlabeledCounter = 0;

//    Choose if remove classes is ative from the GUI.
    public MultiChoiceOption rcChooser = new MultiChoiceOption(
            "RC",
            'R',
            "If is true remove some classes to semi-supervised learning",
            new String[]{"True", "False"},
            new String[]{"true", "false"},
            0);

//    Choose the chance of removing a class from the GUI
    public FloatOption removeChanceChooser = new FloatOption(
            "removeChance",
            'C',
            "Chance of removing a class of a instance, for semi-supervised learning," +
                    " only works when RC is true",
            0.1,
            0.0,
            1.0);
    public FloatOption levaticWeight = new FloatOption(
            "levaticWeight",
            'W',
            "Levatic's  weigth used in levatic's metric",
            0.5,
            0.0,
            1.0);



    /*   public MultiChoiceOption leafpredictionOption = new MultiChoiceOption(
            "leafprediction", 'l', "Leaf prediction to use.", new String[]{
                "MC", "NB", "NBAdaptive"}, new String[]{
                "Majority class",
                "Naive Bayes",
                "Naive Bayes Adaptive"}, 2);*/
    protected int prunedAlternateTrees;
    protected int switchedAlternateTrees;
    private Object IOException;

    @Override
    public String getPurposeString() {
        return "Hoeffding Adaptive Tree for evolving data streams that uses ADWIN to replace branches for new ones.";
    }

    @Override
    protected LearningNode newLearningNode(double[] initialClassObservations) {
        // IDEA: to choose different learning nodes depending on predictionOption
        return new AdaLearningNode(initialClassObservations);
    }

    @Override
    protected SplitNode newSplitNode(InstanceConditionalTest splitTest,
                                     double[] classObservations, int size) {
        return new AdaSplitNode(splitTest, classObservations, size);
    }

    @Override
    protected SplitNode newSplitNode(InstanceConditionalTest splitTest,
                                     double[] classObservations) {
        return new AdaSplitNode(splitTest, classObservations);
    }

    public int getTotalLabeled() {
        return totalLabeled;
    }

    public void setTotalLabeled(int totalLabeled) {
        this.totalLabeled = totalLabeled;
    }

    public ArrayList getClassesDistribution() {
        return classesDistribution;
    }

    public void setClassesDistribution(ArrayList classesDistribution) {
        this.classesDistribution = classesDistribution;
    }

    public ArrayList<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(ArrayList<Attribute> attributes) {
        this.attributes = attributes;
    }


    // KENNY + VITOR e IGOR
    public boolean validateClassIsMissing(Instance inst) {
        double classPred = inst.classValue();

        return Double.isNaN(classPred);
    }

    // KENNY
    public void trainOnInstanceImplWithoutClass(Instance inst) {
        if (this.treeRoot == null) {
            this.treeRoot = newLearningNode();
            this.activeLeafNodeCount = 1;
        }

        this.totalUnlabeled++;

        ((NewNode) this.treeRoot).learnFromInstance(inst, this, null, -1);
    }

    // KENNY
    public static double entropy(ArrayList classesDistribution, int totalAppearances) {
        int numClasses = (int) classesDistribution.size();
        double entropy = 0.0;
        double probability = 0.0;
        double log = 0.0;

        for (int i = 0; i < numClasses; i++) {
            int distribution = (int) classesDistribution.get(i);

            probability = (double) distribution / totalAppearances;
            log = Math.log(probability);

            entropy -= probability * log;
        }

        return entropy;
    }

    // KENNY
    public double gini(ArrayList classesDistribution, int totalAppearances) {

        int numClasses = (int) classesDistribution.size();
        double summation = 0.0;
        double probability = 0.0;

        for (int i = 0; i < numClasses; i++) {
            int distribution = (int) classesDistribution.get(i);
            if (totalAppearances != 0) {
                probability = (double) distribution / totalAppearances;
                probability = probability * probability;
            } else {
                probability = 0;
            }

            summation += probability;
        }

        return (1 - summation);
    }

    public double variance(ArrayList oldData, double sum, int totalCount) {
        double variance = 0.0;
        double rightSide = (sum * sum) / totalCount;
        double leftSide = 0.0;

        for (int j = 0; j < oldData.size(); j++) {
            double val = (double) oldData.get(j);

            leftSide = leftSide + (val * val);
        }

        variance = (leftSide - rightSide) / totalCount;

        return variance;
    }

    public double impurity(ArrayList classesDistribution, ArrayList<Attribute> attributes) {
        double impurity = 0.0;
        double w = levaticWeight.getValue(); // original = 0.5

        int totalLabeled = 0;


        for (int i = 0; i < classesDistribution.size(); i++) {
            totalLabeled += (int) classesDistribution.get(i);
        }

        {/*double entropyLabeled = this.entropy(classesDistribution, totalLabeled);
        //double entropyLabeledTraining = this.entropy(this.classesDistribution, this.totalLabeled);
        double supervised = w * entropyLabeled;*/}



        double giniLabeled = this.gini(classesDistribution, totalLabeled); //supostamente apenas rotulados
        double giniLabeledTraining = this.gini(this.classesDistribution, this.totalLabeled); // conjunto total

        //E1

        double supervised = w * (giniLabeled / giniLabeledTraining);

        int numAttributes = attributes.size();
        double semisupervised = 0.0;
        double sslimpurity = 0.0;


        for (int i = 0; i < numAttributes; i++) {
            Attribute att = (Attribute) attributes.get(i);
            Attribute treeAttribute = (Attribute) this.getAttributes().get(i);

            //Eu
            if (att.getType() == "nominal") {
                double giniNode = this.gini(att.getAppearances(), att.getAppearancesCount());
                double giniTree = this.gini(treeAttribute.getAppearances(), treeAttribute.getAppearancesCount());

                sslimpurity += giniNode/giniTree;
            } else if (att.getType() == "numeric") {
                double varianceNode = this.variance(att.getValues(), att.getSum(), att.getCount());
                double varianceTree = this.variance(treeAttribute.getValues(), treeAttribute.getSum(), treeAttribute.getCount());

                sslimpurity += varianceNode/varianceTree;
            } else {
                continue;
            }
        }

        semisupervised = ((1-w) / numAttributes) * sslimpurity;
        impurity = supervised + semisupervised;

        //Comentada
//        return impurity >= 0.8 ? Math.abs(0.97 - impurity) : impurity;

        return impurity;
    }



    /* ADICIONAMOS A CLASSE RC, QUE FAZ A REMOCAO DA CLASSE A PARTIR DO setClassMissing().
    utilizava Double.NaN. Alteramos o metodo getInstanceWithRemovedClass original de RC para lidar com apenas 1 instancia
    por vez. A verificacao da probabilidade de remocao tbm foi levada para o metodo.
    * */

    // KENNY + VITOR E IGOR
    @Override
    public void trainOnInstanceImpl(Instance inst) {
        Instance newInstance = inst.copy();
        SSLUtils SSLUtils = new SSLUtils();
        this.rcEnabled = this.rcChooser.getChosenIndex() == 0;
        this.removeChance = this.removeChanceChooser.getValue();


//        if(this.rcEnabled && random.nextFloat() < removeChance) {
////            newInstance.setClassValue(newInstance.classIndex(), Double.NaN);
////            unlabeledCounter++;
////        }

        if (this.rcEnabled) {
            newInstance = SSLUtils.getInstanceWithRemovedClass(newInstance,removeChance);
            if(this.validateClassIsMissing(newInstance)){
                this.unlabeledCounter++;
            }
        }

        this.total++;

        int numAttributes = inst.numAttributes() - 1;

        // instanciando os atributos
        if (this.classesDistribution.size() == 0) {
            List classes = newInstance.classAttribute().getAttributeValues(); // pega a classe

            classes.forEach(item-> {
                this.classesDistribution.add((int) 0);
            });

            // ARRAY COM O NUMERO DE CLASSES DA INSTANCIA

            for (int i = 0; i < numAttributes; i++) {
                com.yahoo.labs.samoa.instances.Attribute att = newInstance.attribute(i);
                String type = "";
                String name = att.name();
                List nominalValues = att.getAttributeValues();
                ArrayList appearances = new ArrayList();

                if (att.isNominal()) {
                    type = "nominal";

                    for (int j = 0; j < nominalValues.size(); j++) {
                        appearances.add(0);
                    }
                } else if (att.isNumeric()) {
                    type = "numeric";
                }

                this.attributes.add(new Attribute((String) name, type, 0, 0, appearances));
            }
        }

        //Cria raiz
        if (this.treeRoot == null) {
            this.treeRoot = newLearningNode();
            this.activeLeafNodeCount = 1;
        }
        //  ??
        int classIndex = (int) newInstance.classValue();

        //ARRAY QUE GUARDA A QNTD DE INSTANCIAS DE CADA CLASSE
        if (!this.validateClassIsMissing(inst))
            this.classesDistribution.set(classIndex, (int) this.classesDistribution.get(classIndex) + 1);

        for (int i = 0; i < numAttributes; i++) {
            Attribute attribute = this.attributes.get(i);

            if (attribute.getType() == "nominal") {
                ArrayList appearances = attribute.getAppearances();
                // aumenta o count daquele nominal em +1
                if (appearances.size() > i) {
                    appearances.set((int) inst.value(i), (int) appearances.get(i) + 1);
                    attribute.setAppearancesCount(attribute.getAppearancesCount() + 1);
                }
            } else if (attribute.getType() == "numeric") {
                attribute.setCount(attribute.getCount() + 1);
                attribute.setSum(attribute.getSum() + (double) inst.value(i));
                attribute.getValues().add(inst.value(i));
            }
        }

        if (this.validateClassIsMissing(newInstance)) {
            this.totalUnlabeled++;
        } else {
            this.totalLabeled++;
        }

        ((NewNode) this.treeRoot).learnFromInstance(newInstance, this, null, -1);
    }

    //New for options vote
    public FoundNode[] filterInstanceToLeaves(Instance inst, SplitNode parent, int parentBranch, boolean updateSplitterCounts) {
        List<FoundNode> nodes = new LinkedList<FoundNode>();
        ((NewNode) this.treeRoot).filterInstanceToLeaves(inst, parent, parentBranch, nodes,
                updateSplitterCounts);
        return nodes.toArray(new FoundNode[nodes.size()]);
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        if (this.treeRoot != null) {
            FoundNode[] foundNodes = filterInstanceToLeaves(inst,
                    null, -1, false);
            DoubleVector result = new DoubleVector();
            int predictionPaths = 0;
            for (FoundNode foundNode : foundNodes) {
                if (foundNode.parentBranch != -999) {
                    Node leafNode = foundNode.node;
                    if (leafNode == null) {
                        leafNode = foundNode.parent;
                    }
                    double[] dist = leafNode.getClassVotes(inst, this);
                    //Albert: changed for weights
                    //double distSum = Utils.sum(dist);
                    //if (distSum > 0.0) {
                    //	Utils.normalize(dist, distSum);
                    //}
                    result.addValues(dist);
                    //predictionPaths++;
                }
            }
            //if (predictionPaths > this.maxPredictionPaths) {
            //	this.maxPredictionPaths++;
            //}
            return result.getArrayRef();
        }
        return new double[0];
    }

    @Override
    public ImmutableCapabilities defineImmutableCapabilities() {
        if (this.getClass() == SSLHoeffdingAdaptiveTree.class)
            return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
        else
            return new ImmutableCapabilities(Capability.VIEW_STANDARD);
    }

    public interface NewNode {

        // Change for adwin
        //public boolean getErrorChange();
        int numberLeaves();

        double getErrorEstimation();

        double getErrorWidth();

        boolean isNullError();

        void killTreeChilds(SSLHoeffdingAdaptiveTree ht);

        void learnFromInstance(Instance inst, SSLHoeffdingAdaptiveTree ht, SplitNode parent, int parentBranch);

        void filterInstanceToLeaves(Instance inst, SplitNode myparent, int parentBranch, List<FoundNode> foundNodes,
                                    boolean updateSplitterCounts);
    }

    public static class AdaSplitNode extends SplitNode implements NewNode {

        private static final long serialVersionUID = 1L;
        public boolean ErrorChange = false;
        protected Node alternateTree;
        //public boolean isAlternateTree = false;
        protected ADWIN estimationErrorWeight;
        protected int randomSeed = 1;

        // KENNY
        int totalLabeled;
        int totalUnlabeled;
        ArrayList classesDistribution = new ArrayList();
        ArrayList<Attribute> attributes = new ArrayList<Attribute>();

        protected Random classifierRandom;

        public AdaSplitNode(InstanceConditionalTest splitTest, double[] classObservations, int size) {
            super(splitTest, classObservations, size);
            this.classifierRandom = new Random(this.randomSeed);
        }

        public AdaSplitNode(InstanceConditionalTest splitTest, double[] classObservations) {
            super(splitTest, classObservations);
            this.classifierRandom = new Random(this.randomSeed);
        }

        //public boolean getErrorChange() {
        //		return ErrorChange;
        //}
        @Override
        public int calcByteSizeIncludingSubtree() {
            int byteSize = calcByteSize();
            if (alternateTree != null) {
                byteSize += alternateTree.calcByteSizeIncludingSubtree();
            }
            if (estimationErrorWeight != null) {
                byteSize += estimationErrorWeight.measureByteSize();
            }
            for (Node child : this.children) {
                if (child != null) {
                    byteSize += child.calcByteSizeIncludingSubtree();
                }
            }
            return byteSize;
        }

        @Override
        public int numberLeaves() {
            int numLeaves = 0;
            for (Node child : this.children) {
                if (child != null) {
                    numLeaves += ((NewNode) child).numberLeaves();
                }
            }
            return numLeaves;
        }

        @Override
        public double getErrorEstimation() {
            return this.estimationErrorWeight.getEstimation();
        }

        @Override
        public double getErrorWidth() {
            double w = 0.0;
            if (isNullError() == false) {
                w = this.estimationErrorWeight.getWidth();
            }
            return w;
        }

        @Override
        public boolean isNullError() {
            return (this.estimationErrorWeight == null);
        }

        // SplitNodes can have alternative trees, but LearningNodes can't
        // LearningNodes can split, but SplitNodes can't
        // Parent nodes are allways SplitNodes
        @Override
        public void learnFromInstance(Instance inst, SSLHoeffdingAdaptiveTree ht, SplitNode parent, int parentBranch) {
            int numAttributes = inst.numAttributes() - 1;

            // instanciando os atributos
            if (this.classesDistribution.size() == 0) {
                List classes = inst.classAttribute().getAttributeValues();

                classes.forEach(item-> {
                    this.classesDistribution.add((int) 0);
                });

                for (int i = 0; i < numAttributes; i++) {
                    com.yahoo.labs.samoa.instances.Attribute att = inst.attribute(i);
                    String type = "";
                    String name = att.name();
                    List nominalValues = att.getAttributeValues();
                    ArrayList appearances = new ArrayList();

                    if (att.isNominal()) {
                        type = "nominal";

                        for (int j = 0; j < nominalValues.size(); j++) {
                            appearances.add(0);
                        }
                    } else if (att.isNumeric()) {
                        type = "numeric";
                    }

                    this.attributes.add(new Attribute((String) name, type, 0, 0, appearances));
                }
            }

            int classIndex = (int) inst.classValue();

            double classPred = inst.classValue();

            if (!ht.validateClassIsMissing((inst)))
                this.classesDistribution.set(classIndex, (int) this.classesDistribution.get(classIndex) + 1);

            for (int i = 0; i < numAttributes; i++) {
                Attribute attribute = this.attributes.get(i);

                if (attribute.getType() == "nominal") {
                    ArrayList appearances = attribute.getAppearances();
                    // aumenta o count daquele nominal em +1
                    if (appearances.size() > i) {
                        appearances.set((int) inst.value(i), (int) appearances.get(i) + 1);
                        attribute.setAppearancesCount(attribute.getAppearancesCount() + 1);
                    }
                } else if (attribute.getType() == "numeric") {
                    attribute.setCount(attribute.getCount() + 1);
                    attribute.setSum(attribute.getSum() + (double) inst.value(i));
                    attribute.getValues().add(inst.value(i));
                }
            }

            if (ht.validateClassIsMissing(inst)) {
                this.totalUnlabeled++;
            } else {
                this.totalLabeled++;
            }

            // FIM LIDANDO COM OS DADOS

            int trueClass = (int) inst.classValue();
            //New option vore
            int k = MiscUtils.poisson(1.0, this.classifierRandom);
            Instance weightedInst = inst.copy();

            if (k > 0) {
                //weightedInst.setWeight(inst.weight() * k);
            }

            //Compute ClassPrediction using filterInstanceToLeaf
            //int ClassPrediction = Utils.maxIndex(filterInstanceToLeaf(inst, null, -1).node.getClassVotes(inst, ht));

            int ClassPrediction = 0;
            if (filterInstanceToLeaf(inst, parent, parentBranch).node != null) {
                ClassPrediction = Utils.maxIndex(filterInstanceToLeaf(inst, parent, parentBranch).node.getClassVotes(inst, ht));
            }

            boolean blCorrect = (trueClass == ClassPrediction);

            if (this.estimationErrorWeight == null) {
                this.estimationErrorWeight = new ADWIN();
            }

            double oldError = this.getErrorEstimation();
            this.ErrorChange = this.estimationErrorWeight.setInput(blCorrect == true ? 0.0 : 1.0);

            if (this.ErrorChange == true && oldError > this.getErrorEstimation()) {
                //if error is decreasing, don't do anything
                this.ErrorChange = false;
            }

            // Check condition to build a new alternate tree
            //if (this.isAlternateTree == false) {
            if (this.ErrorChange == true) {//&& this.alternateTree == null) {
                //Start a new alternative tree : learning node
                this.alternateTree = ht.newLearningNode();
                //this.alternateTree.isAlternateTree = true;
                ht.alternateTrees++;
            } // Check condition to replace tree
            else if (this.alternateTree != null && ((NewNode) this.alternateTree).isNullError() == false) {
                if (this.getErrorWidth() > 300 && ((NewNode) this.alternateTree).getErrorWidth() > 300) {
                    double oldErrorRate = this.getErrorEstimation();
                    double altErrorRate = ((NewNode) this.alternateTree).getErrorEstimation();
                    double fDelta = .05;
                    //if (gNumAlts>0) fDelta=fDelta/gNumAlts;
                    double fN = 1.0 / ((NewNode) this.alternateTree).getErrorWidth() + 1.0 / this.getErrorWidth();
                    double Bound = Math.sqrt(2.0 * oldErrorRate * (1.0 - oldErrorRate) * Math.log(2.0 / fDelta) * fN);
                    if (Bound < oldErrorRate - altErrorRate) {
                        // Switch alternate tree
                        ht.activeLeafNodeCount -= this.numberLeaves();
                        ht.activeLeafNodeCount += ((NewNode) this.alternateTree).numberLeaves();
                        killTreeChilds(ht);
                        if (parent != null) {
                            parent.setChild(parentBranch, this.alternateTree);
                            //((AdaSplitNode) parent.getChild(parentBranch)).alternateTree = null;
                        } else {
                            // Switch root tree
                            ht.treeRoot = ((AdaSplitNode) ht.treeRoot).alternateTree;
                        }
                        ht.switchedAlternateTrees++;
                    } else if (Bound < altErrorRate - oldErrorRate) {
                        // Erase alternate tree
                        if (this.alternateTree instanceof ActiveLearningNode) {
                            this.alternateTree = null;
                            //ht.activeLeafNodeCount--;
                        } else if (this.alternateTree instanceof InactiveLearningNode) {
                            this.alternateTree = null;
                            //ht.inactiveLeafNodeCount--;
                        } else {
                            ((AdaSplitNode) this.alternateTree).killTreeChilds(ht);
                        }
                        ht.prunedAlternateTrees++;
                    }
                }
            }
            //}
            //learnFromInstance alternate Tree and Child nodes
            if (this.alternateTree != null) {
                ((NewNode) this.alternateTree).learnFromInstance(weightedInst, ht, parent, parentBranch);
            }

            int childBranch = this.instanceChildIndex(inst);
            Node child = this.getChild(childBranch);

            if (child != null) {
                ((NewNode) child).learnFromInstance(weightedInst, ht, this, childBranch);
            }
        }

        @Override
        public void killTreeChilds(SSLHoeffdingAdaptiveTree ht) {
            for (Node child : this.children) {
                if (child != null) {
                    //Delete alternate tree if it exists
                    if (child instanceof AdaSplitNode && ((AdaSplitNode) child).alternateTree != null) {
                        ((NewNode) ((AdaSplitNode) child).alternateTree).killTreeChilds(ht);
                        ht.prunedAlternateTrees++;
                    }
                    //Recursive delete of SplitNodes
                    if (child instanceof AdaSplitNode) {
                        ((NewNode) child).killTreeChilds(ht);
                    }
                    if (child instanceof ActiveLearningNode) {
                        child = null;
                        ht.activeLeafNodeCount--;
                    } else if (child instanceof InactiveLearningNode) {
                        child = null;
                        ht.inactiveLeafNodeCount--;
                    }
                }
            }
        }

        //New for option votes
        //@Override
        public void filterInstanceToLeaves(Instance inst, SplitNode myparent, int parentBranch, List<FoundNode> foundNodes, boolean updateSplitterCounts) {
            if (updateSplitterCounts) {
                this.observedClassDistribution.addToValue((int) inst.classValue(), inst.weight());
            }
            int childIndex = instanceChildIndex(inst);
            if (childIndex >= 0) {
                Node child = getChild(childIndex);
                if (child != null) {
                    ((NewNode) child).filterInstanceToLeaves(inst, this, childIndex, foundNodes, updateSplitterCounts);
                } else {
                    foundNodes.add(new FoundNode(null, this, childIndex));
                }
            }
            if (this.alternateTree != null) {
                ((NewNode) this.alternateTree).filterInstanceToLeaves(inst, this, -999, foundNodes, updateSplitterCounts);
            }
        }
    }

    public static class AdaLearningNode extends LearningNodeNBAdaptive implements NewNode {

        private static final long serialVersionUID = 1L;
        public boolean ErrorChange = false;
        protected ADWIN estimationErrorWeight;
        protected int randomSeed = 1;

        // KENNY
        protected ArrayList<Instance> instancesWithoutClass = new ArrayList();
        int totalLabeled;
        int totalUnlabeled;
        ArrayList classesDistribution = new ArrayList();
        ArrayList<Attribute> attributes = new ArrayList<Attribute>();

        int rcCounter = 0;

        protected Random classifierRandom;

        public AdaLearningNode(double[] initialClassObservations) {
            super(initialClassObservations);
            this.classifierRandom = new Random(this.randomSeed);
        }

        @Override
        public int calcByteSize() {
            int byteSize = super.calcByteSize();
            if (estimationErrorWeight != null) {
                byteSize += estimationErrorWeight.measureByteSize();
            }
            return byteSize;
        }

        @Override
        public int numberLeaves() {
            return 1;
        }

        @Override
        public double getErrorEstimation() {
            if (this.estimationErrorWeight != null) {
                return this.estimationErrorWeight.getEstimation();
            } else {
                return 0;
            }
        }

        @Override
        public double getErrorWidth() {
            return this.estimationErrorWeight.getWidth();
        }

        @Override
        public boolean isNullError() {
            return (this.estimationErrorWeight == null);
        }

        @Override
        public void killTreeChilds(SSLHoeffdingAdaptiveTree ht) {
        }

        @Override
        public void learnFromInstance(Instance inst, SSLHoeffdingAdaptiveTree ht, SplitNode parent, int parentBranch) {

            if (ht.validateClassIsMissing(inst)) {
                this.rcCounter++;
                DoubleVector newOb = new DoubleVector();

                double total = 0;
                int numClasses = inst.numClasses();

                for (int i = 0; i < numClasses; i++) {
                    total += this.observedClassDistribution.getValue(i);
                }

                for (int i = 0; i < inst.numClasses(); i++) {
                    double value = this.observedClassDistribution.getValue(i);
                    double newValue = value;

                    //newValue = this.rcCounter * (int) (value/total) + value;

                    newOb.setValue(i, newValue);
                }

                this.observedClassDistribution = newOb;
            }

            double impurity = 0;
            int numAttributes = inst.numAttributes() - 1;

            // instanciando os atributos
            if (this.classesDistribution.size() == 0) {
                List classes = inst.classAttribute().getAttributeValues();

                classes.forEach(item-> {
                    this.classesDistribution.add((int) 0);
                });

                for (int i = 0; i < numAttributes; i++) {
                    com.yahoo.labs.samoa.instances.Attribute att = inst.attribute(i);
                    String type = "";
                    String name = att.name();
                    List nominalValues = att.getAttributeValues();
                    ArrayList appearances = new ArrayList();

                    if (att.isNominal()) {
                        type = "nominal";

                        for (int j = 0; j < nominalValues.size(); j++) {
                            appearances.add(0);
                        }
                    } else if (att.isNumeric()) {
                        type = "numeric";
                    }

                    this.attributes.add(new Attribute((String) name, type, 0, 0, appearances));
                }
            }

            int classIndex = (int) inst.classValue();

            if (!ht.validateClassIsMissing(inst))
                this.classesDistribution.set(classIndex, (int) this.classesDistribution.get(classIndex) + 1);

            for (int i = 0; i < numAttributes; i++) {
                Attribute attribute = this.attributes.get(i);

                if (attribute.getType() == "nominal") {
                    ArrayList appearances = attribute.getAppearances();
                    // aumenta o count daquele nominal em +1
                    if (appearances.size() > i) {
                        appearances.set((int) inst.value(i), (int) appearances.get(i) + 1);
                        attribute.setAppearancesCount(attribute.getAppearancesCount() + 1);
                    }
                } else if (attribute.getType() == "numeric") {
                    attribute.setCount(attribute.getCount() + 1);
                    attribute.setSum(attribute.getSum() + (double) inst.value(i));
                    attribute.getValues().add(inst.value(i));
                }
            }

            if (ht.validateClassIsMissing(inst)) {
                this.totalUnlabeled++;
            } else {
                this.totalLabeled++;
            }

            // FIM LIDANDO COM OS DADOS

            int trueClass = (int) inst.classValue();

            //New option vore
            int k = MiscUtils.poisson(1.0, this.classifierRandom);
            Instance weightedInst = inst.copy();
            if (k > 0) {
                weightedInst.setWeight(inst.weight() * k);
            }

            boolean blCorrect = false;

            //Compute ClassPrediction using filterInstanceToLeaf
            if (!ht.validateClassIsMissing(inst)) {
                int ClassPrediction = Utils.maxIndex(this.getClassVotes(inst, ht));

                blCorrect = (trueClass == ClassPrediction);
            }

            if (this.estimationErrorWeight == null) {
                this.estimationErrorWeight = new ADWIN();
            }

            double oldError = this.getErrorEstimation();
            this.ErrorChange = this.estimationErrorWeight.setInput(blCorrect == true ? 0.0 : 1.0);
            if (this.ErrorChange == true && oldError > this.getErrorEstimation()) {
                this.ErrorChange = false;
            }

            //Update statistics
            learnFromInstance(weightedInst, ht);    //newInstance

            //Check for Split condition
            double weightSeen = this.getWeightSeen();
            if (weightSeen - this.getWeightSeenAtLastSplitEvaluation() >= ht.gracePeriodOption.getValue()) {
                //impurity = ht.impurity(this.classesDistribution, this.attributes);
                //ht.attemptToSplit(this, parent, parentBranch, impurity, inst, ht);
                ht.attemptToSplitSSL(this, parent, parentBranch, inst, ht,this.classesDistribution,this.attributes);

                this.setWeightSeenAtLastSplitEvaluation(weightSeen);
            }

            //learnFromInstance alternate Tree and Child nodes
			/*if (this.alternateTree != null)  {
            this.alternateTree.learnFromInstance(newInstance,ht);
            }
            for (Node child : this.children) {
            if (child != null) {
            child.learnFromInstance(newInstance,ht);
            }
            }*/
        }

        @Override
        public double[] getClassVotes(Instance inst, HoeffdingTree ht) {
            double[] dist;
            int predictionOption = ((SSLHoeffdingAdaptiveTree) ht).leafpredictionOption.getChosenIndex();
            if (predictionOption == 0) { //MC
                dist = this.observedClassDistribution.getArrayCopy();
            } else if (predictionOption == 1) { //NB
                dist = NaiveBayes.doNaiveBayesPrediction(inst,
                        this.observedClassDistribution, this.attributeObservers);
            } else { //NBAdaptive
                if (this.mcCorrectWeight > this.nbCorrectWeight) {
                    dist = this.observedClassDistribution.getArrayCopy();
                } else {
                    dist = NaiveBayes.doNaiveBayesPrediction(inst,
                            this.observedClassDistribution, this.attributeObservers);
                }
            }
            //New for option votes
            double distSum = Utils.sum(dist);
            if (distSum * this.getErrorEstimation() * this.getErrorEstimation() > 0.0) {
                Utils.normalize(dist, distSum * this.getErrorEstimation() * this.getErrorEstimation()); //Adding weight
            }
            return dist;
        }

        //New for option votes
        @Override
        public void filterInstanceToLeaves(Instance inst, SplitNode splitparent, int parentBranch, List<FoundNode> foundNodes, boolean updateSplitterCounts) {
            foundNodes.add(new FoundNode(this, splitparent, parentBranch));
        }
    }
}

