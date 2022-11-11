package om.self.beans.core;

public class BeanContainer {
    public Object bean;
    public boolean isLoaded;

    public BeanContainer() {
    }

    public BeanContainer(Object bean, boolean isLoaded) {
        this.bean = bean;
        this.isLoaded = isLoaded;
    }
}
