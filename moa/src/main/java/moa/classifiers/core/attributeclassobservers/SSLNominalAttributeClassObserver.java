package moa.classifiers.core.attributeclassobservers;

import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.conditionaltests.NominalAttributeBinaryTest;
import moa.classifiers.core.conditionaltests.NominalAttributeMultiwayTest;
import moa.classifiers.core.splitcriteria.LevaticImpurityCriterion;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import moa.core.ObjectRepository;
import moa.core.Utils;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

/**
 * Class for observing the class and attribute data distribution for a nominal attribute.
 * This observer monitors the class and the attributes distribution of a given attribute.
 * Used in naive Bayes and decision trees to monitor data statistics on leaves.
 *
 * @author Igor Froehner e Vitor Clemes
 * @version $Revision: 1 $
 */
public class SSLNominalAttributeClassObserver extends AbstractOptionHandler implements
        DiscreteAttributeClassObserver {

    private static final long serialVersionUID = 1L;

    protected double totalWeightObserved = 0.0;
    protected double missingWeightObserved = 0.0;

    public AutoExpandVector<DoubleVector> attValDistPerClass = new AutoExpandVector<DoubleVector>();
    public AutoExpandVector<DoubleVector> attValDistPerAttribute = new AutoExpandVector<DoubleVector>();


    //Class-Based
    @Override
    public void observeAttributeClass(double attVal, int classVal, double weight) {
        // attVall = valor do atributo ("posicao")
        // classVal = valor da classe ("posicao")  // vote -> democrata = 0 , republican = 1
        if (Utils.isMissingValue(attVal)) {
            this.missingWeightObserved += weight;
        } else {
            //cast para inteiro
            int attValInt = (int) attVal;

            DoubleVector valDist = this.attValDistPerClass.get(classVal);
            if (valDist == null) {
                valDist = new DoubleVector();
                this.attValDistPerClass.set(classVal, valDist);
            }
            valDist.addToValue(attValInt, weight);
        }
        this.totalWeightObserved += weight;
    }

    /*
    ClassOberserver do Atributo A
    A = [ 0 , 1 ]
    Fiz o split em 0:
        30,100  -> Classes
    Fiz o split em 1:
        50,50  - > Classes


    Atributo A com O atributo B,C,D,E,F
    A = [0,1]
    Fiz o split em 0:
        B = 30,60
        C = 10,10
        D = 103..
    Fiz o split em 1:
        ...

    */

    //Attribute-based
    public void observeClassAttribute(double attVal,int classVal,double weight){
        if (Utils.isMissingValue(attVal)) {
            this.missingWeightObserved += weight;
        } else {
            //cast para inteiro
            int attValInt = (int) attVal;
            DoubleVector valDist = this.attValDistPerAttribute.get(classVal);
            if (valDist == null) {
                valDist = new DoubleVector();
                this.attValDistPerAttribute.set(classVal, valDist);
            }
            valDist.addToValue(attValInt, weight);
        }
        this.totalWeightObserved += weight;
    }

    @Override
    public double probabilityOfAttributeValueGivenClass(double attVal,
                                                        int classVal) {
        DoubleVector obs = this.attValDistPerClass.get(classVal);
        return obs != null ? (obs.getValue((int) attVal) + 1.0)
                / (obs.sumOfValues() + obs.numValues()) : 0.0;
    }

    public double totalWeightOfClassObservations() {
        return this.totalWeightObserved;
    }

    public double weightOfObservedMissingValues() {
        return this.missingWeightObserved;
    }

    @Override
    public AttributeSplitSuggestion getBestEvaluatedSplitSuggestion(
            SplitCriterion criterion, double[] preSplitDist, int attIndex,
            boolean binaryOnly) {

        AttributeSplitSuggestion bestSuggestion = null;

        int maxAttValsObserved = getMaxAttValsObserved();
        //DONE
        int maxAttAsClassValsObserved = getMaxAttAsClassValsObserved();

        if (!binaryOnly) {
            double[][] postSplitDists = getClassDistsResultingFromMultiwaySplit(maxAttValsObserved);
            //DONE
            double[][] postAttSplitDists = getAttributeDistResultingFromMultiwaySplit(maxAttAsClassValsObserved);
            if (criterion instanceof LevaticImpurityCriterion){
               //DONE
                ((LevaticImpurityCriterion) criterion).setPostSplitAttributesDist(postAttSplitDists);
            }

            double merit = criterion.getMeritOfSplit(preSplitDist,
                    postSplitDists);
            bestSuggestion = new AttributeSplitSuggestion(
                    new NominalAttributeMultiwayTest(attIndex), postSplitDists,
                    merit);
        }

        //Done
        for (int valIndexAtt = 0 ; valIndexAtt < maxAttAsClassValsObserved;valIndexAtt++){
            double[][] postAttSplitDist = getAttributeDistResultingFromBinarySplit(valIndexAtt);
            if (criterion instanceof LevaticImpurityCriterion){
                ((LevaticImpurityCriterion) criterion).setPostSplitAttributesDist(postAttSplitDist);
            }
        }

        for (int valIndex = 0; valIndex < maxAttValsObserved; valIndex++) {
            double[][] postSplitDists = getClassDistsResultingFromBinarySplit(valIndex);
            //PRECISO MUDAR ValIndex em relacao a MaxAttAsClassValsObserved

            double merit = criterion.getMeritOfSplit(preSplitDist,
                    postSplitDists);
            if ((bestSuggestion == null) || (merit > bestSuggestion.merit)) {
                bestSuggestion = new AttributeSplitSuggestion(
                        new NominalAttributeBinaryTest(attIndex, valIndex),
                        postSplitDists, merit);
            }
        }

        return bestSuggestion;
    }

    public int getMaxAttValsObserved() {
        int maxAttValsObserved = 0;
        for (DoubleVector attValDist : this.attValDistPerClass) {
            if ((attValDist != null)
                    && (attValDist.numValues() > maxAttValsObserved)) {
                maxAttValsObserved = attValDist.numValues();
            }
        }
        return maxAttValsObserved;
    }

    //DONE
    public int getMaxAttAsClassValsObserved() {
        int maxAttValsObserved = 0;
        for (DoubleVector attValDist : this.attValDistPerAttribute) {
            if ((attValDist != null)
                    && (attValDist.numValues() > maxAttValsObserved)) {
                maxAttValsObserved = attValDist.numValues();
            }
        }
        return maxAttValsObserved;
    }

    //DONE
    public double[][] getAttributeDistResultingFromMultiwaySplit(int maxAttValsObserved) {
        DoubleVector[] resultingDists = new DoubleVector[maxAttValsObserved];
        for (int i = 0; i < resultingDists.length; i++) {
            resultingDists[i] = new DoubleVector();
        }
        for (int i = 0; i < this.attValDistPerAttribute.size(); i++) {
            DoubleVector attValDist = this.attValDistPerAttribute.get(i);
            if (attValDist != null) {
                for (int j = 0; j < attValDist.numValues(); j++) {
                    resultingDists[j].addToValue(i, attValDist.getValue(j));
                }
            }
        }
        double[][] distributions = new double[maxAttValsObserved][];
        for (int i = 0; i < distributions.length; i++) {
            distributions[i] = resultingDists[i].getArrayRef();
        }
        return distributions;
    }

    public double[][] getClassDistsResultingFromMultiwaySplit(
            int maxAttValsObserved) {
        DoubleVector[] resultingDists = new DoubleVector[maxAttValsObserved];
        for (int i = 0; i < resultingDists.length; i++) {
            resultingDists[i] = new DoubleVector();
        }
        for (int i = 0; i < this.attValDistPerClass.size(); i++) {
            DoubleVector attValDist = this.attValDistPerClass.get(i);
            if (attValDist != null) {
                for (int j = 0; j < attValDist.numValues(); j++) {
                    resultingDists[j].addToValue(i, attValDist.getValue(j));
                }
            }
        }
        double[][] distributions = new double[maxAttValsObserved][];
        for (int i = 0; i < distributions.length; i++) {
            distributions[i] = resultingDists[i].getArrayRef();
        }
        return distributions;
    }

    //DONE
    public double[][] getAttributeDistResultingFromBinarySplit(int valIndex) {
        DoubleVector equalsDist = new DoubleVector();
        DoubleVector notEqualDist = new DoubleVector();
        for (int i = 0; i < this.attValDistPerAttribute.size(); i++) {
            DoubleVector attValDist = this.attValDistPerAttribute.get(i);
            if (attValDist != null) {
                for (int j = 0; j < attValDist.numValues(); j++) {
                    if (j == valIndex) {
                        equalsDist.addToValue(i, attValDist.getValue(j));
                    } else {
                        notEqualDist.addToValue(i, attValDist.getValue(j));
                    }
                }
            }
        }
        return new double[][]{equalsDist.getArrayRef(),
                notEqualDist.getArrayRef()};
    }

    public double[][] getClassDistsResultingFromBinarySplit(int valIndex) {
        DoubleVector equalsDist = new DoubleVector();
        DoubleVector notEqualDist = new DoubleVector();
        for (int i = 0; i < this.attValDistPerClass.size(); i++) {
            DoubleVector attValDist = this.attValDistPerClass.get(i);
            if (attValDist != null) {
                for (int j = 0; j < attValDist.numValues(); j++) {
                    if (j == valIndex) {
                        equalsDist.addToValue(i, attValDist.getValue(j));
                    } else {
                        notEqualDist.addToValue(i, attValDist.getValue(j));
                    }
                }
            }
        }
        return new double[][]{equalsDist.getArrayRef(),
                notEqualDist.getArrayRef()};
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        // TODO Auto-generated method stub
    }

    @Override
    public void observeAttributeTarget(double attVal, double target) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
