package example;

import om.self.beans.core.Autowired;
import om.self.beans.core.ParamSettings;
import om.self.beans.core.Profile;

@Profile("production")
public class ProductionBean extends DefaultBean{

    @Autowired
    public void set(@ParamSettings(allowNull = true) TestBean1 b){}
}
