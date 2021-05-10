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

import java.util.ArrayList;

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
    public AutoExpandVector<AutoExpandVector<DoubleVector>> attValDistPerAttribute = new AutoExpandVector<AutoExpandVector<DoubleVector>>();

    /*
    Attributo-classe A
    Attributos a serem comparados : B,C,..,N

    ClassValue0:
        A1.1
        A2.1
    ClassValue1:
        A1.2
        A2.2

    A1.1
    A1.2

    A2.1
    A2.2

    A0:
        A:
            A0:100%
            A1:0%
        B:                             Numerico
            Bi : 10
            Bii : 20
        C:                             Nominal
            Ci : 13
            Cii : 23
        N:                             Nominal
            Ni : 10
            Nii : 12

    A1:

        B:
            Bi : 23
            Bii : 32
        C:
            Ci : 23
            Cii : 13
        N:
            Ni : 22
            Nii : 10

     An...
     */

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



    //Attribute-based
    //DONE
    public void observeAttributes(double attVal,int attFlag,int attAsClassVal,double weight){
        if (Utils.isMissingValue(attVal)) {
            this.missingWeightObserved += weight;
        } else {
            //cast para inteiro
            int attValInt = (int) attVal;

            //pega A0
            AutoExpandVector attDist = this.attValDistPerAttribute.get(attAsClassVal);
            if (attDist == null)
                this.attValDistPerAttribute.set(attAsClassVal,new AutoExpandVector<DoubleVector>());

            //Pega o Attributo que sera comparado
            DoubleVector valDist = this.attValDistPerAttribute.get(attAsClassVal).get(attFlag);
            if(valDist == null){
                valDist = new DoubleVector();
                this.attValDistPerAttribute.get(attAsClassVal).set(attFlag,valDist);
            }

            // Pega o valor
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

        if (criterion instanceof LevaticImpurityCriterion){
            //DONE
            ((LevaticImpurityCriterion) criterion).setPostSplitAttributesDist(this.attValDistPerAttribute);
        }
        if (!binaryOnly) {
            double[][] postSplitDists = getClassDistsResultingFromMultiwaySplit(maxAttValsObserved);

            double merit = criterion.getMeritOfSplit(preSplitDist,
                    postSplitDists);
            bestSuggestion = new AttributeSplitSuggestion(
                    new NominalAttributeMultiwayTest(attIndex), postSplitDists,
                    merit);
        }

        for (int valIndex = 0; valIndex < maxAttValsObserved; valIndex++) {
            double[][] postSplitDists = getClassDistsResultingFromBinarySplit(valIndex);

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
//    public int getMaxAttAsClassValsObserved() {
//        int maxAttValsObserved = 0;
//        for (AutoExpandVector<DoubleVector> attDist : this.attValDistPerAttribute) {
//            for(DoubleVector attValDist : attDist) {
//                if ((attValDist != null)
//                        && (attValDist.numValues() > maxAttValsObserved)) {
//                    maxAttValsObserved = attValDist.numValues();
//                }
//            }
//        }
//        return maxAttValsObserved;
//    }


//    DONE
//    public double[][] getAttributeDistResultingFromMultiwaySplit(int maxAttValsObserved) {
//        // TODO: Sla
//        return null;
//    }

    /*

    ClassVal1:
        A1.1
        A2.1
    ClassVal2:
        A1.2
        A2.2

    1:
        A1.1
        A1.2
    2:
        A2.1
        A2.2
    */
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


//    public double[][] getAttributeDistResultingFromBinarySplit(int valIndex) {
//        // TODO: Sla
//        return null;
//    }

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
