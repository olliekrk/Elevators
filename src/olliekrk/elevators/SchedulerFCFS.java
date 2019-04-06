package olliekrk.elevators;

import olliekrk.elevators.exceptions.ElevatorsSchedulerException;
import olliekrk.elevators.requests.Request;

import java.util.Collection;

/**
 * Scheduler implementing {@link ElevatorsScheduler} interface.
 * Responsible for enqueuing requests in the order given by FC-FS algorithm.
 * <p>
 * Follows the first-come, first-served (also known as FC-FS) rule.
 * Assigns the requests so that there is a guarantee that a request which came first to be served will be served first no matter what.
 */
public class SchedulerFCFS implements ElevatorsScheduler {
    @Override
    public void enqueuePickupRequest(Request request, Collection<ElevatorController> elevatorControllers) throws ElevatorsSchedulerException {
        if (elevatorControllers == null || elevatorControllers.isEmpty()) {
            throw new ElevatorsSchedulerException("Failed to find any elevator controller");
        }
        switch (request.getRequestType()) {
            case UP:
            case DOWN:

                int stepsRequired;
                int minimumStepsRequired = 0;
                ElevatorController chosenController = null;

                for (ElevatorController controller : elevatorControllers) {
                    stepsRequired = controller.calculateStepsToReachFloor(request.getFloor());
                    if (chosenController == null || stepsRequired < minimumStepsRequired) {
                        minimumStepsRequired = stepsRequired;
                        chosenController = controller;
                    }
                }
                if (chosenController == null) {
                    throw new ElevatorsSchedulerException("Failed to find any available elevator controller");
                }
                //check if requested pickup-floor is already enqueued
                boolean alreadyEnqueued = chosenController
                        .getRequestsQueue()
                        .stream()
                        .anyMatch(enqueuedRequest -> enqueuedRequest.getFloor().equals(request.getFloor()));

                //if not then put it at the end of chosen controller's queue (FC-FS strategy)
                if (!alreadyEnqueued) {
                    chosenController.getRequestsQueue().add(request);
                }
                break;
            default:
                throw new ElevatorsSchedulerException("Unsupported pickup request received");
        }
    }

    @Override
    public void enqueueInternalRequest(Request request, ElevatorController controller) throws ElevatorsSchedulerException {
        if (controller == null) {
            throw new ElevatorsSchedulerException("Controller unavailable to proceed request");
        }
        switch (request.getRequestType()) {
            case FLOOR:
                //check if requested floor is already enqueued
                boolean alreadyEnqueued = controller
                        .getRequestsQueue()
                        .stream()
                        .anyMatch(enqueuedRequest -> enqueuedRequest.getFloor().equals(request.getFloor()));

                //if not then put it at the end (FC-FS strategy)
                if (!alreadyEnqueued)
                    controller.getRequestsQueue().add(request);

                break;
            case RESTART:
                enqueueRestartRequest(request, controller);
                break;
            default:
                throw new ElevatorsSchedulerException("Unsupported internal request received");
        }
    }
}
