package me.eigenraven.personalspace.api.cluster;

/**
 * Tells Personal Space whether the registered cluster integration has taken
 * ownership of the current operation.
 */
public enum PersonalSpaceClusterResult {
    /** Personal Space should continue with its normal local behavior. */
    CONTINUE,

    /** The integration has handled or deferred the operation. */
    HANDLED
}
