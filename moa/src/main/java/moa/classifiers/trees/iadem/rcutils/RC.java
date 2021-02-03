package moa.classifiers.trees.iadem.rcutils;

//java imports
import java.io.Serializable;
import com.yahoo.labs.samoa.instances.Instance;
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
public class RC implements Serializable {

    public RC() {

    }
    //MODIFICADO POR VITOR E IGOR
    /**
     * Compute a new set containing the same instances as provided in originalInstances
     * but with the class attribute missing accordingly to the probability set.
     * An uniform distribution with the set seed is used in order to decide with instances
     * should have the class removed.
     *
     * @param originalInstance
     * @return a set containing the same provided instances but without the class for some
     */
    public Instance getInstanceWithRemovedClass(Instance originalInstance,double removeChance){
        Random random = new Random();
        Instance newInstance = originalInstance.copy();
        //PERCORRERIA A LISTA PRA ACHAR NEWINSTANCE
        if(random.nextFloat()<removeChance){
            newInstance.setClassMissing();
          //  System.out.println("removeu!");
        }
        return newInstance;
    }
}
