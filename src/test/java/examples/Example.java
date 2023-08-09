package examples;

import om.self.beans.core.BeanManager;
import om.self.beans.core.Profile;

/**
 * A simple example of how to use the library
 */
public class Example {
    public static void main(String[] args)  {
        //we create a bean manager then add our classes to it
        BeanManager bm = new BeanManager();
        new t1(bm);
        new t2(bm);

        //we load all the classes
        bm.load();

        //this will give us t2 because the parent class has the default profile which is inherited by t2 because recursivelyCheckForProfile while t1 has the production profile
        System.out.println("Grabbed " + bm.getBestMatch(parent.class, false) + " with default profile!");

        //this will give us t1 because it has the production profile
        bm.getSettings().setProfile("production");
        System.out.println("Grabbed " + bm.getBestMatch(parent.class, false) + " with production profile!");
    }
}

/**
 * A simple class that will be used as a parent class
 */
@Profile //this is the default profile
class parent{}

/**
 * A simple "production" class that will be loaded
 */
@Profile("production")
class t1 extends parent{
    BeanManager bm;
    public t1(BeanManager bm){
        this.bm = bm;
        bm.addBean(this, this::onLoad, true, false);
    }

    public void onLoad(){
        //this will grab t2 and print it when bm.load() is called
        System.out.println("Loaded " + bm.getBestMatch(t2.class, true) + "!");
    }
}

/**
 * A simple class that will be loaded
 */
class t2 extends parent{
    BeanManager bm;
    public t2(BeanManager bm){
        this.bm = bm;
        bm.addBean(this, this::onLoad, true, false);
    }

    public void onLoad(){
        //this will grab t1 and print it when bm.load() is called
        System.out.println("Loaded " + bm.getBestMatch(t1.class, false) + "!");
    }
}