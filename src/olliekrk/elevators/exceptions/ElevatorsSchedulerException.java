package olliekrk.elevators.exceptions;

/**
 * Class for representing exception thrown by {@link olliekrk.elevators.ElevatorsScheduler}.
 */
public class ElevatorsSchedulerException extends ElevatorsSystemException {
    public ElevatorsSchedulerException(String message) {
        super("Scheduling exception: " + message);
    }
}
