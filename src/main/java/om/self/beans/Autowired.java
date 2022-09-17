package om.self.beans;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Autowired {
    /**
     * weather the autowired values can be raw beans(beans that have not been auto-wired)
     * @return if this value can be a raw bean
     */
    boolean useRawBeans() default false;
}
