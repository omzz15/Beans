package example;

import om.self.beans.Autowired;
import om.self.beans.Bean;

@Bean
public class TestBean1 {
    @Autowired(useRawBeans = true)
    public void setBean(DefaultBean b){
        System.out.println("Bean Manager set bean in example.TestBean1 as " + b.getClass().getName());
    }
}
