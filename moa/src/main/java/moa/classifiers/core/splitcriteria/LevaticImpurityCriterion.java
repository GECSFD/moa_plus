package moa.classifiers.core.splitcriteria;

import moa.classifiers.trees.iadem.SSL.Attribute;
import moa.classifiers.trees.SSLHoeffdingAdaptiveTree;
import moa.core.ObjectRepository;
import com.github.javacliparser.FloatOption;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

import java.util.ArrayList;

public class LevaticImpurityCriterion extends AbstractOptionHandler implements SplitCriterion {

    SSLHoeffdingAdaptiveTree ht;

    public FloatOption levaticWeight = new FloatOption(
            "levaticWeight",
            'W',
            "Levatic's  weigth used in levatic's metric",
            0.5,
            0.0,
            1.0);

    public LevaticImpurityCriterion(SSLHoeffdingAdaptiveTree ht){
        this.ht=ht;
    }

    public double getMeritOfSplit(double[] preSplitDist, double[][] postSplitDists) {
        return impurity(preSplitDist,ht.getClassesDistribution()) - impurity(postSplitDists,ht.getClassesDistribution());
    }

    public double getRangeOfMerit(double[] preSplitDist) {
        // using same as gini
        return 1.0;
    }

    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        // TODO Auto-generated method stub
    }

    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
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

    public double impurity(ArrayList classesDistribution, ArrayList attributes) {
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
        double giniLabeledTraining = this.gini(ht.getClassesDistribution(), ht.getTotalLabeled()); // conjunto total

        //E1
        double supervised = w * (giniLabeled / giniLabeledTraining);

        int numAttributes = attributes.size();
        double semisupervised = 0.0;
        double sslimpurity = 0.0;


        for (int i = 0; i < numAttributes; i++) {
            Attribute att = (Attribute) attributes.get(i);
            Attribute treeAttribute = (Attribute) ht.getAttributes().get(i);

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
}
