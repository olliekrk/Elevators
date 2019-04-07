package olliekrk.elevators;

import olliekrk.elevators.exceptions.ElevatorsSchedulerException;
import olliekrk.elevators.requests.Request;
import olliekrk.elevators.requests.RequestType;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static olliekrk.elevators.requests.RequestType.FLOOR;
import static olliekrk.elevators.requests.RequestType.RESTART;

/**
 * Scheduler implementing {@link ElevatorsScheduler} interface.
 * Responsible for enqueuing request in the order given by an algorithm.
 * The way it enqueues requests will be referred as "scanner" order.
 * <p>
 * Behaves in a similar way to "SCAN" (or even more "LOOK") disk scheduling algorithm.
 * <p>
 * When choosing an elevator to receive an external pickup request, it checks available elevators in the following order:
 * 1. Pick inactive elevator standing on the same floor that was requested.
 * 2. If there are none, pick closest elevator which is standing on other floor.
 * 3. If there are none, pick elevator which is moving towards the requested floor in the direction identical to requested direction.
 * 4. If there are none, for every elevator calculate which one will move towards requested floor first.
 * <p>
 *
 * <p>
 * Every times it has to assign a request to a chosen elevator, it checks if on a chosen queue, there are any requests pending in the elevator direction.
 * Then it places the request in the chosen place of a queue, so that the elevator will change its direction only after every request in its current direction is completed.
 */
public class SchedulerScanner implements ElevatorsScheduler {

    /**
     * Among all available elevator controllers chooses one which would be optimal to enqueue pickup request to.
     * <p>
     * When choosing an elevator to receive an external pickup request, it checks available elevators in the following order:
     * 1. Pick inactive elevator standing on the same floor that was requested.
     * 2. If there are none, pick closest elevator which is standing on other floor.
     * 3. If there are none, pick elevator which is moving towards the requested floor in the direction identical to requested direction.
     * 4. If there are none, for every elevator calculate which one will move towards requested floor first.
     *
     * @param request             pickup request to be enqueued
     * @param elevatorControllers elevator controllers available in the system
     * @throws ElevatorsSchedulerException when the request can not be enqueued to any controller
     */
    @Override
    public void enqueuePickupRequest(Request request, Collection<ElevatorController> elevatorControllers) throws ElevatorsSchedulerException {
        ElevatorController chosenController;

        //if there is any elevator standing on requested floor, add this request to its queue
        chosenController = findInactiveControllerOnRequestedFloor(request, elevatorControllers);
        if (chosenController != null) {
            enqueueRequestInScannerOrder(request, chosenController);
            return;
        }

        //if not, search for standing elevator on other floor, and pick the closest
        chosenController = findClosestInactiveController(request, elevatorControllers);
        if (chosenController != null) {
            enqueueRequestInScannerOrder(request, chosenController);
            return;
        }

        //if not, search for elevators passing through requested floor
        chosenController = findClosestPassingController(request, elevatorControllers);
        if (chosenController != null) {
            enqueueRequestInScannerOrder(request, chosenController);
            return;
        }

        //if none is found pick elevator which will first reach requested floor
        chosenController = findFastestAvailableController(request, elevatorControllers);

        if (chosenController != null) {
            enqueueRequestInScannerOrder(request, chosenController);
        } else {
            throw new ElevatorsSchedulerException("Failed to find available controller for given request");
        }
    }

    /**
     * Enqueues internal request of type FLOOR or RESTART to chosen elevator controller.
     *
     * @param request    internal request to be enqueued
     * @param controller elevator controller which sent the request
     * @throws ElevatorsSchedulerException when the request can not be enqueued
     */
    @Override
    public void enqueueInternalRequest(Request request, ElevatorController controller) throws ElevatorsSchedulerException {
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

        //if requested floor is already on the queue, do nothing, otherwise insert it at the right index
        if (!alreadyEnqueued) {
            enqueueRequestInScannerOrder(request, controller);
        }
    }

    /**
     * Adds the request to chosen controller's queue.
     * It uses a "scanner" algorithm to chose a index which would be optimal to insert new request at.
     * <p>
     * When elevator is already chosen (by scheduler or the request is of a FLOOR type), it analyzes the elevator's queue in following manner:
     * Starting from the first request of a queue it seeks for a two specific sequences of requests:
     * <p>
     * A.1. Search for a sequence when elevator passes through the requested floor.
     * A.2. If such sequence was found, insert the request so that the elevator will stop on requested floor.
     * <p>
     * B.1. Search for a sequence when elevator moves towards the requested floor in the same direction as requested, but then switches its direction.
     * B.2. If such sequence was found, insert the request so that the elevator will first visit requested floor and then switch its direction.
     *
     * @param request    request to be enqueued
     * @param controller controller to receive the request
     * @throws ElevatorsSchedulerException if the request is of an unsupported type
     */
    private void enqueueRequestInScannerOrder(Request request, ElevatorController controller) throws ElevatorsSchedulerException {
        List<Request> queue = controller.getRequestsQueue();
        if (queue.contains(request)) {
            return;
        }
        if (queue.isEmpty()) {
            controller.getRequestsQueue().add(request);
            return;
        }

        int queueIndex = 0;
        int requestedFloor = request.getFloor();
        int fromFloor = controller.getElevatorStatus().getCurrentFloor();
        int toFloor = controller.getElevatorStatus().getDestinationFloor();

        boolean isMovingUP = fromFloor < toFloor;
        boolean wasMovingUp = isMovingUP;

        //for every two next requests we check for 2 situations: missing requested floor or direction change before reaching requested floor
        for (Request enqueuedRequest : queue) {
            toFloor = enqueuedRequest.getFloor();
            isMovingUP = fromFloor < toFloor;

            boolean passingUpwards = fromFloor < requestedFloor && requestedFloor < toFloor;
            boolean passingDownwards = fromFloor > requestedFloor && requestedFloor > toFloor;

            //missing requested floor when going in the right direction
            boolean passingRequestedFloor;
            //going in the right direction and then switching elevator direction
            boolean insertBeforeSwitch;

            switch (request.getRequestType()) {
                case UP:
                    passingRequestedFloor = passingUpwards;
                    insertBeforeSwitch = wasMovingUp && !isMovingUP;
                    break;
                case DOWN:
                    passingRequestedFloor = passingDownwards;
                    insertBeforeSwitch = !wasMovingUp && isMovingUP;
                    break;
                case FLOOR:
                    passingRequestedFloor = passingUpwards || passingDownwards;
                    insertBeforeSwitch = wasMovingUp != isMovingUP && Math.abs(requestedFloor - toFloor) > Math.abs(requestedFloor - fromFloor);
                    break;
                default:
                    throw new ElevatorsSchedulerException("Invalid request to be enqueued");
            }

            //if any of those events happen, insert request just before it would have happen
            if (passingRequestedFloor || insertBeforeSwitch) {
                controller.getRequestsQueue().add(queueIndex, request);
                return;
            }

            wasMovingUp = isMovingUP;
            fromFloor = toFloor;
            queueIndex++;
        }

        //else add at the end of a queue
        controller.getRequestsQueue().add(request);
    }

    /**
     * Finds an elevator, which is standing inactive on requested floor.
     *
     * @param request             request to be enqueued
     * @param elevatorControllers all available controllers
     * @return an {@link ElevatorController} of an elevator if it satisfies the criteria, otherwise null
     */
    private ElevatorController findInactiveControllerOnRequestedFloor(Request request, Collection<ElevatorController> elevatorControllers) {
        return elevatorControllers
                .stream()
                .filter(controller -> controller.getElevatorStatus().getCurrentFloor() == request.getFloor())
                .filter(ElevatorController::isInactive)
                .findAny()
                .orElse(null);
    }

    /**
     * Finds an closest elevator, which is moving towards requested floor in the same direction as requested direction.
     *
     * @param request             request to be enqueued
     * @param elevatorControllers all available controllers
     * @return an {@link ElevatorController} of an closest elevator that satisfies the criteria, otherwise null
     * @throws ElevatorsSchedulerException if the request is invalid
     */
    private ElevatorController findClosestPassingController(Request request, Collection<ElevatorController> elevatorControllers) throws ElevatorsSchedulerException {
        Stream<ElevatorController> passingElevators;

        switch (request.getRequestType()) {
            case UP:
                passingElevators = elevatorControllers
                        .stream()
                        .filter(controller -> {
                            ElevatorStatus status = controller.getElevatorStatus();
                            return status.getCurrentFloor() < status.getDestinationFloor() && status.getCurrentFloor() <= request.getFloor();
                        });
                break;
            case DOWN:
                passingElevators = elevatorControllers
                        .stream()
                        .filter(controller -> {
                            ElevatorStatus status = controller.getElevatorStatus();
                            return status.getCurrentFloor() > status.getDestinationFloor() && status.getCurrentFloor() >= request.getFloor();
                        });
                break;
            default:
                throw new ElevatorsSchedulerException("Unsupported pickup request received");
        }

        return passingElevators
                .min(Comparator.comparingInt(controller -> Math.abs(controller.getElevatorStatus().getCurrentFloor() - request.getFloor())))
                .orElse(null);
    }

    /**
     * Finds an closest elevator, which is inactive but is standing on the other floor than requested.
     *
     * @param request             request to be enqueued
     * @param elevatorControllers all available controllers
     * @return an {@link ElevatorController} of an closest elevator that satisfies the criteria, otherwise null
     */
    private ElevatorController findClosestInactiveController(Request request, Collection<ElevatorController> elevatorControllers) {
        return elevatorControllers
                .stream()
                .filter(ElevatorController::isInactive)
                .min(Comparator.comparingInt(controller -> Math.abs(controller.getElevatorStatus().getCurrentFloor() - request.getFloor())))
                .orElse(null);
    }

    /**
     * For all given elevator controllers, checks which would be the first to reach requested floor in the "scanner" order and return it.
     *
     * @param request             request to be enqueued
     * @param elevatorControllers all available controllers
     * @return an {@link ElevatorController} of an elevator which would first reach requested floor
     */
    private ElevatorController findFastestAvailableController(Request request, Collection<ElevatorController> elevatorControllers) {
        return elevatorControllers
                .stream()
                .min(Comparator.comparingInt(controller -> calculateStepsToCompleteRequestInScannerOrder(request, controller)))
                .orElse(null);
    }

    /**
     * For given controller, calculates how many steps would it need to reach requested floor by the specific "scanner" order.
     *
     * @param request    request to be enqueued
     * @param controller controller to be checked
     * @return number of steps required by controller to reach the requested floor if it received such request
     * @see SchedulerScanner class JavaDoc for "scanner" order description
     */
    private int calculateStepsToCompleteRequestInScannerOrder(Request request, ElevatorController controller) {
        List<Request> queue = controller.getRequestsQueue();

        //case when such request is already in the queue
        if (queue.contains(request))
            return 0;

        //case when there are no requests in the queue
        if (queue.isEmpty()) {
            return Math.abs(controller.getElevatorStatus().getCurrentFloor() - request.getFloor());
        }

        int stepsRequired = 0;
        int requestedFloor = request.getFloor();
        int fromFloor = controller.getElevatorStatus().getCurrentFloor();
        int toFloor = controller.getElevatorStatus().getDestinationFloor();

        boolean isMovingUP = fromFloor < toFloor;
        boolean wasMovingUP;
        boolean missingRequestedFloor = false;
        boolean insertBeforeSwitch = false;

        //case when there are other requests in the queue
        for (Request enqueuedRequest : queue) {
            fromFloor = toFloor;
            toFloor = enqueuedRequest.getFloor();

            wasMovingUP = isMovingUP;
            isMovingUP = fromFloor < toFloor;

            boolean passingUpwards = fromFloor < requestedFloor && requestedFloor < toFloor;
            boolean passingDownwards = fromFloor > requestedFloor && requestedFloor > toFloor;

            switch (request.getRequestType()) {
                case UP:
                    missingRequestedFloor = passingUpwards;
                    insertBeforeSwitch = wasMovingUP && !isMovingUP;
                    break;
                case DOWN:
                    missingRequestedFloor = passingDownwards;
                    insertBeforeSwitch = !wasMovingUP && isMovingUP;
                    break;
            }

            //if elevator switches direction or passes through requested floor, then the request would be put there
            if (missingRequestedFloor || insertBeforeSwitch)
                break;
            else
                stepsRequired += Math.abs(fromFloor - toFloor);
        }

        stepsRequired += Math.abs(fromFloor - requestedFloor);
        return stepsRequired;
    }
}
