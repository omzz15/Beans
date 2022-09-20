package om.self.beans;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.Map.Entry;

import org.reflections.Reflections;

public class BeanManager {
    private static final BeanManager beanManager = new BeanManager();

    //common settings
    private String targetPackage = "com";
    private String profile = "default";

    //policies and strategies
    private FailurePolicy duplicateBeanPolicy = FailurePolicy.EXCEPTION;
    private SelectionStrategy duplicateAutoWireStrategy = SelectionStrategy.PROFILE;
    private FallBackSelectionStrategy duplicateProfileFallbackStrategy = FallBackSelectionStrategy.EXCEPTION;
    private FallBackSelectionStrategy noProfileFallbackStrategy = FallBackSelectionStrategy.FIRST;

    //other stuff
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

    public SelectionStrategy getDuplicateAutoWireStrategy() {
        return duplicateAutoWireStrategy;
    }

    public void setDuplicateAutoWireStrategy(SelectionStrategy duplicateAutoWireStrategy) {
        if (duplicateAutoWireStrategy == null)
            throw new IllegalArgumentException("duplicateAutoWireStrategy can not be null");
        this.duplicateAutoWireStrategy = duplicateAutoWireStrategy;
    }

    public FallBackSelectionStrategy getDuplicateProfileFallbackStrategy() {
        return duplicateProfileFallbackStrategy;
    }

    public void setDuplicateProfileFallbackStrategy(FallBackSelectionStrategy duplicateProfileFallbackStrategy) {
        if (duplicateProfileFallbackStrategy == null)
            throw new IllegalArgumentException("duplicateProfileFallbackStrategy can not be null");
        this.duplicateProfileFallbackStrategy = duplicateProfileFallbackStrategy;
    }

    public FallBackSelectionStrategy getNoProfileFallbackStrategy() {
        return noProfileFallbackStrategy;
    }

    public void setNoProfileFallbackStrategy(FallBackSelectionStrategy noProfileFallbackStrategy) {
        if (noProfileFallbackStrategy == null)
            throw new IllegalArgumentException("noProfileFallbackStrategy can not be null");
        this.noProfileFallbackStrategy = noProfileFallbackStrategy;
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
            List<Object> validParams = new LinkedList<>(getParamsWithClass(repo, paramCls));
            Object bestParam = getParam(paramCls, validParams, includesRawBeans);
            if(bestParam != null)
                params.add(bestParam);
        }
        try {
            m.invoke(methodObj, params.toArray());
        }catch (Exception e){return false;}
        return true;
    }

    //throw new ExceptionInInitializerError("there are multiple beans with valid profiles for " + paramCls.getName() +"\n[TIP] remove one of the profiles or set duplicateAutoWirePolicy to PROFILE_WITH_FIRST or PROFILE_WITH_RANDOM");
    //throw new IllegalStateException("the duplicate auto-wire policy is not valid so parameter '" + paramCls.getName() + "' can not be loaded");
    private Object getParam(Class<?> paramCls, List<Object> params, boolean includesRawBeans){
        if(params.isEmpty()) return null;

        switch (duplicateAutoWireStrategy) {
            case FIRST -> {
                return params.get(0);
            }
            case RANDOM -> {
                if(includesRawBeans || getParamsWithClass(rawBeans.values(), paramCls).isEmpty()) return getRandomElement(params);
                return null;
            }
            case PROFILE -> {
                List<Object> profiledParams = getParamsWithProfile(params);

                if (includesRawBeans || !isParamWithProfile(getParamsWithClass(rawBeans.values(), paramCls))) {
                    if (profiledParams.isEmpty()){
                        switch (noProfileFallbackStrategy) {
                            case FIRST -> {return params.get(0);}
                            case RANDOM -> {return getRandomElement(params);}
                            case EXCEPTION -> throw new ExceptionInInitializerError("there are no beans with valid profiles for " + paramCls.getName() +"\n[TIP] add a bean with profile '"+ profile + "' or set noProfileFallbackStrategy to FIRST or RANDOM");
                        }
                    } else {
                        switch (duplicateProfileFallbackStrategy){
                            case FIRST -> {return profiledParams.get(0);}
                            case RANDOM -> {return getRandomElement(profiledParams);}
                            case EXCEPTION -> {
                                if(profiledParams.size() == 1) return profiledParams.get(0);
                                throw new ExceptionInInitializerError("there are multiple beans with valid profiles for " + paramCls.getName() +"\n[TIP] remove one of the '" + profile + "' profiles or set duplicateProfileFallbackStrategy to FIRST or RANDOM");
                            }
                        }
                    }
                }

                return null;
            }
            default -> throw new IllegalStateException("the duplicate auto-wire policy is not valid so parameter '" + paramCls.getName() + "' can not be loaded");
        }
    }

    private List<Object> getParamsWithProfile(Collection<Object> params){
        List<Object> profileParams = new LinkedList<>();
        for (Object param: params)
            if(param.getClass().isAnnotationPresent(Profile.class)) profileParams.add(param);
        return profileParams;
    }

    private boolean isParamWithProfile(Collection<Object> params){
        for (Object param: params)
            if(param.getClass().isAnnotationPresent(Profile.class)) return true;
        return false;
    }

    private List<Object> getParamsWithClass(Collection<Object> params, Class<?> cls){
        List<Object> classParams = new LinkedList<>();
        for(Object param : params)
            if(cls.isInstance(param)) classParams.add(param);
        return classParams;
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

    public enum SelectionStrategy {
        FIRST,
        RANDOM,
        PROFILE,
    }

    public enum FallBackSelectionStrategy{
        FIRST,
        RANDOM,
        EXCEPTION
    }
}