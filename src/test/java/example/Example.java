package example;

import om.self.beans.BeanManager;

public class Example {
    public static void main(String[] args) {
        BeanManager.getInstance().setTargetPackage("example");
        BeanManager.getInstance().setProfile("production");
        BeanManager.getInstance().load();

        BeanManager.getInstance().getBeans().forEach((K, V) -> System.out.println(K + " : " + V));
    }
}
