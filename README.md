# Elevators System Project

> Elevators Control System design project.

## Prerequisites

To use this repository all you need is to have JDK 10 and JUnit 4 installed.

To see an quick example of how the system works there is an **ExampleApp** class with main method, where basic features are shown.

## Usage

### Creating new elevators system

Entry point to this project is the **ElevatorsSystem** class.

**ElevatorsSystem** provides simple interface to perform all necessary operations, which are: 

- register an elevator to the system
- update elevator's status
- get every registered elevator's status
- enqueue new request
- make simulation step

To create new ElevatorSystem, we need to pass ElevatorsScheduler as constructor's argument.

```java
ElevatorsSystem elevatorsSystem = new ElevatorsSystem(new SchedulerFCFS());
ElevatorsSystem elevatorsSystem1 = new ElevatorsSystem(new SchedulerScanner());
```

**ElevatorsScheduler** is the interface responsible for scheduling requests for elevators in the right manner.

This project provides two different ElevatorsScheduler interface implementations, which are:
- *SchedulerFCFS*
- *SchedulerScanner*

ElevatorsScheduler is a part of a "**Strategy**" design pattern, which was used for this project.

Both implementations will be later discussed in this document.

### Registering an elevator

After ElevatorsSystem was instantiated, to register new elevators simply use:
```java
int elevatorID = 22445;
int startFloor = 0;
elevatorsSystem.registerElevator(elevatorID, startFloor);
```
The limit of register elevators is set to 16.

An class representing physical elevator is the **Elevator** class and class responsible for storing received requests in the queue and reading them is **ElevatorController**.

These two, alongside with **ElevatorStatus** are sort of a simplified "**Model-View-Controller**" design pattern implementation.

### Creating a request for the system

The system can respond to certain requests, specified by **Request** class.

Every Request consists of enum **RequestType** which can be:
- UP (represents event of clicking 'up' button outside the elevator - pickup request)
- DOWN (represents event of clicking 'down' button outside the elevator - pickup request)
- FLOOR (represents event of clicking internal button inside the elevator)

There are also additional request types, which are not necessary to use for basic operations:
- RESTART (represents event of restarting given elevator)
- EVACUATION (represents event of evacuating the whole building)

To create a new request it is advised to use **RequestFactory** class methods.
RequestFactory is a part of "**Factory**" design pattern and it ensures that created requests are valid and safe to be enqueued.

Here is the example of creating new requests:
```java
int requestedFloor = 10;

Request downRequest = RequestFactory.createDownRequest(requestedFloor);
Request upRequest = RequestFactory.createUpRequest(requestedFloor);
Request floorRequest = RequestFactory.createFloorRequest(elevatorID, requestedFloor);
```

Then, to pass the request to the ElevatorsSystem, we simply use method:
```java
elevatorsSystem.enqueueRequest(floorRequest);
```

### Running the simulation

ElevatorsSystem provides a method to make single step of simulation:
```java
elevatorsSystem.makeSimulationStep();
```
>In this design, I assumed that following actions takes one step of simulation:
>- passing one floor distance by elevator
>- opening elevator door and also releasing passengers
>- closing elevator door

### Updating chosen elevator's status

To manually change the state in which elevator chosen by ID is, ElevatorsSystem provides method:
```java
int updatedCurrentFloor = 14;
int updatedDestinationFloor = 2;
elevatorsSystem.updateElevatorStatus(elevatorID, updatedCurrentFloor, updatedDestinationFloor);
```

### Getting information about elevators' statuses

To extract basic information (ID, current floor, destination floor) about every elevator currently registered in the system, use:
```java
List<ElevatorStatus> elevatorStatuses = elevatorsSystem.getElevatorsStatuses();
```
**ElevatorStatus** is a container-class for storing the state in which elevator currently is.
It contains only the basic information about elevator's ID, its current floor and its destination floor.


## Scheduling algorithms

As mentioned before, for this project, I used two approaches to the problem of requests scheduling.

With use of "Strategy" design pattern, I provided two implementations of **ElevatorsScheduler** interface.

#### 1. SchedulerFCFS
First scheduling algorithm was the FCFS, also known as first-come, first-served.
This scheduler, simply enqueues requests in the order that they arrive.
In this approach, requests the elevators system receive are enqueued to elevators in the following manner:

**Internal requests**: *button pressed inside the elevator*:

Check if requested floor is already in the queue:
- If the request is already in the queue then do nothing
- Otherwise, put it at the end of a queue (FIFO)

**External requests**: *up/down button pressed outside the elevator*

For every elevator available in the system calculate how many steps will it take for this elevator to reach requested floor:
- In calculation, take into account that opening and closing doors costs one simulation step
- Choose elevator which will first finish all its requests first and reach requested floor
- Put request at the end of a queue

This ensures that any request which comes first will be executed before any request that comes after it.

**Advantages of the FCFS**:
- Relatively easy to implement
- Clear and simple to understand
- Every request, no matter at which floor, will get an equal chance to be served
- Guarantees that every request will be served so there is no "requests starvation"

**Disadvantages of the FCFS**:
- Due to FIFO approach, it cannot pickup any requests "on the way"
- Hence, requests from the floors between elevator's current floor and destination floor will have to be ignored and wait until all others are finished, which lengthens their awaiting time
- May not provide the best possible requests order, especially if next two requests it receives are far from one another

#### 2. SchedulerScanner
In this approach I tried to analyze, how to possibly optimize the average awaiting time for the request to be proceeded.
I also tried to group similar requests in the queues.
By that, the elevator is able to complete them one after another, without travelling the end of an opposite direction if such request also arrived in the meantime.

The second scheduling algorithm I implemented is a algorithm, which assigns the requests by the following manner:

**Internal requests**: *button pressed inside the elevator*:

Check if the same request (requested floor and requested direction) is already in the queue.
- If such request is already in the queue, do nothing
- If the queue is empty, put this request as the first in the queue

Otherwise search for a first sequence of requests in the queue, in which the elevator:
-  A) Passes the requested floor. If such sequence was found, then put the request in the queue so that elevator won't pass requested floor but will stop there
-  B) Moves towards the requested floor but then changes its direction to the opposite. If such sequence was found put the request in the queue before direction change occurs.

If there are no such sequences, then simply add requested floor at the end of a queue.

**External requests**: *up/down button pressed outside the elevator*

Check if there is any inactive (idle) elevator on requested floor:
- If there is any, add request to its queue.

If not, search for elevators which are moving towards requested floor in the same direction as the requested direction:
- Do not take steps required for opening and closing doors into calculation
- If there are any, pick the one which is closest to requested floor

If there are none of them, search for inactive (idle) elevators on other floors:
- If there are any, pick the one which is closest to requested floor

If there are also none of them, then for each elevator calculate how many steps will it take for it to reach elevator floor in described "scanning" manner
- Pick the one which will first reach requested floor

After elevator is picked, insert the request in the queue, in the similar manner as with internal requests.

**That means:**

Check if the same request (requested floor and requested direction) is already in the queue.
- If such request is already in the queue, do nothing
- If the queue is empty, put this request as the first in the queue

Otherwise search for a first sequence of requests in the queue, in which the elevator:
- A) Passes the requested floor (in the same direction as requested!).
- If such sequence was found, then put the request in the queue so that elevator won't pass requested floor but will stop there
- B) Moves towards the requested floor (in the same direction as requested!) but then changes its direction.
- If such sequence was found put the request in the queue before direction change occurs.

Otherwise put it at the end of the queue.

**Advantages of this approach**:
- Elevator is able to pick requests "on the way"
- Average awaiting time and overall requests completion time is shortened in most cases
- Also guarantees that every request will be served
- Requests at the midrange will be serviced more often

**Disadvantages of this approach**:
- The algorithm is much more complex and not so simple to understand
- Requests from recently passed floors may have to wait for one or two direction changes to be served
- Requests from floors more distant from elevators may have to wait longer than average 

#### 3. Case when the SchedulerScanner is better than SchedulerFCFS

For example:
> ElevatorSystem receives requests from the following floors in the following order: 8, 10, 1, 12 one after another.
>
>The FCFS algorithm would go straight 8->10->1->12 order, which would cost 2 + 9 + 11 = 22 steps (+ steps required to open/close the door)
>
> Whereas, the Scanner algorithm would chose 8->10->12->1 order, which would cost 2 + 2 + 11 = 15 steps (+ steps required to open/close the door)

## Simulation

To run a very primitive, yet interactive simulation of the system just run provided **Simulation** class.

It allows to compare systems using both FCFS and Scanner schedulers.

After running the configuration for Simulation class (or later by typing `usage`) you'll see:
```
Type one of the following options:
	generate
	step
	status
	end
	usage
```

The usage is very intuitive.

After typing `generate` new requests will be enqueued to both FCFS and Scanner systems.
They are generated randomly, in an amount specified in Simulation class. Both systems will receive exactly the same new queue of requests.

After typing `step` you'll see:

```
How many steps further would you like to proceed?
```
Then just type an integer value and both systems will proceed further given number of steps.

After typing `end` both systems will run the simulation till they finish every request and then they will print the summary report to the console.

After typing `status` both system will print its statuses to the console.

## Tests

In the project I made several simple tests to see whether implemented algorithms work properly and cover certain cases.

Tests are located in the `test.olliekrk.elevators` package.

There are still many things that could be improved in those tests, which I would like to do in the future.

## Author
* **Olgierd Królik** - [GitHub](https://github.com/olliekrk) - other projects

# TODO
* Add more unit & integration tests to cover more cases
* *JavaDoc* improvements

* ***Modifications to think about in SchedulerScanner***
 
 When choosing an elevator to receive external pickup request choose elevators in this order
 (2 and 3 is switched compared to the original version)
>1. Check for inactive elevator on the requested floor
>2. Check for inactive elevators on all other floors
>3. Check for elevators moving towards requested floor in the requested direction
>4. Check for elevators which will reach requested floor first