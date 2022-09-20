package example;

import om.self.beans.Autowired;
import om.self.beans.Bean;
import om.self.beans.Profile;

@Profile("production")
public class ProductionBean extends DefaultBean{
    @Autowired
    public void set(TestBean1 b){}
}
