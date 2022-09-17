package om.self.beans;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.Map.Entry;

import org.reflections.Reflections;

public class ContextManager {
    private static ContextManager contextManager = new ContextManager();

    private String targetPackage = "com";
    private String profile = "default";
    private final Hashtable<Class<?>, Object> beans = new Hashtable<>();
    private final Set<Class<?>> beanClasses = new HashSet<>();

    private final Set<Class<?>> loadLast = new HashSet<>();

    public static ContextManager getInstance() {
        return contextManager;
    }

    public String getTargetPackage() {
        return targetPackage;
    }

    public void setTargetPackage(String targetPackage) {
        if(targetPackage == null) throw new IllegalArgumentException("target package can not be null");
        this.targetPackage = targetPackage;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        if(profile == null) throw new IllegalArgumentException("profile can not be null");
        this.profile = profile;
    }

    public Hashtable<Class<?>, Object> getBeans() {
        return beans;
    }

    public<T> T getBean(Class<T> cls){
        return (T) beans.get(cls);
    }

    public boolean hasBean(Class<?> cls){
        return beans.containsKey(cls);
    }

    public void addBean(Object instance){
        if(beans.containsKey(instance.getClass())) throw new IllegalArgumentException("A instance of '" + instance.getClass().getName() + "' already exists so bean could not be added");
        beans.put(instance.getClass(), instance);
    }

    public void addBean(Class<?> cls) {
        if(beans.containsKey(cls)) throw new IllegalArgumentException("A instance of '" + cls.getName() + "' already exists so bean could not be added");
        Object obj;
        try{
            obj = cls.getConstructor(ContextManager.class).newInstance(this);
        }
        catch (Exception e) {
            try {
                obj = cls.getConstructor().newInstance();
            } catch (NoSuchMethodException exception){
                throw new ExceptionInInitializerError("there is no valid constructor for " + cls.getName() + "\n[TIP] Add a no args constructor or a one arg constructor that takes ContextManager");
            } catch (Exception exception){
                throw new ExceptionInInitializerError("there was a problem when creating an instance of " + cls.getName());
            }
        }
        beans.put(cls, obj);
    }

    public Set<Class<?>> getBeanClasses() {
        return beanClasses;
    }

    public void addBeanClass(Class<?> cls){
        beanClasses.add(cls);
    }

    public void load() {
        Reflections ref = new Reflections(targetPackage);

        //load all beans
        for (Class<?> cls : ref.getTypesAnnotatedWith(Bean.class)){
            Profile profileAnnotation = cls.getAnnotation(Profile.class);
            if(profileAnnotation != null && !profile.equals(profileAnnotation.value())) continue;
            addBeanClass(cls);
            addBean(cls);
        }

        //auto wire beans
        Hashtable<Object, List<Method>> autoWireMethods = new Hashtable<>();
        for (Entry<Class<?>, Object> entry : beans.entrySet()) {
            Object val = entry.getValue();
            for(Method m : entry.getKey().getMethods())
                if(m.isAnnotationPresent(Autowired.class))
                    if(!autoWireMethods.containsKey(val))
                        autoWireMethods.put(val, new LinkedList<>(Collections.singletonList(m)));
                    else
                        autoWireMethods.get(val).add(m);
        }

        for(Entry<Object, List<Method>> entry : autoWireMethods.entrySet()){
            for(Method m : entry.getValue()) {
                List<Object> params = new LinkedList<>();
                for (Parameter p : m.getParameters()) {
                    Object obj = beans.get(p.getType());
                    params.add(obj);
                }
                try {
                    m.invoke(entry.getKey(), params);
                }catch (Exception e){}
            }
        }
    }
}