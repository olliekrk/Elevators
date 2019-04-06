package olliekrk.elevators;

import olliekrk.elevators.requests.Request;
import olliekrk.elevators.requests.RequestFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ElevatorControllerTest {
    private static final int initFloor = 5;
    private static final Integer elevatorID = 1;
    private static ElevatorController controller;

    @BeforeClass
    public static void setUp() throws Exception {
        controller = new ElevatorController(new Elevator(elevatorID, initFloor));
    }

    @Before
    public void setUpQueue() {
        controller.setRequestsQueue(new LinkedList<>());
    }

    @Test
    public void getElevatorStatusEmptyQueueTest() {
        ElevatorStatus elevatorStatus = controller.getElevatorStatus();
        Assert.assertTrue(controller.getRequestsQueue().isEmpty());
        assertEquals(elevatorID, elevatorStatus.getElevatorID());
        assertEquals(initFloor, elevatorStatus.getCurrentFloor());
        assertEquals(initFloor, elevatorStatus.getDestinationFloor());
    }

    @Test
    public void getElevatorStatusSingleElementQueueTest() {
        int requestedFloor = 100;
        controller.getRequestsQueue().add(RequestFactory.createUpRequest(requestedFloor));
        ElevatorStatus elevatorStatus = controller.getElevatorStatus();
        assertEquals(elevatorID, elevatorStatus.getElevatorID());
        assertEquals(initFloor, elevatorStatus.getCurrentFloor());
        assertEquals(requestedFloor, elevatorStatus.getDestinationFloor());
    }

    @Test
    public void getElevatorStatusMultipleElementQueueTest() {
        int requestedFloor1 = 10;
        int requestedFloor2 = 100;
        controller.getRequestsQueue().add(RequestFactory.createDownRequest(requestedFloor1));
        controller.getRequestsQueue().add(RequestFactory.createUpRequest(requestedFloor2));
        ElevatorStatus elevatorStatus = controller.getElevatorStatus();
        assertEquals(elevatorID, elevatorStatus.getElevatorID());
        assertEquals(initFloor, elevatorStatus.getCurrentFloor());
        assertEquals(requestedFloor1, elevatorStatus.getDestinationFloor());
    }

    @Test
    public void calculateStepsToReachFloorTest() {
        List<Request> testQueue = new LinkedList<>();
        testQueue.add(RequestFactory.createUpRequest(10));
        testQueue.add(RequestFactory.createUpRequest(12));
        testQueue.add(RequestFactory.createDownRequest(4));
        testQueue.add(RequestFactory.createUpRequest(6));

        controller.setElevatorCurrentFloor(0);
        controller.setRequestsQueue(testQueue);

        //expectedSteps = travel time + open & close the door time
        int expectedStepsToFloor10 = (10 + 2);
        int expectedStepsToFloor12 = expectedStepsToFloor10 + (2 + 2);
        int expectedStepsToFloor4 = expectedStepsToFloor12 + (8 + 2);
        int expectedStepsToFloor6 = expectedStepsToFloor4 + (2 + 2);
        int expectedStepsToFloor13 = expectedStepsToFloor6 + (7 + 2);

        Assert.assertEquals(expectedStepsToFloor10, controller.calculateStepsToReachFloor(10));
        Assert.assertEquals(expectedStepsToFloor12, controller.calculateStepsToReachFloor(12));
        Assert.assertEquals(expectedStepsToFloor4, controller.calculateStepsToReachFloor(4));
        Assert.assertEquals(expectedStepsToFloor6, controller.calculateStepsToReachFloor(6));
        Assert.assertEquals(expectedStepsToFloor13, controller.calculateStepsToReachFloor(13));

    }
}