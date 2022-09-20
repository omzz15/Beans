package example;

import om.self.beans.BeanManager;

public class Example {
    public static void main(String[] args) {
        BeanManager.getInstance().setTargetPackage("example");
        //BeanManager.getInstance().setProfile("default");
        //BeanManager.getInstance().setDuplicateBeanPolicy(BeanManager.FailurePolicy.EXCEPTION);
        //BeanManager.getInstance().addBean(TestBean1.class, false);
        BeanManager.getInstance().load();

        BeanManager.getInstance().getBeans().forEach((K, V) -> System.out.println(K + " : " + V));
    }
}
