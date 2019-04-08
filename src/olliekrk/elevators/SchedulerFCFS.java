package olliekrk.elevators;

import olliekrk.elevators.exceptions.ElevatorsSchedulerException;
import olliekrk.elevators.requests.Request;

import java.util.Collection;
import java.util.Comparator;

import static olliekrk.elevators.requests.RequestType.*;

/**
 * Scheduler implementing {@link ElevatorsScheduler} interface.
 * Responsible for enqueuing requests in the order given by FC-FS algorithm.
 * <p>
 * Follows the first-come, first-served (also known as FC-FS) rule.
 * Assigns the requests so that there is a guarantee that the earlier a request comes to be served, the earlier it will be served.
 */
public class SchedulerFCFS implements ElevatorsScheduler {
    /**
     * Enqueues request of type UP or DOWN to one chosen controller.
     * <p>
     * Follows the FC-FS rule:
     * First it calculates how many steps will it take for each elevator to reach requested floor, including steps required for opening and closing doors.
     * Then it always picks the controller which will reach requested floor first.
     *
     * @param request             pickup request to be enqueued
     * @param elevatorControllers elevator controllers available in the system
     * @throws ElevatorsSchedulerException if request can not be enqueued
     */
    @Override
    public void enqueuePickupRequest(Request request, Collection<ElevatorController> elevatorControllers) throws ElevatorsSchedulerException {
        if (elevatorControllers == null || elevatorControllers.isEmpty()) {
            throw new ElevatorsSchedulerException("Failed to find any elevator controller");
        }

        if (request.getRequestType() != UP && request.getRequestType() != DOWN) {
            throw new ElevatorsSchedulerException("Unsupported pickup request received to enqueue");
        }

        //find controller which will first reach requested floor
        ElevatorController chosenController = elevatorControllers
                .stream()
                .min(Comparator.comparingInt(controller -> controller.calculateStepsToReachFloor(request.getFloor())))
                .orElse(null);

        if (chosenController == null) {
            throw new ElevatorsSchedulerException("Failed to find any available elevator controller");
        }

        //check if requested floor is already enqueued
        boolean alreadyEnqueued = chosenController
                .getRequestsQueue()
                .stream()
                .anyMatch(enqueuedRequest ->
                        (request.getRequestType() == FLOOR && enqueuedRequest.getFloor().equals(request.getFloor())) || request.equals(enqueuedRequest));

        //if not then put it at the end of chosen controller's queue (FC-FS strategy)
        if (!alreadyEnqueued) {
            chosenController.getRequestsQueue().add(request);
        }
    }

    /**
     * Enqueues request of type FLOOR or RESTART to given controller's queue.
     * <p>
     * In case the requested floor is already in given controller's queue it does nothing.
     * Otherwise it adds the request at the end of the queue (FC-FS strategy)
     *
     * @param request    internal request to be enqueued
     * @param controller elevator controller which has sent the request
     * @throws ElevatorsSchedulerException if request can not be enqueued
     */
    @Override
    public void enqueueInternalRequest(Request request, ElevatorController controller) throws ElevatorsSchedulerException {
        if (controller == null) {
            throw new ElevatorsSchedulerException("Controller unavailable to proceed request");
        }
        if (request.getRequestType() == RESTART) {
            enqueueRestartRequest(request, controller);
            return;
        }
        if (request.getRequestType() != FLOOR) {
            throw new ElevatorsSchedulerException("Unsupported internal request received");
        }

        //check if requested floor is already enqueued
        boolean alreadyEnqueued = controller
                .getRequestsQueue()
                .stream()
                .anyMatch(enqueuedRequest -> enqueuedRequest.getFloor().equals(request.getFloor()));

        //if not then put it at the end (FC-FS strategy)
        if (!alreadyEnqueued)
            controller.getRequestsQueue().add(request);
    }
}
