package olliekrk;

import olliekrk.elevators.ElevatorStatus;
import olliekrk.elevators.ElevatorsSystem;
import olliekrk.elevators.SchedulerFCFS;
import olliekrk.elevators.SchedulerScanner;
import olliekrk.elevators.requests.Request;
import olliekrk.elevators.requests.RequestFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;


/**
 * Class used to test and compare both scheduling algorithms by testing how many steps it takes for them
 * to complete random requests queue.
 */
public class Simulation {
    private static final int ELEVATORS_NO = 5;
    private static final int FLOORS_NO = 20;
    private static final String usage = "Type one of the following options:\n\tgenerate\n\tstep\n\tstatus\n\tend\n\tusage\n";

    public static void main(String[] args) {
        ElevatorsSystem systemFCFS = new ElevatorsSystem(new SchedulerFCFS());
        ElevatorsSystem systemScanner = new ElevatorsSystem(new SchedulerScanner());
        List<Request> requestsQueue;

        //counters for steps
        int stepsFCFS = 0;
        int stepsScanner = 0;
        int totalRequestsCount = 0;

        //register some elevators
        for (int i = 0; i < ELEVATORS_NO; i++) {
            systemFCFS.registerElevator(i, 0);
            systemScanner.registerElevator(i, 0);
        }

        System.out.println(usage);
        Scanner scanner = new Scanner(System.in);

        boolean loop = true;
        while (loop) {
            try {
                String option = scanner.next();
                switch (option) {
                    case "generate":
                        System.out.println("How many records would you like to generate?");
                        int recordsCount = Integer.parseInt(scanner.next());
                        requestsQueue = generateRequests(recordsCount);
                        addRequests(systemFCFS, requestsQueue);
                        addRequests(systemScanner, requestsQueue);
                        totalRequestsCount += requestsQueue.size();
                        break;
                    case "step":
                        System.out.println("How many steps further would you like to proceed?");
                        int steps = Integer.parseInt(scanner.next());
                        for (int i = 0; i < steps; i++) {
                            if (systemFCFS.isAnyRequestUnprocessed()) {
                                systemFCFS.makeSimulationStep();
                                stepsFCFS++;
                            }
                            if (systemScanner.isAnyRequestUnprocessed()) {
                                systemScanner.makeSimulationStep();
                                stepsScanner++;
                            }
                        }
                        break;
                    case "status":
                        System.out.print("---\nSystem with SchedulerFCFS status\n---\n");
                        for (ElevatorStatus status : systemFCFS.getElevatorsStatuses())
                            System.out.println(status);

                        System.out.print("---\nSystem with SchedulerScanner status\n---\n");
                        for (ElevatorStatus status : systemScanner.getElevatorsStatuses())
                            System.out.println(status);
                        break;
                    case "end":
                        loop = false;
                        break;
                    case "usage":
                        System.out.println(usage);
                        break;
                    default:
                        System.err.println("Unrecognized option.");
                        break;
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid simulation argument!");
                System.out.println(e.getMessage());
            }
        }

        while (systemScanner.isAnyRequestUnprocessed()) {
            systemScanner.makeSimulationStep();
            stepsScanner++;
        }

        while (systemFCFS.isAnyRequestUnprocessed()) {
            systemFCFS.makeSimulationStep();
            stepsFCFS++;
        }

        printSimulationResults(stepsFCFS, stepsScanner, totalRequestsCount);
    }

    private static List<Request> generateRequests(int recordsCount) {
        Random rand = new Random();
        List<Request> requests = new ArrayList<>(recordsCount);
        for (int i = 0; i < recordsCount; i++) {
            int type = rand.nextInt(3);
            Request request = null;
            switch (type) {
                case 0:
                    request = RequestFactory.createUpRequest(rand.nextInt(FLOORS_NO + 1));
                    break;
                case 1:
                    request = RequestFactory.createDownRequest(rand.nextInt(FLOORS_NO + 1));
                    break;
                case 2:
                    request = RequestFactory.createFloorRequest(rand.nextInt(ELEVATORS_NO), rand.nextInt(FLOORS_NO + 1));
                    break;
            }
            if (request != null)
                requests.add(request);
        }
        return requests;
    }

    private static void addRequests(ElevatorsSystem system, List<Request> requests) {
        for (Request request : requests)
            system.enqueueRequest(request);
    }

    private static void printSimulationResults(int stepsFCFS, int stepsScanner, int totalRequestsCount) {
        System.out.println("---");
        System.out.println("Simulation summary:");
        System.out.println("---");
        System.out.println("Elevators: " + ELEVATORS_NO);
        System.out.println("Floors: " + FLOORS_NO);
        System.out.println("Total number of requests received: " + totalRequestsCount);
        System.out.println("---");
        System.out.println("Total steps FC-FS: " + stepsFCFS);
        System.out.printf("Average steps per request: %.3f\n", (double) stepsFCFS / totalRequestsCount);
        System.out.println("---");
        System.out.println("Total steps Scanner: " + stepsScanner);
        System.out.printf("Average steps per request: %.3f\n", (double) stepsScanner / totalRequestsCount);
    }
}
