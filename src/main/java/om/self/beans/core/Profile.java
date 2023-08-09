package om.self.beans.core;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * used to annotate classes (beans) so that different classes can be loaded for different profiles
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Profile {
    /**
     * the profile of the bean
     * @return the profile of the bean
     */
    String value() default "default";
}
