package olliekrk.elevators.requests;

import java.util.Objects;

/**
 * Represents a passenger's request which is send to {@link olliekrk.elevators.ElevatorsSystem}.
 */
public class Request {
    /**
     * Type of a request.
     */
    private final RequestType requestType;
    /**
     * ID of an elevator to which request was sent or null if the request was external.
     */
    private final Integer elevatorID;
    /**
     * Floor requested by the request.
     */
    private final int floor;

    Request(RequestType requestType, Integer elevatorID, int floor) {
        this.requestType = requestType;
        this.elevatorID = elevatorID;
        this.floor = floor;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public Integer getElevatorID() {
        return elevatorID;
    }

    public Integer getFloor() {
        return floor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Request request = (Request) o;

        if (floor != request.floor) return false;
        if (requestType != request.requestType) return false;
        return Objects.equals(elevatorID, request.elevatorID);
    }
}
