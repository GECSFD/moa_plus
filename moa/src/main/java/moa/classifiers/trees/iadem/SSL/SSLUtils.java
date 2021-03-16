//Vitor e Igor
package moa.classifiers.trees.iadem.SSL;

//java imports
import java.io.Serializable;
import com.yahoo.labs.samoa.instances.Instance;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Helper class with methods to be used with ClassifierTree instances to set classes as missing.
 *
 * In order to properly implement the services of this class an instance of this object
 * should be stored inside the desired ClassifierTree, the buildClass method should use the
 * getInstancesWithRemovedClasses in order to get the instances with the class set as missing,
 * and inherit the all the other getters and setter methods (some are inherited from
 * ClassifierTree, like options setter, getter and lister, while some other methods are used
 * to control the inner parameter of this class, which should be implemented as per the
 * RCParamsGettersAndSetters interface and delegate the behavior to the respective method in
 * this class)
 *
 */
public class SSLUtils implements Serializable {

    public SSLUtils() {

    }

    /**
     * Compute a new set containing the same instances as provided in originalInstances
     * but with the class attribute missing accordingly to the probability set.
     * An uniform distribution with the set seed is used in order to decide with instances
     * should have the class removed.
     *
     * @param originalInstance instance to get the class attribute removed
     * @param removeChance chances of removing the class attribute
     * @return a set containing the same provided instances but without the class for some
     */
    public Instance getInstanceWithRemovedClass(Instance originalInstance,double removeChance){
        Random random = new Random();
        Instance newInstance = originalInstance.copy();

        if(random.nextFloat()<removeChance){
            newInstance.setClassMissing();
          //  System.out.println("removeu!");
        }
        return newInstance;
    }

    /**
     * Verify if instance class has been removed
     * @param inst instance
     * @return a boolean
     */
    public boolean validateClassIsMissing(Instance inst) {
        double classPred = inst.classValue();
        return Double.isNaN(classPred);
    }

    /**
     *  Set the attributes array.
     * @param instance the instance
     * @param iterator the iterator for numAttributes
     * @return the attribute
     */
    public Attribute setAttributesArray(Instance instance,int iterator){

        com.yahoo.labs.samoa.instances.Attribute att = instance.attribute(iterator);
        String type = "";
        String name = att.name();
        List nominalValues = att.getAttributeValues();
        ArrayList appearances = new ArrayList();

        if (att.isNominal()) {
            type = "nominal";
                for (int j = 0; j < nominalValues.size(); j++) {
                    appearances.add(0);
                }

            }
        else if (att.isNumeric()) {
            type = "numeric";
        }

        //Criou um atributo com o nome e tipo. APPEARANCES eh um array com tamanho m = nmr_valores_atributos
        // e de valor 0, se o atributo foi nominal
        return new Attribute((String) name, type, 0, 0, appearances);

    }
}

