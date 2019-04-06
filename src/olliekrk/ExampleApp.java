package olliekrk;

import olliekrk.elevators.*;
import olliekrk.elevators.requests.Request;
import olliekrk.elevators.requests.RequestFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Class created to demonstrate the usage of designed elevators system {@link ElevatorsSystem}
 */
public class ExampleApp {
    public static void main(String[] args) {
        //create elevators system with FC-FS scheduler
        ElevatorsSystem elevatorsSystem = new ElevatorsSystem(new SchedulerFCFS());

        //register 16 elevators, all starting on zero floor
        int startFloor = 0;
        for (int elevatorID = 0; elevatorID < ElevatorsSystem.ELEVATORS_LIMIT; elevatorID++) {
            elevatorsSystem.registerElevator(elevatorID, startFloor);
        }

        //create some requests for the system
        List<Request> requestList = new LinkedList<>();
        requestList.add(RequestFactory.createDownRequest(10));
        requestList.add(RequestFactory.createUpRequest(6));
        requestList.add(RequestFactory.createFloorRequest(2, 7));

        //pass created requests to the system
        for (Request request : requestList) {
            elevatorsSystem.enqueueRequest(request);
        }

        //make simulation steps
        int totalSteps = 5;
        for (int step = 0; step < totalSteps; step++) {
            elevatorsSystem.makeSimulationStep();
        }

        //get status of the system
        List<ElevatorStatus> elevatorStatuses = elevatorsSystem.getElevatorsStatuses();
        for (ElevatorStatus status : elevatorStatuses) {
            System.out.println(status.toString());
        }

        //update elevator status manually (its current floor and destination floor)
        elevatorsSystem.updateElevatorStatus(0, 1, 8);
    }
}
