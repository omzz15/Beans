package om.self.beans.core;

public class BeanLoad {
    public Runnable loadFunction;
    public boolean isLoaded;

    public BeanLoad() {
    }

    public BeanLoad(Runnable loadFunction, boolean isLoaded) {
        this.isLoaded = isLoaded;
        this.loadFunction = loadFunction;
    }

    public void load(){
        loadFunction.run();
        isLoaded = true;
    }
}
