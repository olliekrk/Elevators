package olliekrk.elevators;

import olliekrk.elevators.exceptions.ElevatorsSchedulerException;
import olliekrk.elevators.requests.Request;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Scheduler implementing {@link ElevatorsScheduler} interface.
 * Responsible for enqueuing request in the order given by an algorithm.
 * The way it enqueues requests will be referred as "scanner" order.
 * <p>
 * Behaves in a similar way to "LOOK" disk scheduling algorithm.
 * Every times it assigns a request, it checks if on a chosen queue, there are any requests pending in the elevator direction.
 * Then it places the request in the queue in the chosen place, so that the elevator will change direction only after every request in its current direction will be completed.
 */
public class SchedulerScanner implements ElevatorsScheduler {

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

    @Override
    public void enqueueInternalRequest(Request request, ElevatorController controller) throws ElevatorsSchedulerException {
        switch (request.getRequestType()) {
            case FLOOR:
                //check if requested floor is already enqueued
                boolean alreadyEnqueued = controller
                        .getRequestsQueue()
                        .stream()
                        .anyMatch(enqueuedRequest -> enqueuedRequest.getFloor().equals(request.getFloor()));
                if (alreadyEnqueued) {
                    return;
                }
                //if queue is empty add it as the first request
                if (controller.getRequestsQueue().isEmpty()) {
                    controller.getRequestsQueue().add(request);
                    return;
                }
                // if not then put it at right index so that elevator will stop at requested floor
                // or just before elevator changes its direction while moving towards requested floor
                int queueIndex = 0;
                int fromFloor = controller.getElevatorStatus().getCurrentFloor();
                int toFloor = controller.getElevatorStatus().getDestinationFloor();
                int requestedFloor = request.getFloor();

                boolean currentDirectionUP = fromFloor < toFloor;
                boolean previousDirectionUP = currentDirectionUP;
                for (Request enqueuedRequest : controller.getRequestsQueue()) {
                    toFloor = enqueuedRequest.getFloor();
                    currentDirectionUP = fromFloor < toFloor;

                    boolean missingRequestedFloor = (fromFloor < requestedFloor && requestedFloor < toFloor) || (fromFloor > requestedFloor && requestedFloor > toFloor);
                    boolean switchingDirection = previousDirectionUP != currentDirectionUP;

                    if (missingRequestedFloor || switchingDirection) {
                        controller.getRequestsQueue().add(queueIndex, request);
                        return;
                    }

                    previousDirectionUP = currentDirectionUP;
                    fromFloor = toFloor;
                    queueIndex++;
                }

                //else add at the end
                controller.getRequestsQueue().add(request);
                break;
            case RESTART:
                enqueueRestartRequest(request, controller);
                break;
            default:
                throw new ElevatorsSchedulerException("Unsupported internal request received");
        }
    }

    /**
     * Adds the request to given controller's queue at the index chosen by the "scanner" order.
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
        int toFloor;

        boolean currentDirectionUP = fromFloor < controller.getElevatorStatus().getDestinationFloor();
        boolean previousDirectionUP = currentDirectionUP;
        boolean missingRequestedFloor = false;

        //if possible, add to the queue when elevator is travelling through requested floor in the right direction
        for (Request enqueuedRequest : queue) {
            toFloor = enqueuedRequest.getFloor();
            currentDirectionUP = fromFloor < toFloor;

            switch (request.getRequestType()) {
                case UP:
                    if (currentDirectionUP) {
                        missingRequestedFloor = fromFloor < requestedFloor && requestedFloor < toFloor;
                    }
                    break;
                case DOWN:
                    if (!currentDirectionUP) {
                        missingRequestedFloor = fromFloor > requestedFloor && requestedFloor > toFloor;
                    }
                    break;
                default:
                    throw new ElevatorsSchedulerException("Invalid pickup request");
            }

            boolean switchingDirection = previousDirectionUP != currentDirectionUP;
            if (missingRequestedFloor || switchingDirection) {
                controller.getRequestsQueue().add(queueIndex, request);
                return;
            }

            previousDirectionUP = currentDirectionUP;
            fromFloor = toFloor;
            queueIndex++;
        }

        //else add at the end
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
     * For given controller, calculates how many steps would it need to reach requested floor by following the "scanner" order.
     *
     * @param request    request to be enqueued
     * @param controller controller to be checked
     * @return number of steps required by controller to reach the requested floor if it received such request
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
        int currentFloor = controller.getElevatorStatus().getCurrentFloor();
        int nextFloor = controller.getElevatorStatus().getDestinationFloor();

        boolean currentDirectionUP = currentFloor < nextFloor;
        boolean previousDirectionUP = currentDirectionUP;
        boolean missingRequestedFloor = false;

        //case when there are other requests in the queue
        for (Request enqueuedRequest : queue) {
            nextFloor = enqueuedRequest.getFloor();
            currentDirectionUP = currentFloor < nextFloor;
            switch (request.getRequestType()) {
                case UP:
                    if (currentDirectionUP) {
                        missingRequestedFloor = currentFloor < requestedFloor && requestedFloor < nextFloor;
                    }
                    break;
                case DOWN:
                    if (!currentDirectionUP) {
                        missingRequestedFloor = currentFloor > requestedFloor && requestedFloor > nextFloor;
                    }
                    break;
            }

            //if elevator switches direction or passes through requested floor, then the request would be put there
            boolean switchingDirection = previousDirectionUP != currentDirectionUP;
            if (missingRequestedFloor || switchingDirection)
                break;

            stepsRequired += Math.abs(currentFloor - nextFloor);
            previousDirectionUP = currentDirectionUP;
            currentFloor = nextFloor;
        }

        stepsRequired += Math.abs(currentFloor - requestedFloor);
        return stepsRequired;
    }
}
