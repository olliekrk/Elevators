package olliekrk.elevators;

import olliekrk.elevators.exceptions.ElevatorsSchedulerException;
import olliekrk.elevators.requests.Request;
import olliekrk.elevators.requests.RequestType;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Class being a part of an {@link ElevatorsSystem}.
 * Responsible for scheduling incoming requests - {@link Request} - to the available elevators.
 * Part of an "Strategy" design pattern.
 */
public interface ElevatorsScheduler {
    /**
     * Enqueues external requests of type UP and DOWN to chosen available elevator controller.
     *
     * @param request             pickup request to be enqueued
     * @param elevatorControllers elevator controllers available in the system
     * @throws ElevatorsSchedulerException when request could not be enqueued properly
     * @see RequestType for more detailed information
     */
    void enqueuePickupRequest(Request request, Collection<ElevatorController> elevatorControllers) throws ElevatorsSchedulerException;

    /**
     * Enqueues internal requests of type FLOOR and RESTART to given elevator controller.
     *
     * @param request    internal request to be enqueued
     * @param controller elevator controller which sent the request
     * @throws ElevatorsSchedulerException when request could not be enqueued properly
     * @see RequestType for more detailed information
     */
    void enqueueInternalRequest(Request request, ElevatorController controller) throws ElevatorsSchedulerException;

    /**
     * Enqueues request of type RESTART to given elevator controller.
     *
     * @param request    restart request to be enqueued
     * @param controller elevator controller to receive restart request
     * @throws ElevatorsSchedulerException when request has invalid type - other than RESTART
     * @see RequestType for more detailed information
     */
    default void enqueueRestartRequest(Request request, ElevatorController controller) throws ElevatorsSchedulerException {
        if (request == null || request.getRequestType() != RequestType.RESTART) {
            throw new ElevatorsSchedulerException("Invalid restart request received");
        }
        //replace all previously enqueued requests with restarted queue
        List<Request> restartedQueue = new LinkedList<>();
        restartedQueue.add(request);
        controller.setRequestsQueue(restartedQueue);
    }

    /**
     * Enqueues EVACUATION requests to every available elevator's controller
     *
     * @param request evacuation request to be enqueued
     * @param elevatorControllers collection of controllers to receive the request
     * @throws ElevatorsSchedulerException when request has invalid type - other than EVACUATION
     * @see RequestType for more detailed information
     */
    default void enqueueEvacuationRequest(Request request, Collection<ElevatorController> elevatorControllers) throws ElevatorsSchedulerException {
        if (request == null || request.getRequestType() != RequestType.EVACUATION) {
            throw new ElevatorsSchedulerException("Invalid evacuation request received");
        }
        //override existing requests with evacuation request
        for (ElevatorController controller : elevatorControllers) {
            List<Request> evacuationQueue = new LinkedList<>();
            evacuationQueue.add(request);
            controller.setRequestsQueue(evacuationQueue);
        }
    }
}
