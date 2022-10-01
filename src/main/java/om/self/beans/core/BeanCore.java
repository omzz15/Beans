package om.self.beans.core;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Stream;

public class BeanCore {

    private BeanCoreSettings settings;

    /**
     * this stores all the beans and weather they are loaded in relation to the beans class
     */
    private final Hashtable<Class<?>, Map.Entry<Object, Boolean>> beans = new Hashtable<>();

    /**
     * this is the list of beans that have to be loaded when {@link BeanCore#load()} is called
     */
    private final Set<Object> loadingBeans = new HashSet<>();

    ///////////////
    //CONSTRUCTOR//
    ///////////////
    public BeanCore(){
        settings = new BeanCoreSettings();
    }

    public BeanCore(BeanCoreSettings settings) {
        this.settings = settings;
    }

    ///////////////////////
    //GETTERS and SETTERS//
    ///////////////////////
    //----------settings----------//
    public BeanCoreSettings getSettings() {
        return settings;
    }

    public void setSettings(BeanCoreSettings settings) {
        if (settings == null) throw new IllegalArgumentException("settings can not be null");
        this.settings = settings;
    }

    //----------beans----------//

    /**
     * adds a bean to the repository {@link BeanCore#beans}
     * @param bean the bean you want to add
     * @param shouldLoad whether the bean should load when {@link BeanCore#load()} is called
     * @param isLoaded whether the bean is already loaded(this prevents {@link BeanCore#loadBean(Object)} from being called on the bean and will prevent it from being called in {@link BeanCore#load()} event if shouldLoad is true)
     */
    public void addBean(Object bean, boolean shouldLoad, boolean isLoaded){
        if(beans.containsKey(bean.getClass())){
            settings.getDuplicateBeanPolicy().throwError(getDuplicateBeanException(bean));
            return;
        }

        beans.put(bean.getClass(), new AbstractMap.SimpleEntry<>(bean, isLoaded));
        if(shouldLoad && !isLoaded) loadingBeans.add(bean);
    }

    /**
     * will remove a bean from the repository {@link BeanCore#beans}, and {@link BeanCore#loadingBeans}
     * @param bean the bean you want to remove
     */
    public void removeBean(Object bean){
        removeBean(bean.getClass());
    }

    /**
     * will remove a bean from the repository {@link BeanCore#beans}, but not from {@link BeanCore#loadingBeans}, use {@link BeanCore#removeBean(Object)} to remove from loadedBeans as well
     * @param bean the bean you want to remove
     * @see BeanCore#removeBean(Object)
     */
    public void removeBean(Class<?> bean){
        beans.remove(bean);
    }

    public Object getBeans(Class<?> bean){
        return beans.get(bean).getValue();
    }

    public Optional<Object> getLoadedBean(Class<?> bean){
        if(beans.get(bean).getValue()) return Optional.of(beans.get(bean).getKey());
        return Optional.empty();
    }

    public Hashtable<Class<?>, Map.Entry<Object, Boolean>> getBeans(){
        return beans;
    }

    public boolean isBeanLoaded(Class<?> bean){
        return beans.get(bean).getValue();
    }
    public boolean isBeanLoaded(Object bean){
        return isBeanLoaded(bean.getClass());
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
    public Object loadBean(@Nonnull Object bean){
        if(!beans.containsKey(bean.getClass())) beans.put(bean.getClass(), new AbstractMap.SimpleEntry<>(bean, false));
        return loadBeanInternal(bean);
    }

    private Object loadBeanInternal(@Nonnull Object bean){
        if(isBeanLoaded(bean)) return bean;

        for (Method m: getAutoWireMethods(bean.getClass())){
            loadMethod(m, bean);
        }

        beans.get(bean.getClass()).setValue(true);

        return bean;
    }

    //----------Method----------//
    private void loadMethod(Method m, Object bean){
        List<Object> vals = new LinkedList<>();
        for(Parameter param : m.getParameters()) {
            try {
                if (param.isAnnotationPresent(ParamSettings.class)) {
                    ParamSettings paramSettings = param.getAnnotation(ParamSettings.class);
                    vals.add(getBestMatch(param, paramSettings.allowRawBean(), paramSettings.allowNull()));
                } else vals.add(getBestMatch(param, false, false));
            } catch (StackOverflowError e){
                throw new StackOverflowError("getBestMatch() on method '"+ m.getName() +"' in " + bean.getClass() + " and parameter '"+ param.getName() + "' has hit a stack overflow most likely because of a recursion error.\n[TIP] try checking for circular dependencies in the params of the method or set allowRaw to true with @ParamSettings");
            }
        }

        try {
            m.invoke(bean, vals.toArray());
        } catch(Exception e){
            throw new IllegalStateException("failed to load method '" + m.getName() + "' with bean '" + bean + "'", e);
        }
    }

    private List<Method> getAutoWireMethods(Class<?> cls){
        return Arrays.stream(cls.getMethods()).filter((m) -> m.isAnnotationPresent(Autowired.class)).toList();
    }

    //----------Parameter----------//
    private Object getBestMatch(Parameter parameter, boolean allowRawBean, boolean allowNull){
        switch (settings.getDuplicateAutoWireStrategy()){
            case FIRST -> {
                return getFirstMatch(parameter, allowRawBean, allowNull, getWithType(parameter.getType(), beans.values().stream().map(Map.Entry::getKey)).toList());
            }
            case RANDOM -> {
                return getRandomMatch(parameter, allowRawBean, allowNull, getWithType(parameter.getType(), beans.values().stream().map(Map.Entry::getKey)).toList());
            }
            case PROFILE -> {
                List<Object> profiledBeans = getWithProfile(getWithType(parameter.getType(), beans.values().stream().map(Map.Entry::getKey))).toList();

                if(profiledBeans.isEmpty()){
                    switch (settings.getNoProfileFallbackStrategy()){
                        case FIRST -> {
                            return getFirstMatch(parameter, allowRawBean, allowNull, getWithType(parameter.getType(), beans.values().stream().map(Map.Entry::getKey)).toList());
                        }
                        case RANDOM -> {
                            return getRandomMatch(parameter, allowRawBean, allowNull, getWithType(parameter.getType(), beans.values().stream().map(Map.Entry::getKey)).toList());
                        }
                        case EXCEPTION -> {
                            throw new ExceptionInInitializerError("there were no beans of type " + parameter.getType().getName() + " with the profile '" + settings.getProfile() + "' \n[TIP] add a bean of the right type with a @Profile("+settings.getProfile()+") annotation or set noProfileFallbackStrategy to FIRST or RANDOM");
                        }
                    }
                }

                switch (settings.getDuplicateProfileFallbackStrategy()){
                    case FIRST -> {
                        return getFirstMatch(parameter, allowRawBean, allowNull, profiledBeans);
                    }
                    case RANDOM -> {
                        return getRandomMatch(parameter, allowRawBean, allowNull, profiledBeans);
                    }
                    case EXCEPTION -> {
                        if(profiledBeans.size() == 1)
                            return getFirstMatch(parameter, allowRawBean, allowNull, profiledBeans);
                        throw new ExceptionInInitializerError("there were multiple beans of type " + parameter.getType().getName() + " with profile '" + settings.getProfile() + "' \n[TIP] remove beans by deleting or adding specific tags in @Bean so only one bean of the right type with the right profile is loaded. You could also set duplicateProfileFallbackStrategy to FIRST or RANDOM");
                    }
                }
            }
        }

        throw new ExceptionInInitializerError("there was an unknown error trying to run getBestMatch(param: " + parameter + ", allowRaw: " + allowRawBean + ", allowNull: " + allowNull + ")");
    }

    private Object getFirstMatch(Parameter parameter, boolean allowRawBean, boolean allowNull, Collection<Object> repo){
        //check for null
        if(repo.isEmpty())
            if(allowNull) return null;
            else throw getNoBeanForParamError(parameter, "there were no beans of type " + parameter.getType());

        //try to find first loaded
        Optional<Object> loadedBean = repo.stream().filter(this::isBeanLoaded).findFirst();
        if(loadedBean.isPresent()) return loadedBean.get();

        //pick unloaded first element
        if (allowRawBean) return repo.stream().findFirst().get();
        return loadBeanInternal(repo.stream().findFirst().get());
    }

    private Object getRandomMatch(Parameter parameter, boolean allowRawBean, boolean allowNull, Collection<Object> repo){
        //check for null
        if(repo.isEmpty())
            if(allowNull) return null;
            else throw getNoBeanForParamError(parameter, "there were no beans of type " + parameter.getType());

        //collect all beans
        List<Object> matches = getWithType(parameter.getType(), repo.stream()).toList();

        //pick a random element
        if(allowRawBean) return getRandomElement(matches);
        return loadBeanInternal(getRandomElement(matches));
    }

    private Stream<Object> getWithType(Class<?> type, Stream<Object> repo){
        return repo.filter(type::isInstance);
    }

    private Stream<Object> getWithProfile(Stream<Object> repo){
        return repo.filter((bean) -> containsProfile(bean.getClass()));
    }

    private Object getRandomElement (List<Object> list) {
        return list.get((int)(Math.random() * list.size()));
    }


    //////////
    //Checks//
    //////////
    private boolean containsProfile(Class<?> bean){
        Profile profile = Utils.getAnnotationRecursively(bean, Profile.class);
        return profile != null && profile.value().equals(settings.getProfile());
    }


    //////////
    //Errors//
    //////////
    private ExceptionInInitializerError getNoBeanForParamError(Parameter parameter, String reason) {
        return new ExceptionInInitializerError("the parameter '" + parameter.getName() + "' of '" + parameter.getType().getName() + "' was unable to find a valid bean for the following reason: " + reason);
    }

    private IllegalArgumentException getDuplicateBeanException(Object bean){
        return new IllegalArgumentException("A instance of '" + bean.getClass().getName() + "' already exists in beans so bean '"+ bean +"' could not be added");
    }
}
