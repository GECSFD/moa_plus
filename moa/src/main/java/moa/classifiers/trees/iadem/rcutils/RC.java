package moa.classifiers.trees.iadem.rcutils;


//java imports
import java.io.Serializable;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;


import java.util.Random;
import java.util.Vector;
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

    /**
     * Methods to be inherited by the Class implementation
     * The implementations of this interface should delegate the behavior to the respective
     * method in this outer class
     */
    public interface RCParamsGettersAndSetters {
        /**
         * Returns the tip text for this property
         *
         * @return tip text for this property suitable for displaying in the
         *         explorer/experimenter gui
         */
        String removeClassSeedTipText();

        /**
         * Get the value of Seed.
         *
         * @return Value of Seed.
         */
        int getRemoveClassSeed();

        /**
         * Set the value of Seed.
         *
         * @param newSeed Value to assign to Seed.
         */
        void setRemoveClassSeed(int newSeed);

        /**
         * Returns the tip text for this property
         *
         * @return tip text for this property suitable for displaying in the
         *         explorer/experimenter gui
         */
        String removeClassProbabilityTipText();

        /**
         * Get the value of the probability.
         *
         * @return Value of the probability.
         */
        double getRemoveClassProbability();

        /**
         * Set the value of the probability.
         *
         * @param probability Value to assign to the probability.
         */
        void setRemoveClassProbability(double probability);
    }

    private int m_removeClassSeed = 0;
    private double m_removeClassProbability = 0.;

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
        Random random = new Random(m_removeClassSeed);
        Instance newInstance = originalInstance.copy();
        //PERCORRERIA A LISTA PRA ACHAR NEWINSTANCE
        if(random.nextFloat()<removeChance){
            newInstance.setClassMissing();
            System.out.println("removeu!");
        }
        return newInstance;
    }

//    public Instances getInstancesWithRemovedClasses(Instances originalInstances) {
//        Random random = new Random(m_removeClassSeed);
//        Instances newInstances = new Instances(originalInstances, originalInstances.numInstances());
//        for(int i=0;i<originalInstances.numInstances();i++){
//            Instance newInstance = (Instance) (originalInstances.instance(i).copy());
//                if (random.nextFloat()<m_removeClassProbability) {
//                    newInstance.setClassMissing();public Instances getInstancesWithRemovedClasses(Instances originalInstances) {
//        Random random = new Random(m_removeClassSeed);
//        Instances newInstances = new Instances(originalInstances, originalInstances.numInstances());
//        for(int i=0;i<originalInstances.numInstances();i++){
//            Instance newInstance = (Instance) (originalInstances.instance(i).copy());
//                if (random.nextFloat()<m_removeClassProbability) {
//                    newInstance.setClassMissing();
//            }
//            newInstances.add(newInstance);
//        }
////        for (Instance originalInstance : originalInstances.) {
////            Instance newInstance = (Instance) (originalInstance.copy());
////            if (random.nextFloat()<m_removeClassProbability) {
////                newInstance.setClassMissing();
////            }
////            newInstances.add(newInstance);
////        }
//        return newInstances;
//    }
//            }
//            newInstances.add(newInstance);
//        }
////        for (Instance originalInstance : originalInstances.) {
////            Instance newInstance = (Instance) (originalInstance.copy());
////            if (random.nextFloat()<m_removeClassProbability) {
////                newInstance.setClassMissing();
////            }
////            newInstances.add(newInstance);
////        }
//        return newInstances;
//    }

//    /**
//     * Returns an enumeration describing the available options.
//     *
//     * Valid options are:
//     * <p>
//     *
//     * -Rc probability <br>
//     * Set the probability of removing the class of an instance. (default 0.0)
//     * <p>
//     *
//     * -Q seed <br>
//     * Seed for the class probability remover. (default 0)
//     * <p>
//     *
//     * See options available for child class
//     *
//     * @return an enumeration of all the available options.
//     */
//    public Enumeration<Option> listOptions() {
//
//        Vector<Option> newVector = new Vector<Option>();
//        newVector.addElement(new Option(
//                "\tSet the probability of removing the class of an instance.\n" + "\t(default 0.0)", "Rc",
//                1, "-Rc <probability of removing a class>"));
//        newVector.addElement(new Option(
//                "\tSeed for the class probability remover (default 0).", "Q", 1, "-Q <seed>"));
//
//        return newVector.elements();
//    }

//    /**
//     * Parses a given list of options.
//     *
//     * <!-- options-start --> Valid options are:
//     * <p/>
//     *
//     *
//     * <pre>
//     * -Rc &lt;probability&gt;
//     *  Set the probability of removing the class of an instance.
//     *  (default 0.0)
//     * </pre>
//     *
//     * <pre>
//     * -Q &lt;seed&gt;
//     *  Seed for the class probability remover.
//     *  (default 0)
//     * </pre>
//     *
//     * <!-- options-end -->
//     *
//     * @param options the list of options as an array of strings
//     * @throws Exception if an option is not supported
//     */
//    public void setOptions(String[] options) throws Exception {
//        String removeCLassProbabilityString = Utils.getOption("Rc", options);
//        if (removeCLassProbabilityString.length() != 0) {
//            m_removeClassProbability = Double.parseDouble(removeCLassProbabilityString);
//            if ((m_removeClassProbability < 0) || (m_removeClassProbability > 1)) {
//                throw new Exception("Remove probability has to be between zero and one!");
//            }
//        } else {
//            m_removeClassProbability = 0.f;
//        }
//
//        String seedString = Utils.getOption('Q', options);
//        if (seedString.length() != 0) {
//            m_removeClassSeed = Integer.parseInt(seedString);
//        } else {
//            m_removeClassSeed = 0;
//        }
//    }

    /**
     * Gets the current settings of the Classifier.
     *
     * @return an array of strings suitable for passing to setOptions
     */
    public String[] getOptions() {

        Vector<String> options = new Vector<String>();

        options.add("-Rc");
        options.add("" + m_removeClassProbability);
        options.add("-Q");
        options.add("" + m_removeClassSeed);

        return options.toArray(new String[0]);
    }

    /**
     * Returns the tip text for this property
     *
     * @return tip text for this property suitable for displaying in the
     *         explorer/experimenter gui
     */
    public String removeClassSeedTipText() {
        return "Seed for the class probability remover.";
    }

    /**
     * Get the value of Seed.
     *
     * @return Value of Seed.
     */
    public int getRemoveClassSeed() {
        return m_removeClassSeed;
    }

    /**
     * Set the value of Seed.
     *
     * @param newSeed Value to assign to Seed.
     */
    public void setRemoveClassSeed(int newSeed) {
        m_removeClassSeed = newSeed;
    }

    /**
     * Returns the tip text for this property
     *
     * @return tip text for this property suitable for displaying in the
     *         explorer/experimenter gui
     */
    public String removeClassProbabilityTipText() {
        return "Probability of removing the class of an instance.";
    }

    /**
     * Get the value of the probability.
     *
     * @return Value of the probability.
     */
    public double getRemoveClassProbability() {
        return m_removeClassProbability;
    }

    /**
     * Set the value of the probability.
     *
     * @param probability Value to assign to the probability.
     */
    public void setRemoveClassProbability(double probability) {
        m_removeClassProbability = probability;
    }

}
