package moa.classifiers.core.attributeclassobservers;

import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.conditionaltests.NominalAttributeBinaryTest;
import moa.classifiers.core.conditionaltests.NominalAttributeMultiwayTest;
import moa.classifiers.core.splitcriteria.LevaticImpurityCriterion;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.classifiers.oneclass.Autoencoder;
import moa.core.*;
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

    public int attIndexOfSplit = 0;
    public AutoExpandVector<DoubleVector> attValDistPerClass = new AutoExpandVector<DoubleVector>();
    public AutoExpandVector<AutoExpandVector<DoubleVector>> attValDistPerAttribute = new AutoExpandVector<AutoExpandVector<DoubleVector>>();
    public AutoExpandVector<AutoExpandVector<GaussianEstimator>> gaussianEstimators = new AutoExpandVector<>();

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
    public void observeNumericAttribute(double attVal,int attFlag,int attAsClassVal,double weight,int attIndex){
        this.attIndexOfSplit = attIndex;
        if (Utils.isMissingValue(attVal)) {
            this.missingWeightObserved += weight;
        } else {
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

            AutoExpandVector estimatorDist = this.gaussianEstimators.get(attAsClassVal);
            if(estimatorDist == null){
                this.gaussianEstimators.set(attAsClassVal, new AutoExpandVector<GaussianEstimator>());
            }

            GaussianEstimator estimator = this.gaussianEstimators.get(attAsClassVal).get(attFlag);
            if(estimator == null){
                estimator = new GaussianEstimator();
                estimator.addObservation(attVal,weight);
                this.gaussianEstimators.get(attAsClassVal).set(attFlag,estimator);
            }
            else{
                estimator.addObservation(attVal,weight);
                this.gaussianEstimators.get(attAsClassVal).set(attFlag,estimator);
            }
            // Pega o valor
            valDist.setValue(0,estimator.getVariance());
        }
        this.totalWeightObserved += weight;
    }

    public void observeNominalAttribute(/* valor do atributo*/double attVal,/*index do atributo*/int attFlag,
            /*Valor do atributo como Classe*/int attAsClassVal,double weight,int attIndex) {

        this.attIndexOfSplit = attIndex;
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
