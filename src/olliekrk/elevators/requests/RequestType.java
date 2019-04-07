package olliekrk.elevators.requests;

/**
 * Represents types of requests {@link Request} supported by {@link olliekrk.elevators.ElevatorsSystem}.
 */
public enum RequestType {
    /**
     * External request for passenger pickup with UP direction.
     */
    UP,
    /**
     * External request for passenger pickup with DOWN direction.
     */
    DOWN,
    /**
     * Internal request for specific elevator to stop on specific floor.
     */
    FLOOR,
    /**
     * Internal request for elevator to restart elevator's queue.
     * Cancels all previously enqueued requests and makes elevator go to specific floor.
     */
    RESTART,
    /**
     * External request for evacuation.
     * Causes every elevator in the system to restart its queue and to go to the ground floor.
     */
    EVACUATION
}
