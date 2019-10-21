package template;
import com.sun.xml.internal.bind.v2.runtime.reflect.Lister;
import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;


public class DeliberativeState {

    private HashSet<Task> taskOnBoard;
    private HashSet<Task> taskAvailable;
    private City currentCity;
    private int spaceLeft;
    private Vehicle vehicle;
    private double cost;
    private double heuristic;
    public double totalCost;

    public DeliberativeState(Vehicle vehicle, TaskSet tasksLeft) {
        this.vehicle = vehicle;
        this.taskOnBoard = new HashSet<>(vehicle.getCurrentTasks());
        this.taskAvailable = new HashSet<>(tasksLeft);
        this.spaceLeft = this.vehicle.capacity() - this.getCapacityOccupied();
        this.currentCity = vehicle.getCurrentCity();
        this.heuristic = 0;
        this.totalCost = 0;
    }

    public void calculateHeuristic() {
        ArrayList<Double> costList = new ArrayList<>();

        for (Task t: this.taskOnBoard) {
            costList.add(currentCity.distanceTo(t.deliveryCity) * vehicle.costPerKm());
        }

        for (Task t: this.taskAvailable) {
            costList.add((currentCity.distanceTo(t.pickupCity) + t.pickupCity.distanceTo(t.deliveryCity)) * vehicle.costPerKm());
        }
        if (costList.isEmpty()) {
            this.heuristic = 0;
            return;
        }

        this.heuristic = Collections.max(costList);
    }

    public void calculateTotal(DeliberativeState lastState) {
        calculateHeuristic();

        if (lastState == null) {
            this.totalCost = this.heuristic;
            return;
        }

        this.totalCost = this.heuristic + lastState.getCost() + vehicle.costPerKm() * lastState.getCurrentCity().distanceTo(currentCity);
    }


    public DeliberativeState(HashSet<Task> taskOnBoard,
                             HashSet<Task> taskAvailable,
                             City currentCity,
                             int spaceLeft,
                             double cost,
                             Vehicle vehicle) {
        this.taskOnBoard = taskOnBoard;
        this.taskAvailable = taskAvailable;
        this.currentCity = currentCity;
        this.spaceLeft = spaceLeft;
        this.vehicle = vehicle;
        this.cost = cost;
    }

    public boolean removeTask(Task task) {
        return this.taskOnBoard.remove(task);
    }

    public boolean addTask(Task task) {
        if (task.weight > this.spaceLeft) {
            return false;
        }

        if( this.taskAvailable.remove(task) && this.taskOnBoard.add(task) ) {
            this.spaceLeft -= task.weight;
            return true;
        }
        return false;
    }

    public int getCapacityOccupied() {
        int capOccupied = 0;
        for (Task task: this.taskOnBoard) {
            capOccupied += task.weight;
        }
        return capOccupied;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public String toString() {
        return this.currentCity.toString() + this.taskOnBoard.toString() + this.taskAvailable.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        DeliberativeState ds = (DeliberativeState)obj;

        // City
        if (!this.currentCity.equals(ds.getCurrentCity())) {
            return false;
        }

        // TaskOnBoard
        if (!this.taskOnBoard.equals(ds.getTaskOnBoard())) {
            return false;
        }

        // TaskAvaiable
        if (!this.taskAvailable.equals(ds.getTaskAvailable())) {
            return false;
        }
        return true;
    }


    public HashSet<Task> getTaskOnBoard() {
        return taskOnBoard;
    }

    public void setTaskOnBoard(HashSet<Task> taskOnBoard) {
        this.taskOnBoard = taskOnBoard;
    }

    public HashSet<Task> getTaskAvailable() {
        return taskAvailable;
    }

    public void setTaskAvailable(HashSet<Task> taskAvailable) {
        this.taskAvailable = taskAvailable;
    }

    public City getCurrentCity() {
        return currentCity;
    }

    public void setCurrentCity(City currentCity) {
        this.currentCity = currentCity;
    }

    public int getSpaceLeft() {
        return spaceLeft;
    }

    public void setSpaceLeft(int spaceLeft) {
        this.spaceLeft = spaceLeft;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }


}
