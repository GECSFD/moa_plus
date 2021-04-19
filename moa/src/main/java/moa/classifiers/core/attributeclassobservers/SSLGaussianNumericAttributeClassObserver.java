package moa.classifiers.core.attributeclassobservers;

import com.github.javacliparser.IntOption;
import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.conditionaltests.NumericAttributeBinaryTest;
import moa.classifiers.core.splitcriteria.LevaticImpurityCriterion;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.core.*;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

import java.util.Set;
import java.util.TreeSet;

/**
 * Class for observing the class and attributes data distribution for a numeric attribute using gaussian estimators.
 * This observer monitors the class and attributes distribution of a given attribute.
 * Used in naive Bayes and decision trees to monitor data statistics on leaves.
 *
 * @author Igor Froehner e Vitor Clemes
 * @version $Revision: 1 $
 */
public class SSLGaussianNumericAttributeClassObserver extends AbstractOptionHandler
        implements NumericAttributeClassObserver {

    private static final long serialVersionUID = 1L;

    protected DoubleVector minValueObservedPerClass = new DoubleVector();

    protected DoubleVector maxValueObservedPerClass = new DoubleVector();

    protected AutoExpandVector<GaussianEstimator> attValDistPerClass = new AutoExpandVector<GaussianEstimator>();
    protected AutoExpandVector<GaussianEstimator> attValDistPerAtt = new AutoExpandVector<>();

    public IntOption numBinsOption = new IntOption("numBins", 'n',
            "The number of bins.", 10, 1, Integer.MAX_VALUE);

    @Override
    public void observeAttributeClass(double attVal, int classVal, double weight) {
        if (Utils.isMissingValue(attVal)) {
        } else {
            GaussianEstimator valDist = this.attValDistPerClass.get(classVal);
            if (valDist == null) {
                valDist = new GaussianEstimator();
                this.attValDistPerClass.set(classVal, valDist);
                this.minValueObservedPerClass.setValue(classVal, attVal);
                this.maxValueObservedPerClass.setValue(classVal, attVal);
            } else {
                if (attVal < this.minValueObservedPerClass.getValue(classVal)) {
                    this.minValueObservedPerClass.setValue(classVal, attVal);
                }
                if (attVal > this.maxValueObservedPerClass.getValue(classVal)) {
                    this.maxValueObservedPerClass.setValue(classVal, attVal);
                }
            }
            valDist.addObservation(attVal, weight);
        }
    }

    @Override
    public double probabilityOfAttributeValueGivenClass(double attVal,
                                                        int classVal) {
        GaussianEstimator obs = this.attValDistPerClass.get(classVal);
        return obs != null ? obs.probabilityDensity(attVal) : 0.0;
    }

    @Override
    public AttributeSplitSuggestion getBestEvaluatedSplitSuggestion(
            SplitCriterion criterion, double[] preSplitDist, int attIndex,
            boolean binaryOnly) {
        AttributeSplitSuggestion bestSuggestion = null;
        double[] suggestedSplitValues = getSplitPointSuggestions();
        for (double splitValue : suggestedSplitValues) {
            double[][] postSplitDists = getClassDistsResultingFromBinarySplit(splitValue);

            double[][] postAttSplitDists = getAttributeDistResultingFromBinarySplit(splitValue);
            if (criterion instanceof LevaticImpurityCriterion){
//                TODO: Passar os atributos para o critério
                ((LevaticImpurityCriterion) criterion).setAttributes(null);
            }

            double merit = criterion.getMeritOfSplit(preSplitDist,
                    postSplitDists);
            if ((bestSuggestion == null) || (merit > bestSuggestion.merit)) {
                bestSuggestion = new AttributeSplitSuggestion(
                        new NumericAttributeBinaryTest(attIndex, splitValue,
                                true), postSplitDists, merit);
            }
        }
        return bestSuggestion;
    }

    public double[] getSplitPointSuggestions() {
        Set<Double> suggestedSplitValues = new TreeSet<Double>();
        double minValue = Double.POSITIVE_INFINITY;
        double maxValue = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < this.attValDistPerClass.size(); i++) {
            GaussianEstimator estimator = this.attValDistPerClass.get(i);
            if (estimator != null) {
                if (this.minValueObservedPerClass.getValue(i) < minValue) {
                    minValue = this.minValueObservedPerClass.getValue(i);
                }
                if (this.maxValueObservedPerClass.getValue(i) > maxValue) {
                    maxValue = this.maxValueObservedPerClass.getValue(i);
                }
            }
        }
        if (minValue < Double.POSITIVE_INFINITY) {
            double range = maxValue - minValue;
            for (int i = 0; i < this.numBinsOption.getValue(); i++) {
                double splitValue = range / (this.numBinsOption.getValue() + 1.0) * (i + 1)
                        + minValue;
                if ((splitValue > minValue) && (splitValue < maxValue)) {
                    suggestedSplitValues.add(splitValue);
                }
            }
        }
        double[] suggestions = new double[suggestedSplitValues.size()];
        int i = 0;
        for (double suggestion : suggestedSplitValues) {
            suggestions[i++] = suggestion;
        }
        return suggestions;
    }

    public double[][] getAttributeDistResultingFromBinarySplit(double splitValue) {
//        TODO: implementar isso aqui
        return null;
    }

    // assume all values equal to splitValue go to lhs
    public double[][] getClassDistsResultingFromBinarySplit(double splitValue) {
        DoubleVector lhsDist = new DoubleVector();
        DoubleVector rhsDist = new DoubleVector();
        for (int i = 0; i < this.attValDistPerClass.size(); i++) {
            GaussianEstimator estimator = this.attValDistPerClass.get(i);
            if (estimator != null) {
                if (splitValue < this.minValueObservedPerClass.getValue(i)) {
                    rhsDist.addToValue(i, estimator.getTotalWeightObserved());
                } else if (splitValue >= this.maxValueObservedPerClass.getValue(i)) {
                    lhsDist.addToValue(i, estimator.getTotalWeightObserved());
                } else {
                    double[] weightDist = estimator.estimatedWeight_LessThan_EqualTo_GreaterThan_Value(splitValue);
                    lhsDist.addToValue(i, weightDist[0] + weightDist[1]);
                    rhsDist.addToValue(i, weightDist[2]);
                }
            }
        }
        return new double[][]{lhsDist.getArrayRef(), rhsDist.getArrayRef()};
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
