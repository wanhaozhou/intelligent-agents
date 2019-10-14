package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;


public class ReactiveTrained implements ReactiveBehavior {

    private int numActions;
    private Agent myAgent;
    private ArrayList<City> cities;
    private ArrayList<ReactiveState> states;
    private HashMap<Vehicle, ReactiveControl> policy;
    private final Double EPSILON = 0.1;


    @Override
    public void setup(Topology topology, TaskDistribution td, Agent agent) {

        // Reads the discount factor from the agents.xml file.
        // If the property is not present it defaults to 0.95
        Double discount = agent.readProperty("discount-factor",
                                                Double.class,
                                                0.95);

        numActions = 0;
        myAgent = agent;
        cities = new ArrayList<>(topology.cities());
        states = generateStates();
        policy = new HashMap<>();

        long startTime = System.nanoTime();
        for (Vehicle v: myAgent.vehicles()) {
            policy.putIfAbsent(v, new ReactiveControl(v,
                    topology, discount, td, states, EPSILON));
        }

        long endTime = System.nanoTime();

        long timeElapsed = endTime - startTime;

        System.out.println("Execution time in milliseconds : " +
                timeElapsed / 1000000);
    }


    @Override
    public Action act(Vehicle vehicle, Task availableTask) {
        Action action;
        HashMap<ReactiveState, City> p = policy.get(vehicle).getPolicy();
        City currentCity = vehicle.getCurrentCity();
        ReactiveState currentState;

        if (availableTask != null) {
            currentState = new ReactiveState(currentCity, availableTask.deliveryCity);
        } else {
            currentState = new ReactiveState(currentCity, null);
            return new Move(p.get(currentState));
        }
        City a = p.get(currentState);

        if (a.equals(availableTask.deliveryCity)) {
            action = new Pickup(availableTask);
        } else {
            action = new Move(a);
        }

        if (numActions >= 1) {
            System.out.println("The total profit after "+
                                numActions+
                                " actions is "+
                                myAgent.getTotalProfit()+
                                " (average profit: "+
                                (myAgent.getTotalProfit() / (double)numActions)+")");
        }
        numActions++;

        return action;
    }


    /**
     * ReactiveState.currentCity: currently city
     * ReactiveState.packageDest: destination of package
     * ReactiveState.actions: array list of possible next hops
     * @return All possible reactive states pairs
     */
    private ArrayList<ReactiveState> generateStates() {
        ArrayList<ReactiveState> result = new ArrayList<>();
        for (City c: this.cities) {
            for (City d: this.cities) {
                if (!c.equals(d)) {
                    result.add(new ReactiveState(c, d));
                }
            }
            result.add(new ReactiveState(c, null));
        }

        for (ReactiveState rc: result) {
            for (City c: rc.getCurrentCity().neighbors()) {
                rc.getActions().add(c);
            }
            if ((!rc.getActions().contains(rc.getPackageDest())) && (rc.getPackageDest() != null)) {
                rc.getActions().add(rc.getPackageDest());
            }
        }

        return result;
    }
}

