package om.self.beans;

import om.self.beans.core.BeanCore;
import om.self.beans.core.Profile;
import org.reflections.Reflections;

import java.util.*;

import static om.self.beans.core.Utils.getAnnotationRecursively;

public class PackageBeanManager extends BeanCore{

    private String targetPackage = "com";
    private final Set<String> tags = new HashSet<>(Collections.singleton("default"));

    public PackageBeanManager(){}

    public PackageBeanManager(String targetPackage, String... tags){
        setTargetPackage(targetPackage);
        Arrays.stream(tags).forEach(this::addTag);
    }

    ///////////////////////
    //GETTERS and SETTERS//
    ///////////////////////
    public Set<String> getTags() {
        return tags;
    }

    public void addTag(String tag){
        if(tag == null) throw new IllegalArgumentException("tag can not be null");
        tags.add(tag);
    }

    public void removeTag(String tag){
        tags.remove(tag);
    }

    public void removeDefaultTag(){
        tags.remove("default");
    }

    public String getTargetPackage() {
        return targetPackage;
    }

    public void setTargetPackage(String targetPackage) {
        if (targetPackage == null) throw new IllegalArgumentException("targetPackage can not be null");
        this.targetPackage = targetPackage;
    }


    ///////////
    //loading//
    ///////////
    public void load(String targetPackage){
        new Reflections(targetPackage).getTypesAnnotatedWith(Bean.class).stream()
                .filter(this::isBeanLoadable)
                .forEach((bean) -> addBean(makeInstance(bean), getAnnotationRecursively(bean, Bean.class).alwaysLoad(), false));
        super.load();
    }

    @Override
    public void load(){
        load(targetPackage);
    }

    /**
     * checks if a bean has a valid profile and tags to be loaded with the current settings
     * @param bean the class of the bean you want to check
     * @return if the bean is valid
     */
    private boolean isBeanLoadable(Class<?> bean){
        return containsTag(getAnnotationRecursively(bean, Bean.class)) && isProfileValid(bean);
    }

    private boolean containsTag(Bean bean){
        return Arrays.stream(bean.tags()).anyMatch(tags::contains);
    }

    private boolean isProfileValid(Class<?> bean){
        return !bean.isAnnotationPresent(Profile.class) || Objects.equals(bean.getAnnotation(Profile.class).value(), getSettings().getProfile());
    }

    private Object makeInstance(Class<?> cls){
        Object obj;
        try{
            obj = cls.getConstructor(BeanCore.class).newInstance(this);
        }
        catch (Exception e) {
            try {
                obj = cls.getConstructor().newInstance();
            } catch (NoSuchMethodException exception){
                throw new ExceptionInInitializerError("there is no valid constructor for " + cls.getName() + "\n[TIP] Add a no args constructor or a one arg constructor that takes BeanCore or PackageBeanManager");
            } catch (Exception exception){
                throw new ExceptionInInitializerError("there was a problem when creating an instance of " + cls.getName());
            }
        }
        return obj;
    }
}