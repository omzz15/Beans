package om.self.beans.core;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BeanManager {

//    private static final BeanManager instance = new BeanManager();

    private BeanManagerSettings settings;

    /**
     * this stores all the beans and weather they are loaded in relation to the beans class
     */
    private final Hashtable<Class<?>, Map.Entry<Object, BeanLoad>> beans = new Hashtable<>();

    /**
     * this is the list of beans that have to be loaded when {@link BeanManager#load()} is called
     */
    private final Set<Object> loadingBeans = new HashSet<>();

    ///////////////
    //CONSTRUCTOR//
    ///////////////
    public BeanManager(){
        settings = new BeanManagerSettings();
    }

    public BeanManager(BeanManagerSettings settings) {
        this.settings = settings;
    }

    ///////////////////////
    //GETTERS and SETTERS//
    ///////////////////////

    //----------settings----------//
    public BeanManagerSettings getSettings() {
        return settings;
    }

    public void setSettings(BeanManagerSettings settings) {
        if (settings == null) throw new IllegalArgumentException("settings can not be null");
        this.settings = settings;
    }

    //----------beans----------//

    /**
     * adds a bean to the repository {@link BeanManager#beans}
     * @param bean the bean you want to add
     * @param shouldLoad whether the bean should load when {@link BeanManager#load()} is called
     * @param isLoaded whether the bean is already loaded(this prevents {@link BeanManager#loadBean(Object, Runnable)} from being called on the bean and will prevent it from being called in {@link BeanManager#load()} event if shouldLoad is true)
     */
    public void addBean(Object bean, Runnable loadFunction, boolean shouldLoad, boolean isLoaded){
        if(isBeanThere(bean.getClass())){
            if(isBeanThere(bean)) return;
            settings.getDuplicateBeanPolicy().throwError(getDuplicateBeanException(bean));
            return;
        }

        beans.put(bean.getClass(), new AbstractMap.SimpleEntry<>(bean, new BeanLoad(loadFunction, isLoaded)));
        if(shouldLoad && !isLoaded) loadingBeans.add(bean);
    }

    /**
     * will remove a bean from the repository {@link BeanManager#beans}, and {@link BeanManager#loadingBeans}
     * @param bean the bean you want to remove
     */
    public void removeBean(Object bean){
        removeBean(bean.getClass());
    }

    /**
     * will remove a bean from the repository {@link BeanManager#beans}, but not from {@link BeanManager#loadingBeans}, use {@link BeanManager#removeBean(Object)} to remove from loadedBeans as well
     * @param bean the bean you want to remove
     * @see BeanManager#removeBean(Object)
     */
    public void removeBean(Class<?> bean){
        beans.remove(bean);
    }

    /**
     * gives you a hashtable that links the bean classes to entries containing the bean object and weather it is loaded
     * @return all the beans
     */
    public Hashtable<Class<?>, Map.Entry<Object, BeanLoad>> getBeans(){
        return beans;
    }

    /**
     * checks if there is a loaded bean of the class beanCls
     * @param beanCls the class of the bean you want to check
     * @return false if there is no bean or if it is not loaded, true if the bean is loaded
     */
    public boolean isBeanLoaded(Class<?> beanCls){
        return isBeanThere(beanCls) && beans.get(beanCls).getValue().isLoaded;
    }

    /**
     * checks if the passed in bean is the same one as the one stored and if it is loaded
     * @param bean the bean you want to check
     * @return false if the bean doesn't match/exist or if it is not loaded, true if the bean is loaded
     */
    public boolean isBeanLoaded(Object bean){
        Class<? extends Object> beanCls = bean.getClass();
        return isBeanThere(beanCls) && beans.get(beanCls).getKey() == bean && beans.get(beanCls).getValue().isLoaded;
    }

    /**
     * checks if there is a bean of the specified class
     * @param beanCls the class you want to check
     * @return weather the bean is there
     */
    public boolean isBeanThere(Class<?> beanCls){
        return beans.containsKey(beanCls);
    }

    /**
     * checks if the exact bean is stored
     * @param bean the bean you want to check
     * @return weather the bean is there
     */
    public boolean isBeanThere(Object bean){
        Class<? extends Object> beanCls = bean.getClass();
        return isBeanThere(beanCls) && beans.get(beanCls).getKey() == bean;
    }

    ////////
    //Load//
    ////////
    //----------All----------//
    public void load(){
        loadingBeans.forEach(this::loadBeanInternal);
        loadingBeans.clear();
    }

    //----------Bean----------//
    public <T> T loadBean(T bean, Runnable loadFunction){
        addBean(bean, loadFunction, false, false);
        return loadBeanInternal(bean);
    }

    private<T> T loadBeanInternal(T bean){
        if(isBeanLoaded(bean)) return bean;

        beans.get(bean.getClass()).getValue().load();

        return bean;
    }

    //----------Parameter----------//

    /**
     * picks the best matched bean based on the input class(could return a subclass) and the settings from {@link BeanManagerSettings}. This is the preferred method to get a bean because it is the most flexible
     * @param cls the class of the bean you want
     * @param allowRawBean weather the bean can be raw(meaning not all @Autowired methods have been called)
     * @return the bean that best matches the input class
     * @param <T> the type of the bean
     */
    public <T> T getBestMatch(Class<T> cls, boolean allowRawBean){
        return getBestMatch(cls, allowRawBean, false);
    }

    /**
     * similar to {@link BeanManager#getBestMatch(Class, boolean)} but if no valid bean is found it will return an empty optional instead of throwing an error
     */
    public <T> Optional<T> tryGetBestMatch(Class<T> cls, boolean allowRawBean){
        T val = getBestMatch(cls, allowRawBean, true);
        if(val == null) return Optional.empty();
        return Optional.of(val);
    }

    /**
     * picks the best matched bean based on the input class(could return a subclass) and the settings from {@link BeanManagerSettings}. This is the preferred method to get a bean because it is the most flexible
     * @param cls the class of the bean you want
     * @param allowRawBean weather the bean can be raw(meaning not all @Autowired methods have been called)
     * @param allowNull weather it can return null if it cant find an appropriate bean. this method will throw an error if this is false, and it cant find a valid bean
     * @return the bean that best matches the input class
     * @param <T> the type of the bean
     */
    public  <T> T getBestMatch(Class<T> cls, boolean allowRawBean, boolean allowNull){
        switch (settings.getDuplicateAutoWireStrategy()){
            case FIRST :
                return getFirstMatch(cls, allowRawBean, allowNull, getWithType(cls, beans.values().stream().map(Map.Entry::getKey)).collect(Collectors.toList()));
            case RANDOM :
                return getRandomMatch(cls, allowRawBean, allowNull, getWithType(cls, beans.values().stream().map(Map.Entry::getKey)).collect(Collectors.toList()));
            case PROFILE :
                List<T> profiledBeans = getWithProfile(getWithType(cls, beans.values().stream().map(Map.Entry::getKey))).collect(Collectors.toList());

                if(profiledBeans.isEmpty()){
                    switch (settings.getNoProfileFallbackStrategy()){
                        case FIRST :
                            return getFirstMatch(cls, allowRawBean, allowNull, getWithType(cls, beans.values().stream().map(Map.Entry::getKey)).collect(Collectors.toList()));
                        case RANDOM :
                            return getRandomMatch(cls, allowRawBean, allowNull, getWithType(cls, beans.values().stream().map(Map.Entry::getKey)).collect(Collectors.toList()));
                        case EXCEPTION : throw new ExceptionInInitializerError("there were no beans of type " + cls.getName() + " with the profile '" + settings.getProfile() + "' \n[TIP] add a bean of the right type with a @Profile("+settings.getProfile()+") annotation or set noProfileFallbackStrategy to FIRST or RANDOM");
                    }
                }

                switch (settings.getDuplicateProfileFallbackStrategy()){
                    case FIRST :
                        return getFirstMatch(cls, allowRawBean, allowNull, profiledBeans);
                    case RANDOM :
                        return getRandomMatch(cls, allowRawBean, allowNull, profiledBeans);
                    case EXCEPTION :
                        if(profiledBeans.size() == 1)
                            return getFirstMatch(cls, allowRawBean, allowNull, profiledBeans);
                        throw new ExceptionInInitializerError("there were multiple beans of type " + cls.getName() + " with profile '" + settings.getProfile() + "' \n[TIP] remove beans by deleting or adding specific tags in @Bean so only one bean of the right type with the right profile is loaded. You could also set duplicateProfileFallbackStrategy to FIRST or RANDOM");
            }
        }

        throw new ExceptionInInitializerError("there was an unknown error trying to run getBestMatch(cls: " + cls + ", allowRaw: " + allowRawBean + ", allowNull: " + allowNull + ")");
    }

    private<T> T getFirstMatch(Class<T> cls, boolean allowRawBean, boolean allowNull, Collection<T> repo){
        //check for null
        if(repo.isEmpty())
            if(allowNull) return null;
            else throw getNoBeanForParamError(cls, "there were no beans of type " + cls);

        //try to find first loaded
        Optional<T> loadedBean = repo.stream().filter(this::isBeanLoaded).findFirst();
        if(loadedBean.isPresent()) return loadedBean.get();

        //pick unloaded first element
        if (allowRawBean) return repo.stream().findFirst().get();
        return loadBeanInternal(repo.stream().findFirst().get());
    }

    private<T> T getRandomMatch(Class<T> cls, boolean allowRawBean, boolean allowNull, Collection<T> repo){
        //check for null
        if(repo.isEmpty())
            if(allowNull) return null;
            else throw getNoBeanForParamError(cls, "there were no beans of the right type");

        //pick a random element
        if(allowRawBean) return getRandomElement(repo);
        return loadBeanInternal(getRandomElement(repo));
    }

    private<T> Stream<T> getWithType(Class<T> type, Stream<Object> repo){
        return repo.filter(type::isInstance).map(bean -> (T) bean);
    }

    private<T> Stream<T> getWithProfile(Stream<T> repo){
        return repo.filter((bean) -> containsProfile(bean.getClass()));
    }

    private<T> T getRandomElement (Collection<T> repo) {
        return repo.stream().skip((long)(Math.random() * repo.size())).findFirst().get();
    }


    //////////
    //Checks//
    //////////
    private boolean containsProfile(Class<?> bean){
        Profile profile;
        if(settings.recursivelyCheckForProfile)profile = Utils.getAnnotationRecursively(bean, Profile.class);
        else profile = bean.getAnnotation(Profile.class);
        return profile != null && profile.value().equals(settings.getProfile());
    }


    //////////
    //Errors//
    //////////
    private ExceptionInInitializerError getNoBeanForParamError(Class<?> cls, String reason) {
        return new ExceptionInInitializerError("there was no bean of '" + cls.getName() + "' for the following reason: " + reason);
    }

    private IllegalArgumentException getDuplicateBeanException(Object bean){
        return new IllegalArgumentException("A instance of '" + bean.getClass().getName() + "' already exists in beans so bean '"+ bean +"' could not be added");
    }
}
