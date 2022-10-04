package example;

import om.self.beans.Bean;
import om.self.beans.core.Autowired;
import om.self.beans.core.ParamSettings;
import om.self.beans.core.Profile;

@Bean
@Profile
public class DefaultBean {
    @Autowired
    public void load(@ParamSettings(allowRawBean = true) TestBean1 tb1){
        System.out.println("Danish: " + tb1);
    }
}
