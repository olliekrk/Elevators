package olliekrk.elevators;

import olliekrk.elevators.exceptions.ElevatorsSchedulerException;
import olliekrk.elevators.exceptions.ElevatorsSystemException;
import olliekrk.elevators.requests.Request;
import olliekrk.elevators.requests.RequestFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class which is an entry point for this project.
 * Provides interface with all necessary methods to manage the system and to simulate the system's behaviour.
 */
public class ElevatorsSystem {
    /**
     * Maximum number of elevators the system can manage.
     */
    public final static int ELEVATORS_LIMIT = 16;
    /**
     * Map of registered elevators' controllers.
     * The key is the elevator ID and the value is its controller.
     */
    private Map<Integer, ElevatorController> elevatorControllers;
    /**
     * Scheduler used to enqueue incoming requests.
     * Part of a "Strategy" design pattern.
     */
    private ElevatorsScheduler scheduler;

    /**
     * Creates a new elevators system and assigns given {@link ElevatorsScheduler} to schedule incoming requests.
     *
     * @param scheduler scheduler to be used for enqueuing requests
     */
    public ElevatorsSystem(ElevatorsScheduler scheduler) {
        this.elevatorControllers = new HashMap<>(ELEVATORS_LIMIT * 2);
        this.scheduler = scheduler;
    }

    /**
     * Method to change system's scheduler.
     * Can be used to switch scheduling strategy.
     *
     * @param scheduler new scheduler to replace old one with
     */
    public void setScheduler(ElevatorsScheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * Send a new request to be enqueued to the right elevator's controller.
     * Calls corresponding method of {@link ElevatorsScheduler} interface (strategy design pattern).
     * The scheduler uses an algorithm to assign the request in its own manner to chosen controller.
     *
     * @param request request to be enqueued
     */
    public void enqueueRequest(Request request) {
        try {
            switch (request.getRequestType()) {
                case UP:
                case DOWN:
                    scheduler.enqueuePickupRequest(request, elevatorControllers.values());
                    break;
                case FLOOR:
                case RESTART:
                    scheduler.enqueueInternalRequest(request, elevatorControllers.get(request.getElevatorID()));
                    break;
                case EVACUATION:
                    scheduler.enqueueEvacuationRequest(request, elevatorControllers.values());
                    break;
                default:
                    throw new ElevatorsSystemException("System has received an unsupported request to be enqueued");
            }
        } catch (ElevatorsSystemException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Makes a single simulation step of elevators system's work schedule.
     * <p>
     * Every elevator controller checks its queue and performs single operation depending on first request in the queue.
     *
     * @see ElevatorController for more information about proceeding requests
     */
    public void makeSimulationStep() {
        for (ElevatorController controller : elevatorControllers.values()) {
            controller.makeOneStep();
        }
    }

    /**
     * Registers new elevator in the system.
     * The elevator which is being registered must have unique ID compared to already registered elevators.
     * The limit of registered elevators cannot be exceeded.
     *
     * @param elevatorID ID of elevator to be registered
     * @param startFloor floor on which elevator starts its work
     */
    public void registerElevator(Integer elevatorID, int startFloor) {
        if (elevatorControllers.size() == ELEVATORS_LIMIT || elevatorControllers.containsKey(elevatorID)) {
            System.err.println("Failed to register elevator with ID: " + elevatorID.toString());
            return;
        }
        Elevator elevator = new Elevator(elevatorID, startFloor);
        ElevatorController controller = new ElevatorController(elevator);
        elevatorControllers.put(elevatorID, controller);
    }

    /**
     * Manually updates chosen elevator's status: ID, its current floor and its destination floor.
     *
     * @param elevatorID       ID of an updated elevator
     * @param currentFloor     new current floor
     * @param destinationFloor new destination floor
     */
    public void updateElevatorStatus(Integer elevatorID, int currentFloor, int destinationFloor) {
        ElevatorController controller = elevatorControllers.get(elevatorID);
        if (controller == null) {
            System.err.println("Failed to update status of elevator with ID: " + elevatorID.toString());
            return;
        }
        controller.setElevatorCurrentFloor(currentFloor);
        Request request = RequestFactory.createRestartRequest(elevatorID, destinationFloor);
        try {
            scheduler.enqueueInternalRequest(request, controller);
        } catch (ElevatorsSchedulerException e) {
            //unlikely to happen as created request is validated with use of factory design pattern
            System.err.println(e.getMessage());
        }
    }

    /**
     * Get summary information about every elevator registered in the system's status.
     *
     * @return list of {@link ElevatorStatus} statuses of system's elevators
     */
    public List<ElevatorStatus> getElevatorsStatuses() {
        return elevatorControllers.values()
                .stream()
                .map(ElevatorController::getElevatorStatus)
                .collect(Collectors.toList());
    }

    /**
     * Checks whether there are any awaiting requests in the system.
     *
     * @return true if there are any requests in any controller's queue, otherwise false
     */
    public boolean isAnyRequestUnprocessed() {
        return !elevatorControllers
                .values()
                .stream()
                .allMatch(ElevatorController::isInactive);
    }

    /**
     * Counts the total number of requests enqueued in the system.
     *
     * @return total number of all requests enqueued to all controllers
     */
    public int numberOfRequestsEnqueued() {
        int totalRequests = 0;
        for (ElevatorController controller : elevatorControllers.values()) {
            totalRequests += controller.getRequestsQueue().size();
        }
        return totalRequests;
    }
}
