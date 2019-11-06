package template;


import logist.task.Task;

public class CentralizedAction {
    Task task;
    Boolean isPickUp;

    public CentralizedAction(Task task, Boolean isPickUp) {
        this.task = task;
        this.isPickUp = isPickUp;
    }

}
