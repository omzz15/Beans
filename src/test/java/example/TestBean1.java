package example;

import om.self.beans.Bean;
import om.self.beans.core.Autowired;

@Bean(alwaysLoad = true)
public class TestBean1 {
    @Autowired
    public void test(DefaultBean bean){
        System.out.println("Eclair: " + bean);
    }
}
