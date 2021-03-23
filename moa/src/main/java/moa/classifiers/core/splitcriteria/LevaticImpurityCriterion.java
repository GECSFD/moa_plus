package moa.classifiers.core.splitcriteria;

import moa.core.ObjectRepository;
import moa.core.Utils;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

public class LevaticImpurityCriterion extends AbstractOptionHandler implements
        SplitCriterion {

    @Override
    public double getMeritOfSplit(double[] preSplitDist, double[][] postSplitDists) {
        double totalWeight = 0.0;
        double[] distWeights = new double[postSplitDists.length];
        for (int i = 0; i < postSplitDists.length; i++) {
            distWeights[i] = Utils.sum(postSplitDists[i]);
            totalWeight += distWeights[i];
        }
        double gini = 0.0;
        for (int i = 0; i < postSplitDists.length; i++) {
            gini += (distWeights[i] / totalWeight);
                    //* computeGini(postSplitDists[i], distWeights[i]);
        }
        return 1.0 - gini;
    }

    @Override
    public double getRangeOfMerit(double[] preSplitDist) {
        int numClasses = preSplitDist.length > 2 ? preSplitDist.length : 2;
        return Utils.log2(numClasses);
    }

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        // TODO Auto-generated method stub
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }

    public double computeImpurity(double[] split){
        return 1.0;
    }
}
