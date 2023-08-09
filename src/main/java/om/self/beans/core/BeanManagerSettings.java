package om.self.beans.core;

/**
 * Contains all the settings for the bean manager. <br>
 * <ul>
 *     <li>{@link #profile} the profile that will be used to load the beans (this can be used to load different beans with different profiles)</li>
 *     <li>{@link #duplicateBeanPolicy} the policy that will be used when a duplicate bean is found</li>
 *     <li>{@link #autoWireStrategy} the strategy that will be used when running {@link BeanManager#getBestMatch(Class, boolean, boolean)}</li>
 *     <li>{@link #duplicateProfileFallbackStrategy} the strategy that will be used to select a bean when a duplicate beans with the right profile are found</li>
 *     <li>{@link #noProfileFallbackStrategy} the strategy that will be used to select a bean when no beans with the right profile are found</li>
 *     <li>{@link #recursivelyCheckForProfile} weather to recursively check for the profile annotation in the super classes or not</li>
 * </ul>
 */
public class BeanManagerSettings {
    /**
     * The profile that will be used to load the beans (this can be used to load different beans with different profiles)
     */
    private String profile = "default";

    //policies and strategies
    /**
     * The policy that will be used when a duplicate bean is found
     */
    private FailurePolicy duplicateBeanPolicy = FailurePolicy.EXCEPTION;
    /**
     * The strategy that will be used when running {@link BeanManager#getBestMatch(Class, boolean, boolean)}
     */
    private SelectionStrategy autoWireStrategy = SelectionStrategy.PROFILE;
    /**
     * The strategy that will be used to select a bean when a duplicate beans with the right profile are found
     */
    private FallBackSelectionStrategy duplicateProfileFallbackStrategy = FallBackSelectionStrategy.EXCEPTION;
    /**
     * The strategy that will be used to select a bean when no beans with the right profile are found
     */
    private FallBackSelectionStrategy noProfileFallbackStrategy = FallBackSelectionStrategy.FIRST;

    /**
     * Weather to recursively check for the profile annotation in the super classes or not
     */
    public boolean recursivelyCheckForProfile = true;

    /**
     * gets the profile of the bean manager
     * @return {@link #profile}
     */
    public String getProfile() {
        return profile;
    }

    /**
     * sets the profile of the bean manager ({@link #profile})
     * @param profile the new profile
     * @throws IllegalArgumentException if the profile is null
     */
    public void setProfile(String profile) {
        if (profile == null) throw new IllegalArgumentException("profile can not be null");
        this.profile = profile;
    }

    /**
     * gets the policy that will be used when a duplicate bean is found
     * @return {@link #duplicateBeanPolicy}
     */
    public FailurePolicy getDuplicateBeanPolicy() {
        return duplicateBeanPolicy;
    }

    /**
     * sets the policy that will be used when a duplicate bean is found ({@link #duplicateBeanPolicy})
     * @param duplicateBeanPolicy the new policy
     * @throws IllegalArgumentException if the policy is null
     */
    public void setDuplicateBeanPolicy(FailurePolicy duplicateBeanPolicy) {
        if (duplicateBeanPolicy == null) throw new IllegalArgumentException("duplicateBeanPolicy can not be null");
        this.duplicateBeanPolicy = duplicateBeanPolicy;
    }

    /**
     * gets the strategy that will be used when running {@link BeanManager#getBestMatch(Class, boolean, boolean)}
     * @return {@link #autoWireStrategy}
     */
    public SelectionStrategy getAutoWireStrategy() {
        return autoWireStrategy;
    }

    /**
     * sets the strategy that will be used when running {@link BeanManager#getBestMatch(Class, boolean, boolean)} ({@link #autoWireStrategy})
     * @param autoWireStrategy the new strategy
     * @throws IllegalArgumentException if the strategy is null
     */
    public void setAutoWireStrategy(SelectionStrategy autoWireStrategy) {
        if (autoWireStrategy == null)
            throw new IllegalArgumentException("duplicateAutoWireStrategy can not be null");
        this.autoWireStrategy = autoWireStrategy;
    }

    /**
     * gets the strategy that will be used to select a bean when a duplicate beans with the right profile are found
     * @return {@link #duplicateProfileFallbackStrategy}
     */
    public FallBackSelectionStrategy getDuplicateProfileFallbackStrategy() {
        return duplicateProfileFallbackStrategy;
    }

    /**
     * sets the strategy that will be used to select a bean when a duplicate beans with the right profile are found ({@link #duplicateProfileFallbackStrategy})
     * @param duplicateProfileFallbackStrategy the new strategy
     * @throws IllegalArgumentException if the strategy is null
     */
    public void setDuplicateProfileFallbackStrategy(FallBackSelectionStrategy duplicateProfileFallbackStrategy) {
        if (duplicateProfileFallbackStrategy == null)
            throw new IllegalArgumentException("duplicateProfileFallbackStrategy can not be null");
        this.duplicateProfileFallbackStrategy = duplicateProfileFallbackStrategy;
    }

    /**
     * gets the strategy that will be used to select a bean when no beans with the right profile are found
     * @return {@link #noProfileFallbackStrategy}
     */
    public FallBackSelectionStrategy getNoProfileFallbackStrategy() {
        return noProfileFallbackStrategy;
    }

    /**
     * sets the strategy that will be used to select a bean when no beans with the right profile are found ({@link #noProfileFallbackStrategy})
     * @param noProfileFallbackStrategy the new strategy
     * @throws IllegalArgumentException if the strategy is null
     */
    public void setNoProfileFallbackStrategy(FallBackSelectionStrategy noProfileFallbackStrategy) {
        if (noProfileFallbackStrategy == null)
            throw new IllegalArgumentException("noProfileFallbackStrategy can not be null");
        this.noProfileFallbackStrategy = noProfileFallbackStrategy;
    }

    /**
     * What to do when a failure occurs (used in {@link #duplicateBeanPolicy})
     */
    public enum FailurePolicy{
        /**
         * No action will be taken
         */
        QUIET,
        /**
         * An exception will be thrown
         */
        EXCEPTION;

        /**
         * throws the exception if the policy is {@link #EXCEPTION}
         * @param e the exception to throw
         * @param <T> the type of the exception
         * @throws T if the policy is {@link #EXCEPTION}
         */
        <T extends Throwable> void throwError(T e) throws T {
            if(this == EXCEPTION) throw e;
        }
    }

    /**
     * How to select bean when {@link BeanManager#getBestMatch(Class, boolean, boolean)} is called
     */
    public enum SelectionStrategy {
        /**
         * Select the first bean that matches the criteria
         */
        FIRST,
        /**
         * Select a random bean that matches the criteria
         */
        RANDOM,
        /**
         * Select the bean that matches the profile
         */
        PROFILE,
    }

    /**
     * How to select a bean when the primary selection strategy fails
     */
    public enum FallBackSelectionStrategy{
        /**
         * Select the first bean that matches the criteria
         */
        FIRST,
        /**
         * Select a random bean that matches the criteria
         */
        RANDOM,
        /**
         * Throw an exception
         */
        EXCEPTION
    }
}
