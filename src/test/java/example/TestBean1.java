package example;

import om.self.beans.Bean;
import om.self.beans.core.Autowired;
import om.self.beans.core.Profile;

@Bean
public class TestBean1 {
    @Autowired
    public void test(DefaultBean bean){
        System.out.println(bean);
    }
}
