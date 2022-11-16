package example;

import om.self.beans.core.BeanManager;

public class Example {
    public static void main(String[] args)  {
        BeanManager bm = new BeanManager();
        new t1(bm);
        new t2(bm);

        bm.load();
    }
}

class t1{
    BeanManager bm;
    public t1(BeanManager bm){
        this.bm = bm;
        bm.addBean(this, this::onLoad, true, false);
    }

    public void onLoad(){
        System.out.println(bm.getBestMatch(t2.class, true));
    }
}

class t2{
    BeanManager bm;
    public t2(BeanManager bm){
        this.bm = bm;
        bm.addBean(this, this::onLoad, true, false);
    }

    public void onLoad(){
        System.out.println(bm.getBestMatch(t1.class, false));
    }
}