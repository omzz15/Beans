package om.self.beans;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.Map.Entry;

import org.reflections.Reflections;

public class BeanManager {
    private static final BeanManager beanManager = new BeanManager();

    private String targetPackage = "com";
    private String profile = "default";
    private final Hashtable<Class<?>, Object> beans = new Hashtable<>();
    private final Hashtable<Class<?>, Object> rawBeans = new Hashtable<>();
    private final Set<Class<?>> beanClasses = new HashSet<>();


    public static BeanManager getInstance() {
        return beanManager;
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

    public void addBean(Object bean, boolean runInLoad){
        Class<?> cls = bean.getClass();
        if(runInLoad)
            if(rawBeans.containsKey(cls)) throw new IllegalArgumentException("A instance of '" + cls.getName() + "' already exists in rawBeans so bean could not be added");
            else rawBeans.put(cls, bean);
        else
        if(beans.containsKey(cls)) throw new IllegalArgumentException("A instance of '" + cls.getName() + "' already exists in beans so bean could not be added");
        else beans.put(cls, bean);
    }

    /**
     * method to instantiate a bean based on class
     * @param runInLoad weather to run dependency injection during {@link BeanManager#load()}
     * @param cls the class of the bean you want to instantiate
     */
    public void addBean(Class<?> cls, boolean runInLoad) {
        addBean(makeRawBean(cls), runInLoad);
    }

    private Object makeRawBean(Class<?> cls){
        Object obj;
        try{
            obj = cls.getConstructor(BeanManager.class).newInstance(this);
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
        return obj;
    }

    /**
     * get the classes of the beans that are going to be loaded. if you want to get the classes of loaded beans call {@link BeanManager#getBeans()} and get the keySet
     * @return the class of pending beans
     */
    public Set<Class<?>> getBeanClasses() {
        return beanClasses;
    }

    public void addBeanClass(Class<?> cls){
        beanClasses.add(cls);
    }

    public void load() {
        Reflections ref = new Reflections(targetPackage);

        //load all bean classes
        for (Class<?> cls : ref.getTypesAnnotatedWith(Bean.class)){
            Profile profileAnnotation = cls.getAnnotation(Profile.class);
            if(profileAnnotation != null && !profile.equals(profileAnnotation.value())) continue;
            addBeanClass(cls);
        }

        //make raw beans
        beanClasses.forEach((cls) -> addBean(cls, true));
        beanClasses.clear();

        //get auto wire methods
        Hashtable<Object, List<Method>> autoWireMethods = new Hashtable<>();
        rawBeans.forEach((cls, obj) -> autoWireMethods.put(obj, getAutoWireMethods(cls)));

        //auto wire
        while (!autoWireMethods.isEmpty()){
            List<Object> completedBeans = new LinkedList<>();
            //try to wire all methods in a bean
            for (Entry<Object, List<Method>> entry : autoWireMethods.entrySet()) {
                Object key = entry.getKey();
                entry.getValue().removeIf(m -> loadMethod(m, key, beans.values()));
                if (entry.getValue().isEmpty())
                    completedBeans.add(key);
            }
            //remove completed beans from raw and add them to the final bean list
            for (Object bean: completedBeans) {
                autoWireMethods.remove(bean);
                rawBeans.remove(bean.getClass());
                addBean(bean, false);
            }
            //if no new beans were completed then it will run forever
            if(completedBeans.isEmpty()) throw new ExceptionInInitializerError("unable to auto-wire the following: " + autoWireMethods.entrySet() + "\n[TIP] check for circular dependencies or dependencies that arent loaded");
        }
    }

    private List<Method> getAutoWireMethods(Class<?> cls){
        List<Method> methods = new LinkedList<>();
        for(Method m : cls.getMethods())
            if(m.isAnnotationPresent(Autowired.class))
                methods.add(m);
        return methods;
    }

    private boolean loadMethod(Method m, Object methodObj, Collection<Object> repo){
        List<Object> params = new LinkedList<>();
        for (Parameter param : m.getParameters()) {
            Class<?> paramCls = param.getType();
            for (Object obj : repo){
                if (paramCls.isInstance(obj)) {
                    params.add(obj);
                    break;
                }
            }
        }
        try {
            m.invoke(methodObj, params.toArray());
        }catch (Exception e){return false;}
        return true;
    }
}