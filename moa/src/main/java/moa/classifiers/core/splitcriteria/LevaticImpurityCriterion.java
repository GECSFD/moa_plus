/*
 *    GiniSplitCriterion.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
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
package moa.classifiers.core.splitcriteria;

import com.github.javacliparser.FloatOption;
import moa.classifiers.trees.SSLHoeffdingAdaptiveTree;
import moa.classifiers.trees.iadem.SSL.Attribute;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;
import java.util.ArrayList;


/**
 * Class for computing splitting criteria using Gini
 * with respect to distributions of class values.
 * The split criterion is used as a parameter on
 * decision trees and decision stumps.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class LevaticImpurityCriterion extends AbstractOptionHandler implements
        SplitCriterion {

    SSLHoeffdingAdaptiveTree ht;
    ArrayList classesDistribution = new ArrayList();
    ArrayList<Attribute> attributes = new ArrayList<Attribute>();

    public SSLHoeffdingAdaptiveTree getHt() {
        return ht;
    }

    public void setHt(SSLHoeffdingAdaptiveTree ht) {
        this.ht = ht;
    }

    public void setClassesDistribution(ArrayList classesDistribution) {
        this.classesDistribution = classesDistribution;
    }

    public void setAttributes(ArrayList attributes) {
        this.attributes = attributes;
    }

    public FloatOption levaticWeight = new FloatOption(
            "levaticWeight",
            'W',
            "Levatic's  weigth used in levatic's metric",
            0.5,
            0.0,
            1.0);


    public double getMeritOfSplit(double[] preSplitDist, double[][] postSplitDists) {
//      Double[] doubleArray = ArrayUtils.toObject(preSplitDist);
//      List<Double> list = Arrays.asList(doubleArray);
//        for(int i = 0;i<preSplitDist.length;i++){
//            System.out.println("Pre : " +preSplitDist[i]);
//        }
//        for(int i = 0;i<postSplitDists.length;i++){
//            for(int j = 0; j<postSplitDists.length;j++){
//                System.out.println("Post : "+postSplitDists[i][j]);
//            }
//        }

        /*
            Considerando Pre-Post:
                se res < 0 => Split RUIM!
                Se res > 0 => Split BOM!
            Considerando Post-Pre:
                se res < 0 = SPLIT BOM!
                se res > 0 = SPLIT RUIM!

            PENSAR NISSO AO COMPARAR
         */

        System.out.println(ht.getClassesDistribution());
        System.out.println(classesDistribution);
        System.out.println(postSplitDists);
        System.out.println(preSplitDist);


        return impurity(this.classesDistribution,this.attributes);

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

    public double impurity(ArrayList classesDistribution, ArrayList<Attribute> attributes) {
        double impurity = 0.0;
        double w = levaticWeight.getValue(); // original = 0.5

        int totalLabeled = 0;


        for (int i = 0; i < classesDistribution.size(); i++) {
            totalLabeled += (int) classesDistribution.get(i);
        }


        //Testes

        {/*double entropyLabeled = this.entropy(classesDistribution, totalLabeled);
        //double entropyLabeledTraining = this.entropy(this.classesDistribution, this.totalLabeled);
        double supervised = w * entropyLabeled;*/}



        double giniLabeled = this.gini(classesDistribution,totalLabeled); //supostamente apenas rotulados
        double giniLabeledTraining = this.gini(ht.getClassesDistribution(),ht.getTotalLabeled()); // conjunto total

        //E1
        double supervised = 0;
        //double supervised = w * (giniLabeled / giniLabeledTraining);

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
