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
    private FailurePolicy duplicateBeanPolicy = FailurePolicy.EXCEPTION;
    private DuplicateAutoWirePolicy duplicateAutoWirePolicy = DuplicateAutoWirePolicy.PROFILE_WITH_EXCEPTION;
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

    public FailurePolicy getDuplicateBeanPolicy() {
        return duplicateBeanPolicy;
    }

    public void setDuplicateBeanPolicy(FailurePolicy duplicateBeanPolicy) {
        if(duplicateBeanPolicy == null) throw new IllegalArgumentException("duplicateBeanPolicy can not be null");
        this.duplicateBeanPolicy = duplicateBeanPolicy;
    }

    public DuplicateAutoWirePolicy getDuplicateAutoWirePolicy() {
        return duplicateAutoWirePolicy;
    }

    public void setDuplicateAutoWirePolicy(DuplicateAutoWirePolicy duplicateAutoWirePolicy) {
        if(duplicateAutoWirePolicy == null) throw new IllegalArgumentException("duplicateAutoWirePolicy can not be null");
        this.duplicateAutoWirePolicy = duplicateAutoWirePolicy;
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
        if(runInLoad)
            addToRawBeans(bean);
        else
            addToBeans(bean);
    }

    private void addToRawBeans(Object bean){
        if(rawBeans.containsKey(bean.getClass()))
            duplicateBeanPolicy.run(new IllegalArgumentException("A instance of '" + bean.getClass().getName() + "' already exists in raw beans so bean could not be added"));
        else rawBeans.put(bean.getClass(), bean);
    }
    private void addToBeans(Object bean){
        if(beans.containsKey(bean.getClass()))
            duplicateBeanPolicy.run(new IllegalArgumentException("A instance of '" + bean.getClass().getName() + "' already exists in beans so bean could not be added"));
        else beans.put(bean.getClass(), bean);
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

        //create list of all beans
        Set<Object> allBeans = new HashSet<>(rawBeans.values());
        allBeans.addAll(beans.values());

        //auto wire
        while (!autoWireMethods.isEmpty()){
            List<Object> completedBeans = new LinkedList<>();
            //try to wire all methods in a bean
            for (Entry<Object, List<Method>> entry : autoWireMethods.entrySet()) {
                Object key = entry.getKey();
                entry.getValue().removeIf(m -> {
                    if(m.getAnnotation(Autowired.class).useRawBeans())
                        return loadMethod(m, key, allBeans, true);
                    return loadMethod(m, key, beans.values(), false);
                });
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

    private boolean loadMethod(Method m, Object methodObj, Collection<Object> repo, boolean includesRawBeans){
        List<Object> params = new LinkedList<>();
        for (Parameter param : m.getParameters()) {
            Class<?> paramCls = param.getType();
            List<Object> validParams = new LinkedList<>();
            for (Object obj : repo)
                if (paramCls.isInstance(param)) validParams.add(obj);

        }
        try {
            m.invoke(methodObj, params.toArray());
        }catch (Exception e){return false;}
        return true;
    }

    private Object getValidParam(Class<?> cls, List<Object> params, boolean includesRawBeans){
        if(!includesRawBeans){}
        else{}
        if(params.isEmpty()) return null;
        if(params.size() == 1) return params.get(0);
    }

    private Object getValidParam(List<Object> params){
        if(params.isEmpty()) return null;
        if(params.size() == 1 || duplicateAutoWirePolicy == DuplicateAutoWirePolicy.FIRST) return params.get(0);
        if(duplicateAutoWirePolicy == DuplicateAutoWirePolicy.RANDOM) return getRandomElement(params);

        List<Object> profiledParams = new LinkedList<>();
        for(Object param : params)
            if(param.getClass().isAnnotationPresent(Profile.class)) profiledParams.add(param);

        if(profiledParams.size() == 1) return profiledParams.get(0);


        if(duplicateAutoWirePolicy == DuplicateAutoWirePolicy.PROFILE_WITH_FIRST){
            if(profiledParams.isEmpty())
                return  params.get(0);
            return profiledParams.get(0);
        } else if(duplicateAutoWirePolicy == DuplicateAutoWirePolicy.PROFILE_WITH_RANDOM) {
            if(profiledParams.isEmpty())
                return getRandomElement(params);
            return getRandomElement(profiledParams);
        } else {
            if(profiledParams.isEmpty())
                return params.get(0);
            throw new ExceptionInInitializerError("there are multiple valid beans with profiles: " + params);
        }
    }

    private<T> T getRandomElement(List<T> list){
        return list.get((int)(list.size() * Math.random()));
    }

    public enum FailurePolicy{
        QUIET,
        EXCEPTION;

        public<T extends Throwable> void run(T e) throws T {
            if(this == EXCEPTION) throw e;
        }
    }

    public enum DuplicateAutoWirePolicy{
        FIRST,
        RANDOM,
        PROFILE_WITH_FIRST,
        PROFILE_WITH_RANDOM,
        PROFILE_WITH_EXCEPTION;

        public boolean useProfile(){
            return this == PROFILE_WITH_FIRST || this == PROFILE_WITH_RANDOM || this == PROFILE_WITH_EXCEPTION;
        }
    }
}