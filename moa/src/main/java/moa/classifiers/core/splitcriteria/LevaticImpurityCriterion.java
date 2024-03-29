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
import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import moa.core.ObjectRepository;
import moa.core.Utils;
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

    //SSLHOEFTree para auxiliar
    SSLHoeffdingAdaptiveTree ht;
    public SSLHoeffdingAdaptiveTree getHt() {
        return ht;
    }
    public void setHt(SSLHoeffdingAdaptiveTree ht) {
        this.ht = ht;
    }

    //PRE-SPLIT
    double preImpurity; //impureza do nodo
    public void setPreImpurity(double preImpurity) {
        this.preImpurity = preImpurity;
    }
    ArrayList<Attribute> preSplitAttributesDist = new ArrayList<Attribute>(); // distribuicao dos atributos
    public void setPreSplitAttributesDist(ArrayList attributes) {
        this.preSplitAttributesDist = attributes;
    }

    //POST-SPLIT
    AutoExpandVector<AutoExpandVector<DoubleVector>> postSplitAttributesDist;
    public void setPostSplitAttributesDist(AutoExpandVector<AutoExpandVector<DoubleVector>>  postSplitAttributesDist){
        this.postSplitAttributesDist = postSplitAttributesDist;
    }
    public int attIndexOfSplit;
    public void setAttIndexOfSplit(int i){
        this.attIndexOfSplit=i;
    }

    public FloatOption levaticWeight = new FloatOption(
            "levaticWeight",
            'W',
            "Levatic's  weigth used in levatic's metric",
            0.5,
            0.0,
            1.0);

    public double getMeritOfSplit(double[] preSplitDist, double[][] postSplitDists) {
        //double impurityPreSplit = preImpurity(this.preSplitClassesDistribution,this.preSplitAttributesDist); // impurity(preSplit) - impurity(postSplit)
        double impurityPreSplit = this.preImpurity;
        double impurityPostSplit = postImpurity(postSplitDists,postSplitAttributesDist);

        return impurityPreSplit - impurityPostSplit;
    }

    public double getRangeOfMerit(double[] preSplitDist) {
        // using same as infogain
        int numClasses = preSplitDist.length > 2 ? preSplitDist.length : 2;
        return Utils.log2(numClasses);
    }

    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        // TODO Auto-generated method stub
    }

    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }

    // VITOR E IGOR (Post-Split)
    public double postSupervisedGini(double[] dist, double distSumOfWeights) {
        double gini = 1.0;
        for (int i = 0; i < dist.length; i++) {
            double relFreq = dist[i] / distSumOfWeights;
            gini -= relFreq * relFreq;
        }
        return gini;
    }

    public double postUnsupervisedGini(DoubleVector nodeAttDist, int appearances){
        double probability = 0;
        double summation = 0;
        int numClasses = nodeAttDist.numValues();

        for(int i = 0; i < numClasses;i++){
            probability = nodeAttDist.getValue(i)/appearances;
            probability = probability * probability;
            summation = probability;
        }
        return 1 - summation;
    }

    public Double postUnsupervisedVariance(DoubleVector varianceNode){

        double variance = 0;

        double left_side = 0;
        double total_sum = 0;

        for (int i=0; i<varianceNode.numValues(); i++) {
            left_side += varianceNode.getValue(i) * varianceNode.getValue(i);
            total_sum += varianceNode.getValue(i);
        }
        double right_side = total_sum*total_sum/varianceNode.numValues();
        variance = left_side - right_side / varianceNode.numValues();

        return variance;
    }

    public double postImpurity(double[][] postClassSplitDist, AutoExpandVector<AutoExpandVector<DoubleVector>> postAttSplitDist){
        double w = levaticWeight.getValue();
        double supervisedImpurity = 0.0;
        double unsupervisedImpurity = 0.0;

        // SUPERVISED
        double totalWeight = 0.0;
        double[] distWeights = new double[postClassSplitDist.length];
        for (int i = 0; i < postClassSplitDist.length; i++) {
            distWeights[i] = Utils.sum(postClassSplitDist[i]);
            totalWeight += distWeights[i];
        }
        double gini = 0.0;
        for (int i = 0; i < postClassSplitDist.length; i++) {
            gini += (distWeights[i] / totalWeight)
                    * postSupervisedGini(postClassSplitDist[i], distWeights[i]);
        }
        supervisedImpurity = 1.0 - gini;

        // UNSUPERVISED
        double sslimpurity = 0.0;
        int numAttributes = this.preSplitAttributesDist.size(); // only to get attListSize
        ArrayList<Double> unsupervisedValues = new ArrayList<Double>();

        // Itera no numero de classes ( Ex: se bin, entao size == 2 )
        for(int i = 0 ; i < postAttSplitDist.size();i++){
            if(postAttSplitDist.get(i) == null){
                continue;
            }

            // Itera no numero de atributos comparados ( Ex : se Atts = [A,B,C...,N] entao size = N )
            for(int j = 0;j < postAttSplitDist.get(i).size();j++){
                Attribute att = (Attribute) preSplitAttributesDist.get(j); // only to get attType

                if (att.getType() == "nominal") {
                    int nodeAppearences = 0;
                    for(int k = 0; k < postAttSplitDist.get(i).get(j).numValues();k++){
                        nodeAppearences += (int) postAttSplitDist.get(i).get(j).getValue(k);
                    }
                    double giniNode = postUnsupervisedGini(postAttSplitDist.get(i).get(j),nodeAppearences);

                    // Guarda o valor total da arvore
                    int treeAppearances = 0;
                    DoubleVector treeValues = new DoubleVector();

                    // Itera no numero de classes ( Ex: se bin, entao size == 2 )
                    for (int k = 0 ; k < postAttSplitDist.size();k++){
                        if(postAttSplitDist.get(k) == null){
                            continue;
                        }
                        for(int n = 0 ; n < postAttSplitDist.get(k).get(j).numValues();n++){
                            treeAppearances += (int) postAttSplitDist.get(k).get(j).getValue(n);
                            treeValues.setValue(n,treeValues.getValue(n) + postAttSplitDist.get(k).get(j).getValue(n));
                        }
                    }

                    double giniTree = postUnsupervisedGini(treeValues,treeAppearances);
                    sslimpurity += giniNode/giniTree;
                }
                else if (att.getType() == "numeric") {
                    double varianceNode = this.postUnsupervisedVariance(postAttSplitDist.get(i).get(j));
                    //double varianceNode = this.preVariance(att.getValues(), att.getSum(), att.getCount());
                    //double varianceTree = this.preVariance(treeAttribute.getValues(), treeAttribute.getSum(), treeAttribute.getCount());
                    sslimpurity += 1.0;
                } else {
                    continue;
                }
            }
            unsupervisedValues.add(sslimpurity);
        }

        //ponderacao
        double ponderedSSLImpurity = 0.0;

        //A A0 A1 = A0 *impurityA0 / A  + A1 * IMPURITYA1 / A

        //if nominal
        if(this.preSplitAttributesDist.get(attIndexOfSplit).getType() == "nominal"){
            ArrayList<Integer> totalAttDist = new ArrayList<Integer>();
            int sumAtts = 0;
            for(int i = 0 ; i < postAttSplitDist.size();i++){
                if(postAttSplitDist.get(i) == null){
                    continue;
                }
                int perAtt = 0;
                for(int j = 0; j < postAttSplitDist.get(i).get(attIndexOfSplit).numValues();j++){
                    sumAtts = (int) postAttSplitDist.get(i).get(attIndexOfSplit).getValue(j);
                    perAtt = sumAtts;
                }
                totalAttDist.add(perAtt);
                perAtt = 0;
            }
            for(int i = 0 ; i < totalAttDist.size();i++){
                ponderedSSLImpurity += (unsupervisedValues.get(i) * totalAttDist.get(i)) / sumAtts;
            }
        }

        //if numeric
        else if(this.preSplitAttributesDist.get(attIndexOfSplit).getType() == "numeric"){

        }

        unsupervisedImpurity = ((1-w) / numAttributes) * ponderedSSLImpurity;
        return supervisedImpurity + unsupervisedImpurity;
    }
}
