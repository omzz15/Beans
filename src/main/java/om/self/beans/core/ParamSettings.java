package om.self.beans.core;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ParamSettings {

    boolean allowRawBean() default false;
    boolean allowNull() default false;
}
