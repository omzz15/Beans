package example;
import om.self.beans.PackageBeanManager;

public class Example {
    public static void main(String[] args)  {
        PackageBeanManager test = new PackageBeanManager("example", "runtime");
        test.getSettings().setProfile("production");
        //test.removeDefaultTag();

        test.load();
        System.out.println(test.getBestMatch(TestBean1.class, false, false));
        System.out.println("done!");
    }
}
