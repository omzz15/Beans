package om.self.beans.core;


public class BeanManagerSettings {
    private String profile = "default";

    //policies and strategies
    private FailurePolicy duplicateBeanPolicy = FailurePolicy.EXCEPTION;
    private SelectionStrategy duplicateAutoWireStrategy = SelectionStrategy.PROFILE;
    private FallBackSelectionStrategy duplicateProfileFallbackStrategy = FallBackSelectionStrategy.EXCEPTION;
    private FallBackSelectionStrategy noProfileFallbackStrategy = FallBackSelectionStrategy.FIRST;

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        if (profile == null) throw new IllegalArgumentException("profile can not be null");
        this.profile = profile;
    }

    public FailurePolicy getDuplicateBeanPolicy() {
        return duplicateBeanPolicy;
    }

    public void setDuplicateBeanPolicy(FailurePolicy duplicateBeanPolicy) {
        if (duplicateBeanPolicy == null) throw new IllegalArgumentException("duplicateBeanPolicy can not be null");
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

    public enum FailurePolicy{
        QUIET,
        EXCEPTION;

        <T extends Throwable> void throwError(T e) throws T {
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
