package olliekrk.elevators.requests;

/**
 * Represents types of requests supported by {@link olliekrk.elevators.ElevatorsSystem}.
 */
public enum RequestType {
    /**
     * External request for pickup with UP direction.
     */
    UP,
    /**
     * External request for pickup with DOWN direction.
     */
    DOWN,
    /**
     * Internal request for elevator to stop on specified floor.
     */
    FLOOR,
    /**
     * Internal request for elevator to restart elevator's queue and go to specified floor.
     */
    RESTART,
    /**
     * External request for evacuation, causing every elevator to restart its queue and to go to floor 0.
     */
    EVACUATION
}
