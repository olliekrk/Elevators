package olliekrk.elevators;

import olliekrk.elevators.exceptions.ElevatorsSchedulerException;
import olliekrk.elevators.requests.Request;
import olliekrk.elevators.requests.RequestFactory;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class SchedulerScannerTest {

    @Test
    public void enqueuePickupRequestWhenElevatorOnFloorTest() {
        ElevatorController shouldBeChosenController = new ElevatorController(new Elevator(0, 2));
        ElevatorController anotherController = new ElevatorController(new Elevator(1, 10));
        List<ElevatorController> controllerList = List.of(shouldBeChosenController, anotherController);
        Request pickupRequest = RequestFactory.createUpRequest(2);

        SchedulerScanner schedulerScanner = new SchedulerScanner();
        try {
            schedulerScanner.enqueuePickupRequest(pickupRequest, controllerList);
        } catch (ElevatorsSchedulerException e) {
            e.printStackTrace();
        }

        //assert that request was added to the elevator on the floor
        assertEquals(1, shouldBeChosenController.getRequestsQueue().size());
        assertEquals(0, anotherController.getRequestsQueue().size());
        assertEquals((int) pickupRequest.getFloor(), shouldBeChosenController.getElevatorStatus().getDestinationFloor());
    }

    @Test
    public void enqueuePickupRequestWhenElevatorMovingTowardFloorTest() {
        ElevatorController shouldBeChosenController = new ElevatorController(new Elevator(0, 2));
        ElevatorController anotherController = new ElevatorController(new Elevator(1, 10));
        List<ElevatorController> controllerList = List.of(shouldBeChosenController, anotherController);

        Request request1 = RequestFactory.createUpRequest(6);
        Request request2 = RequestFactory.createUpRequest(15);
        shouldBeChosenController.getRequestsQueue().add(request1);
        anotherController.getRequestsQueue().add(request2);

        Request pickupRequest = RequestFactory.createUpRequest(8);

        SchedulerScanner schedulerScanner = new SchedulerScanner();
        try {
            schedulerScanner.enqueuePickupRequest(pickupRequest, controllerList);
        } catch (ElevatorsSchedulerException e) {
            e.printStackTrace();
        }

        //assert that request was added to the elevator moving towards the floor
        assertEquals(2, shouldBeChosenController.getRequestsQueue().size());
        assertEquals(1, anotherController.getRequestsQueue().size());
        assertEquals((int) request1.getFloor(), shouldBeChosenController.getElevatorStatus().getDestinationFloor());
    }

    @Test
    public void enqueuePickupRequestToFastestAvailableElevator() {
        ElevatorController shouldBeChosenController = new ElevatorController(new Elevator(0, 2));
        ElevatorController anotherController = new ElevatorController(new Elevator(1, 10));
        List<ElevatorController> controllerList = List.of(shouldBeChosenController, anotherController);

        Request request1 = RequestFactory.createUpRequest(0);
        Request request2 = RequestFactory.createDownRequest(20);
        shouldBeChosenController.getRequestsQueue().add(request1);
        anotherController.getRequestsQueue().add(request2);

        Request pickupRequest = RequestFactory.createUpRequest(6);

        SchedulerScanner schedulerScanner = new SchedulerScanner();
        try {
            schedulerScanner.enqueuePickupRequest(pickupRequest, controllerList);
        } catch (ElevatorsSchedulerException e) {
            e.printStackTrace();
        }

        //assert that request was added to the elevator moving towards the floor
        assertEquals(2, shouldBeChosenController.getRequestsQueue().size());
        assertEquals(1, anotherController.getRequestsQueue().size());
    }
}