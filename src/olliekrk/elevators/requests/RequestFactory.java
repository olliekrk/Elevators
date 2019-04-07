package olliekrk.elevators.requests;

/**
 * Factory class, used to create valid and safe to be scheduled {@link Request} instances.
 * Part of an "Factory" design pattern.
 *
 * @see RequestType for more information
 */
public class RequestFactory {
    public static Request createUpRequest(int floor) {
        return new Request(RequestType.UP, null, floor);
    }

    public static Request createDownRequest(int floor) {
        return new Request(RequestType.DOWN, null, floor);
    }

    public static Request createFloorRequest(int elevatorID, int floor) {
        return new Request(RequestType.FLOOR, elevatorID, floor);
    }

    public static Request createRestartRequest(int elevatorID, int restartFloor) {
        return new Request(RequestType.RESTART, elevatorID, restartFloor);
    }

    public static Request createEvacuationRequest() {
        return new Request(RequestType.EVACUATION, null, 0);
    }
}
