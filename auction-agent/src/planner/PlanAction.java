package planner;
// t2

import logist.task.Task;

public class PlanAction {
    public Task task;
    public Boolean isPickUp;

    public PlanAction(Task task, Boolean isPickUp) {
        this.task = task;
        this.isPickUp = isPickUp;
    }

}
