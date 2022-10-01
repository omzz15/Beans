package om.self.beans;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Bean {
    String[] tags() default {"default"};

    boolean alwaysLoad() default false;
}
