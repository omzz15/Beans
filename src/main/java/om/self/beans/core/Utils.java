package om.self.beans.core;

import java.lang.annotation.Annotation;

public class Utils {
    /**
     * gets an annotation if it is present in the specified class or any of its parent/super classes
     * @param cls the class you want to check
     * @param annotation the annotaion you want to get
     * @return the annotation if found else it will return null
     * @param <T> the type of the annotation
     */
    public static <T extends Annotation> T getAnnotationRecursively(Class<?> cls, Class<T> annotation){
        if(cls == null) return null;
        if(cls.isAnnotationPresent(annotation)) return cls.getAnnotation(annotation);
        return getAnnotationRecursively(cls.getSuperclass(), annotation);
    }
}
