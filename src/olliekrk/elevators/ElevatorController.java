package olliekrk.elevators;

import olliekrk.elevators.requests.Request;

import java.util.LinkedList;
import java.util.List;

/**
 * Class representing internal controller of an specific elevator.
 * Manages the model class {@link Elevator}, as it is a part of Model-View-Controller design pattern.
 * Receives requests from {@link ElevatorsSystem} which are inserted to its requestsQueue in specific order.
 * <p>
 * The queue is the list of requests to be proceeded by elevator.
 * This controller is responsible for translating given requests to commands and controlling the elevator behaviour.
 * Depending on what is the first request in the queue it can open or close elevator door, or else move one floor up or down.
 */
class ElevatorController {
    /**
     * Elevator controlled by this controller.
     */
    private final Elevator elevator;
    /**
     * Queue of request to be completed.
     */
    private List<Request> requestsQueue;

    ElevatorController(Elevator elevator) {
        this.elevator = elevator;
        this.requestsQueue = new LinkedList<>();
    }

    private void openElevatorDoor() {
        elevator.setDoorOpened(true);
    }

    private void closeElevatorDoor() {
        elevator.setDoorOpened(false);
    }

    private void moveElevatorUp() {
        elevator.setCurrentFloor(elevator.getCurrentFloor() + 1);
    }

    private void moveElevatorDown() {
        elevator.setCurrentFloor(elevator.getCurrentFloor() - 1);
    }

    boolean isInactive() {
        return requestsQueue.isEmpty();
    }

    List<Request> getRequestsQueue() {
        return requestsQueue;
    }

    ElevatorStatus getElevatorStatus() {
        int destinationFloor = (requestsQueue.isEmpty()) ? elevator.getCurrentFloor() : requestsQueue.get(0).getFloor();
        return new ElevatorStatus(elevator.getId(), elevator.getCurrentFloor(), destinationFloor);
    }

    void setElevatorCurrentFloor(int floor) {
        elevator.setCurrentFloor(floor);
    }

    void setRequestsQueue(List<Request> requestsQueue) {
        this.requestsQueue = requestsQueue;
    }

    /**
     * Method used for simulating controller's behaviour.
     * It reads first request from the queue and then does the following:
     * - If queue is empty, it opens elevator doors and after that does nothing until new request comes.
     * - Otherwise, closes the door if they are opened and starts to move elevator towards requested floor.
     */
    void makeOneStep() {
        //case when there are no pending requests, idle state
        if (requestsQueue.isEmpty()) {
            openElevatorDoor();
            return;
        }

        Request currentRequest = requestsQueue.get(0);

        //case when current request is on current floor
        if (currentRequest.getFloor() == elevator.getCurrentFloor()) {
            //open elevator door and remove completed request
            openElevatorDoor();
            requestsQueue.remove(currentRequest);
            return;
        }

        //case when current request is on other floor
        if (elevator.isDoorOpened()) {
            closeElevatorDoor();
        } else {
            //move towards the requested floor
            if (currentRequest.getFloor() > elevator.getCurrentFloor()) {
                moveElevatorUp();
            } else {
                moveElevatorDown();
            }
        }
    }

    /**
     * Method for calculating distance in steps, from the elevator to given floor.
     * It assumes that opening and closing door on floors cost one extra step.
     * <p>
     * If given floor is not in the queue, calculates how many steps will it take to finish the queue and then to reach the floor.
     *
     * @param floor floor to which distance is being checked
     * @return number of steps required to reach given floor
     */
    int calculateStepsToReachFloor(int floor) {
        if (elevator.getCurrentFloor() == floor) {
            return elevator.isDoorOpened() ? 0 : 1;
        }

        int stepsRequired = 0;
        int floorReached = elevator.getCurrentFloor();
        int floorEnqueued;

        for (Request request : requestsQueue) {
            floorEnqueued = request.getFloor();
            //add step required to close the door
            stepsRequired += 1;
            //add steps required to pass distance between next enqueued floor
            stepsRequired += Math.abs(floorReached - floorEnqueued);
            //update floor reached at this point
            floorReached = floorEnqueued;
            //add step required to open the door
            stepsRequired += 1;

            if (floorReached == floor) {
                return stepsRequired;
            }
        }

        //if at the current moment elevator had closed door, stepsRequired was incremented one extra time in for-loop
        if (!elevator.isDoorOpened()) {
            stepsRequired -= 1;
        }

        //add step required to close the door
        stepsRequired += 1;
        //add steps required to reach requested floor
        stepsRequired += Math.abs(floorReached - floor);
        //add step required to open the door
        stepsRequired += 1;

        return stepsRequired;
    }
}
