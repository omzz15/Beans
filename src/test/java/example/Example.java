package example;
import om.self.beans.PackageBeanManager;

public class Example {
    public static void main(String[] args)  {
        PackageBeanManager test = new PackageBeanManager("example", "runtime");
        //test.getSettings().setProfile("production");
        //test.removeDefaultTag();

        test.load();
    }
}
