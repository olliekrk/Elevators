package olliekrk.elevators;

/**
 * Class for representing information about the state in which the {@link Elevator} currently is.
 */
public class ElevatorStatus {
    /**
     * Elevator's ID.
     */
    private final Integer elevatorID;
    /**
     * Floor on which the elevator currently is.
     */
    private final int currentFloor;
    /**
     * Floor towards which the elevator is currently moving.
     */
    private final int destinationFloor;

    ElevatorStatus(Integer elevatorID, int currentFloor, int destinationFloor) {
        this.elevatorID = elevatorID;
        this.currentFloor = currentFloor;
        this.destinationFloor = destinationFloor;
    }

    public Integer getElevatorID() {
        return elevatorID;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public int getDestinationFloor() {
        return destinationFloor;
    }

    @Override
    public String toString() {
        StringBuilder stringStatus = new StringBuilder("Elevator status:");
        stringStatus
                .append("\tID: ")
                .append(elevatorID)
                .append("\tCurrent floor: ")
                .append(currentFloor)
                .append("\tDestination floor: ")
                .append(destinationFloor);

        return stringStatus.toString();
    }
}
