package om.self.beans.core;

/**
 * A simple container that holds a Runnable that will be executed to load a bean(the load function) and weather it is loaded or not
 */
public class BeanLoad {
    /**
     * The code that will be executed to load the bean
     */
    public Runnable loadFunction = () -> {};
    /**
     * Flag that lets the bean manager know whether the bean is loaded or not
     */
    public boolean isLoaded;

    /**
     * Default constructor
     */
    public BeanLoad() {
    }

    /**
     * Constructor that sets the load function ({@link #loadFunction}) and weather it is loaded or not ({@link #isLoaded})
     * @param loadFunction code to be run to load the bean
     * @param isLoaded weather the bean is already loaded or not
     */
    public BeanLoad(Runnable loadFunction, boolean isLoaded) {
        this.isLoaded = isLoaded;
        this.loadFunction = loadFunction;
    }

    /**
     * runs {@link #loadFunction} and sets {@link #isLoaded} to true
     */
    public void load(){
        loadFunction.run();
        isLoaded = true;
    }
}
