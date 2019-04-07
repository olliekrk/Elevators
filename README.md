# Elevators System Project

> Problem: Design an elevators system.

## Prerequisites

To use this repository all you need is to have JDK 10 (+ eventually JUnit 4) installed.

To see an quick example of how the system works there is an **ExampleApp** class with main method, where basic features are shown.

To run a simple, yet interactive simulation in which two used approaches to the problem are shown, run the main method of **Simulation** class.

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
- **SchedulerFCFS**
- **SchedulerScanner**

ElevatorsScheduler is a part of a "*Strategy*" design pattern, which was used for this project.

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

The ElevatorController can depending on what is the next request in the queue:
* open elevator door & remove completed requests
* close elevator door
* move elevator one floor up or down

These two, alongside with **ElevatorStatus** are sort of a simplified "*Model-View-Controller*" design pattern implementation.

### Creating a request for the system

The system can respond to certain requests, specified by **Request** class.

Every Request consists of enum **RequestType** which can be:
- UP (represents event of clicking 'up' button outside the elevator - pickup request)
- DOWN (represents event of clicking 'down' button outside the elevator - pickup request)
- FLOOR (represents event of clicking internal button inside the elevator)

There are also additional request types included, which are not necessary to use for basic operations:
- RESTART (represents event of restarting given elevator - cancels previously enqueued requests and directs elevator to go to specific floor)
- EVACUATION (represents event of evacuating the whole building - cancels previously enqueued requests and directs every elevator in the system to go to the ground floor)

To create a new request it is advised to use **RequestFactory** class methods.
RequestFactory is a part of "*Factory*" design pattern and it ensures that created requests are valid and safe to be enqueued.

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
In case there are some problems with enqueuing requests, the system will inform the user by throwing **ElevatorsSchedulerException** or **ElevatorsSystemException**. Possible cases when this may happen is when we want to enqueue the request but we haven't registered any elevator in the system yet.

### Running the simulation

ElevatorsSystem provides a method to make single step of simulation:
```java
elevatorsSystem.makeSimulationStep();
```
>In this design, I assumed that following actions takes one step of simulation:
>- passing one floor distance by elevator
>- opening elevator door and releasing passengers / picking up passengers
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
It contains only the basic information about elevator's ID, its current floor and its destination floor, available by getters methods and overriden toString() method to display the status in readable form.

## Scheduling algorithms

As mentioned before, for this project, I used two approaches to the problem of requests scheduling.

With use of "Strategy" design pattern, I provided two implementations of **ElevatorsScheduler** interface.

#### 1. SchedulerFCFS
First scheduling algorithm was the FCFS, also known as first-come, first-served.

Every ElevatorController has a queue, and the only operations it can perform are removing request from the start and adding request at the end. This scheduler simply enqueues requests in the order that they arrive.

By this approach, it is guaranteed that the earlier the request came to the system scheduler, the earlier it is served.

Due to that, requests the elevators system receive are enqueued to elevators in the following manner:

**Internal requests**: *button pressed inside the elevator*:

Check if requested floor is already in the queue:
- If the request is already in the queue then do nothing
- Otherwise, put it at the end of a queue (FIFO)

**External requests**: *up/down button pressed outside the elevator*

For every elevator available in the system calculate how many steps will it take for this elevator to reach requested floor:
- In calculation, take into account that opening and closing doors costs one simulation step
- Choose elevator controller which will first finish all its requests and reach requested floor
- Put request at the end of a chosen's elevator's controller queue

This behaviour ensures that any request which comes first will be executed before any request that comes after it.

**Advantages of the FCFS**:
- Relatively easy to implement
- Clear and simple to understand
- Every request, no matter at which floor, will get an equal chance to be served
- Guarantees that every request will be served so there is no infinite "requests starvation"

**Disadvantages of the FCFS**:
- Due to FIFO approach, it cannot pickup any requests "on the way"
- Hence, requests from the floors between elevator's current floor and destination floor will have to be ignored and wait until all others are finished, which lengthens their awaiting time
- May not provide the best possible requests order, especially if next two requests it receives are far from one another
- Receiving ground floor and the highest floor requests alternately will starve every other request which is on the way

#### 2. SchedulerScanner

The second scheduler I implemented is a "scanning" scheduler.

The name "Scanner" cames from the fact that it behaves in a similar way to "SCAN" (or even more "LOOK") disk scheduling algorithm.

In this approach, every elevator's queue is managed differently.
The elevator always travels in its current direction to the furthest requested floor if the direction requested was the same as the direction the elevator is moving in.

Alternatively speaking, it continues to travel travel in one direction until the queue is empty or all requests in this direction are completed.

In contrast to FCFS, it can stop to pick up and release passengers on the way.

I tried to analyze, how to possibly optimize the average awaiting time for the request to be proceeded.
I also tried to group similar requests in the queues.

The second scheduling algorithm I implemented is a algorithm, which assigns the requests by the following manner:

**Internal requests**: *button pressed inside the elevator*:

Check if the same request (requested floor and requested direction) is already in the queue.
- If such request is already in the queue, do nothing
- If the queue is empty, put this request as the first in the queue

Otherwise search for a first sequence of requests in the queue, in which the elevator:
- A.1. Passes the requested floor.
- A.2. If such sequence was found, then put the request in the queue so that elevator won't pass requested floor but will stop there and let off the passengers
- B.1. Moves towards the requested floor in requested direction but then changes its direction to the opposite.
- B.2. If such sequence was found put the request in the queue before direction change occurs.

If there are no such sequences, then simply add requested floor at the end of a queue.

**External requests**: *up/down button pressed outside the elevator*

Check if there is any inactive (idle) elevator on requested floor:
- If there is any, add request to its queue.

If there are none of them, search for inactive (idle) elevators on other floors:
- If there are any, pick the one which is closest to requested floor

If not, search for elevators which are moving towards requested floor in the same direction as the requested direction:
- Do not take steps required for opening and closing doors into calculation!
- If there are any, pick the one which is closest to requested floor

If there are also none of them, then for each elevator calculate how many steps will it take for it to reach elevator floor in described "scanning" manner
- Pick the one which will first reach requested floor

After elevator is picked, insert the request in the queue, in the similar manner as with internal requests.

**That means:**

Check if the same request (requested floor and requested direction) is already in the queue.
- If such request is already in the queue, do nothing
- If the queue is empty, put this request as the first in the queue

Otherwise search for a first sequence of requests in the queue, in which the elevator (case A or case B):
- A.1. Passes the requested floor (in the same direction as requested!).
- A.2. If such sequence was found, then put the request in the queue so that elevator won't pass requested floor but will stop there
- B.1. Moves towards the requested floor (in the same direction as requested!) but then changes its direction to the opposite
- B.2. If such sequence was found put the request in the queue before direction change occurs.

Otherwise put it at the end of the queue.

**Advantages of this approach**:
- Elevator is able to pick requests "on the way"
- Average awaiting time and overall requests completion time is shortened in most cases
- Also guarantees that every request will be served
- Requests at the mid-range will be serviced more often
- Resistant to "corrupted" cases of requests

**Disadvantages of this approach**:
- The algorithm is much more complex and not so simple to understand
- Requests from recently passed floors may have to wait for up to two direction changes occurs to be served
- Requests from floors more distant from elevators may have to wait longer than average awaiting time
- The order of requests is not maintained
- More advanced operations on enqueued elements need to be performed

#### 3. Case when the SchedulerScanner is better than SchedulerFCFS

For example:
> ElevatorSystem receives requests from the following floors in the following order: 8, 10, 1, 12 one after another.
>
>The FCFS algorithm would go straight 8->10->1->12 order, which would cost 2 + 9 + 11 = 22 steps (+ steps required to open/close the door)
>
> Whereas, the Scanner algorithm would chose 8->10->12->1 order, which would cost 2 + 2 + 11 = 15 steps (+ steps required to open/close the door)

### 4. Summary comparision

**FCFS** scheduler offers very simple scheduling strategy, however its performance is worse than **Scanner** scheduler in most cases.
It is especially ineffective in cases when an elevator receives requests from upper and lower floors alternately.
Another disadvantage of this approach is that the choice of enqueuing each request is made regardless of the requested direction (up/down).
The only factor significant for this scheduler is the order of received requests.

In this simulation I did not take into account fact that elevators have limited passengers capacity.
Whereas, using this scheduler in real building might result in overflowed elevators going into directions not requested by the passengers already inside.
Because of that, passengers awaiting for elevator on the floor will be more likely to be unable to enter busy elevators.
In fact, they would have to call the elevator again and hope that the next time there is enough space to pick them up. 

**Scanner** scheduler uses more complex strategy to store requests in queues in specific order.
It analyzes the current direction in which the elevator is moving and does not switch directions until every request in current direction is completed.
Because of this rule, it is able to pick up new passengers on the way, and also stop on floors to let off passengers which are inside.
This scheduler is advantageous over FCFS in most cases, because it includes the information about requested direction, when assigning requests to elevators.
By that it is able to plan the best path after every new request and rearrange enqueued requests to shorten average request completion time.

A possible drawback of this approach is that it requires more computation to choose the optimal elevator.
Another disadvantage is that passengers from recently visited floors may have to wait for up to two direction switches, which is more than average.
On the contrary, there is a lower risk comparing to FCFS, of an elevator being overflowed with passengers and unable to pick up new requests.

In general, if the key is simplicity - pick the **FCFS** strategy. Otherwise pick **Scanner** - especially if the elevators have limited load and the system should take that fact into account.

***Modifications possible to apply to SchedulerScanner***
 
* When choosing an elevator to receive external pickup request choose elevators in this order
 (2 and 3 is switched compared to the original version)
>1. Check for inactive elevator on the requested floor
>2. Check for elevators moving towards requested floor in the requested direction
>3. Check for inactive elevators on all other floors
>4. Check for elevators which will reach requested floor first

* Sort queue after every modification instead of looking for the right index to insert received request

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

After typing `generate` new requests will be enqueued to both FCFS and Scanner systems in amount specified by the next argument.
```
How many records would you like to generate?
```
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

They are located in the `test.olliekrk.elevators` package.

There are still many things that could be improved in those tests, as they cover only simple cases.

# TODO
* Add more unit & integration tests to cover more cases
* *JavaDoc* improvements

## Author
* **Olgierd Królik** - [GitHub](https://github.com/olliekrk) - my projects
