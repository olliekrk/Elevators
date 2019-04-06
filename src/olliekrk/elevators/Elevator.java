package olliekrk.elevators;

/**
 * Class representing physical elevator, fully managed by {@link ElevatorController} class.
 * Belongs to the whole system of elevators represented by {@link ElevatorsSystem} class.
 * Part of an Model-View-Controller design pattern alongside with {@link ElevatorController}.
 */
public class Elevator {
    /**
     * Unique ID of an elevator.
     */
    private final Integer id;
    /**
     * Flag indicating whether the elevator door are opened.
     */
    private boolean doorOpened;
    /**
     * Current floor on which elevator is standing.
     */
    private int currentFloor;

    Elevator(Integer id, int currentFloor) {
        this.id = id;
        this.doorOpened = true;
        this.currentFloor = currentFloor;
    }

    int getId() {
        return id;
    }

    boolean isDoorOpened() {
        return doorOpened;
    }

    int getCurrentFloor() {
        return currentFloor;
    }

    void setDoorOpened(boolean doorOpened) {
        this.doorOpened = doorOpened;
    }

    void setCurrentFloor(int currentFloor) {
        this.currentFloor = currentFloor;
    }
}
