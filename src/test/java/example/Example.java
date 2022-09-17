import om.self.beans.BeanManager;

public class BeansTest {
    public static void main(String[] args) {
        BeanManager.getInstance().setTargetPackage("om.self.beans");
        BeanManager.getInstance().setProfile("production");
        BeanManager.getInstance().load();

        BeanManager.getInstance().getBeans().forEach((K, V) -> System.out.println(K + " : " + V));
    }
}
