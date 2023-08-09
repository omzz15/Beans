package om.self.beans.core;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is the main class that handles all the beans, loading them,
 * and finding the best match based on the profile and settings
 */
public class BeanManager {

    /**
     * this is the settings for the bean manager including how to handle duplicates or missing beans
     */
    private BeanManagerSettings settings;

    /**
     * this stores all the beans and weather they are loaded in relation to the class of the bean
     */
    private final Hashtable<Class<?>, Map.Entry<Object, BeanLoad>> beans = new Hashtable<>();

    /**
     * this is the list of beans that have to be loaded when {@link BeanManager#load()} is called
     */
    private final Set<Object> loadingBeans = new HashSet<>();

    /**
     * Stores all the beans that are currently being loaded to prevent infinite loops
     */
    private final Set<Class<?>> inLoading = new HashSet<>();

    ///////////////
    //CONSTRUCTOR//
    ///////////////

    /**
     * Default constructor that has default settings
     */
    public BeanManager(){
        settings = new BeanManagerSettings();
    }

    /**
     * constructor that sets custom settings
     * @param settings the settings you want to use
     */
    public BeanManager(BeanManagerSettings settings) {
        this.settings = settings;
    }

    ///////////////////////
    //GETTERS and SETTERS//
    ///////////////////////

    //----------settings----------//

    /**
     * gets the settings for the bean manager
     * @return {@link #settings}
     */
    public BeanManagerSettings getSettings() {
        return settings;
    }

    /**
     * sets the settings for the bean manager
     * @param settings the settings you want to use
     */
    public void setSettings(BeanManagerSettings settings) {
        if (settings == null) throw new IllegalArgumentException("settings can not be null");
        this.settings = settings;
    }

    //----------beans----------//

    /**
     * adds a bean to the repository {@link BeanManager#beans}
     * @param bean the bean you want to add
     * @param shouldLoad whether the bean should load when {@link BeanManager#load()} is called
     * @param isLoaded whether the bean is already loaded (this prevents {@link BeanManager#loadBean(Object, Runnable)} from being called on the bean and will prevent it from being called in {@link BeanManager#load()} event if shouldLoad is true)
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
    /**
     * loads all the beans that have been added to {@link #loadingBeans}
     * (basically all the beans
     * that have been added with {@link #addBean(Object, Runnable, boolean, boolean)} with shouldLoad set to true
     */
    public void load(){
        for (Object bean: loadingBeans) {
            try{
                loadBeanInternal(bean);
            } catch (StackOverflowError e){
                throw new StackOverflowError("StackOverflowError while loading '" + bean + "' of " + bean.getClass() + ", this is most likely caused by a circular dependency.\nTIP: try settings allow raw beans to true in the code that loads this bean (load function)");
            }
        }

        loadingBeans.clear();
    }

    //----------Bean----------//

    /**
     * This will add a bean using {@link #addBean(Object, Runnable, boolean, boolean)} then immediately load it with {@link #loadBeanInternal(Object)}
     * @param bean the bean you want to add and load
     * @param loadFunction the function that will be called when the bean is loaded
     * @return the bean that was passed in after it is loaded
     * @param <T> the type of the bean
     */
    public <T> T loadBean(T bean, Runnable loadFunction){
        addBean(bean, loadFunction, false, false);
        return loadBeanInternal(bean);
    }

    /**
     * This will run the load function (from {@link BeanLoad#load()}) for the bean if it is not already loaded
     * @param bean the bean you want to load
     * @return the bean that was passed in after it is loaded
     * @param <T> the type of the bean
     */
    private<T> T loadBeanInternal(T bean){
        if(isBeanLoaded(bean)) return bean;

        inLoading.add(bean.getClass());
        beans.get(bean.getClass()).getValue().load();
        inLoading.remove(bean.getClass());

        return bean;
    }

    //----------Parameter----------//

    /**
     * An implementation on {@link #getBestMatch(Class, boolean, boolean)} that will throw an error if no valid bean is found
     * @param cls the class of the bean you want (it may return a subclass)
     * @param allowRawBean weather the bean can be raw (meaning the bean has not been loaded yet)
     * @return the bean that best matches the input class
     * @param <T> the type of the bean
     */
    public <T> T getBestMatch(Class<T> cls, boolean allowRawBean){
        return getBestMatch(cls, allowRawBean, false);
    }

    /**
     * Similar to {@link BeanManager#getBestMatch(Class, boolean)} but
     * if no valid bean is found it will return an empty optional instead of throwing an error.
     *
     * @param cls the class of the bean you want (it may return a subclass)
     * @param allowRawBean weather the bean can be raw (meaning the bean has not been loaded yet)
     * @param <T> the type of the bean
     * @return an optional containing the bean that best matches the input class, or an empty optional if no valid bean is found
     */
    public <T> Optional<T> tryGetBestMatch(Class<T> cls, boolean allowRawBean){
        T val = getBestMatch(cls, allowRawBean, true);
        if(val == null) return Optional.empty();
        return Optional.of(val);
    }

    /**
     * Picks the best matched bean based on the input class
     * (could return a subclass) and the settings from {@link BeanManagerSettings}.
     * This is the preferred method to get a bean because it is the most flexible
     * @param cls the class of the bean you want
     * @param allowRawBean weather the bean can be raw (meaning the bean has not been loaded yet)
     * @param allowNull weather it can return null if it cant find an appropriate bean. this method will throw an error if this is false, and it cant find a valid bean
     * @return the bean that best matches the input class
     * @param <T> the type of the bean
     */
    public  <T> T getBestMatch(Class<T> cls, boolean allowRawBean, boolean allowNull){
        switch (settings.getAutoWireStrategy()){
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

    /**
     * Used to get the first valid bean in a collection of beans (repo)
     * @param cls the class of the bean you want
     * @param allowRawBean weather the bean can be raw (meaning the bean has not been loaded yet)
     * @param allowNull weather it can return null if it cant find an appropriate bean (if false it will throw an error)
     * @param repo the collection of beans to search through
     * @return the first valid bean in the collection
     * @param <T> the type of the bean
     */
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

    /**
     * Used to get a random valid bean in a collection of beans (repo)
     * @param cls the class of the bean you want
     * @param allowRawBean weather the bean can be raw (meaning the bean has not been loaded yet)
     * @param allowNull weather it can return null if it cant find an appropriate bean (if false it will throw an error if a bean is not found)
     * @param repo the collection of beans to search through
     * @return a random valid bean in the collection
     * @param <T> the type of the bean
     */
    private<T> T getRandomMatch(Class<T> cls, boolean allowRawBean, boolean allowNull, Collection<T> repo){
        //check for null
        if(repo.isEmpty())
            if(allowNull) return null;
            else throw getNoBeanForParamError(cls, "there were no beans of the right type");

        //pick a random element
        if(allowRawBean) return getRandomElement(repo);
        return loadBeanInternal(getRandomElement(repo));
    }

    /**
     * Method to filter a stream of objects to a stream of a specific type
     * @param type the type to filter to
     * @param repo the stream of objects to filter
     * @return a stream of the type specified
     * @param <T> the type to filter to
     */
    private<T> Stream<T> getWithType(Class<T> type, Stream<Object> repo){
        return repo.filter(type::isInstance).map(bean -> (T) bean);
    }

    /**
     * Method to filter a stream to a stream that have the profile tag
     * @param repo the stream to filter
     * @return a stream of beans that have the profile tag
     * @param <T> the type of the stream to filter
     */
    private<T> Stream<T> getWithProfile(Stream<T> repo){
        return repo.filter((bean) -> containsProfile(bean.getClass()));
    }

    /**
     * Method to get a random element from a collection
     * @param repo the collection to get the element from
     * @return a random element from the collection
     * @param <T> the type of the collection
     */
    private<T> T getRandomElement (Collection<T> repo) {
        return repo.stream().skip((long)(Math.random() * repo.size())).findFirst().get();
    }


    //////////
    //Checks//
    //////////

    /**
     * Method to check if a bean has a profile tag
     * @param bean the bean class to check
     * @return weather the bean has a profile tag
     */
    private boolean containsProfile(Class<?> bean){
        Profile profile;
        if(settings.recursivelyCheckForProfile)profile = Utils.getAnnotationRecursively(bean, Profile.class);
        else profile = bean.getAnnotation(Profile.class);
        return profile != null && profile.value().equals(settings.getProfile());
    }


    //////////
    //Errors//
    //////////

    /**
     * Method to create an error for when a bean is not found
     * @param cls the class of the bean that was not found
     * @param reason the reason the bean was not found
     * @return an error with the class and reason
     */
    private ExceptionInInitializerError getNoBeanForParamError(Class<?> cls, String reason) {
        return new ExceptionInInitializerError("there was no bean of '" + cls.getName() + "' for the following reason: " + reason);
    }

    /**
     * Method to create an error for when duplicate beans are found
     * @param bean the bean that is a duplicate
     * @return an error with the bean
     */
    private IllegalArgumentException getDuplicateBeanException(Object bean){
        return new IllegalArgumentException("A instance of '" + bean.getClass().getName() + "' already exists in beans so bean '"+ bean +"' could not be added");
    }
}
