package template;

import com.sun.xml.internal.stream.buffer.sax.DefaultWithLexicalHandler;
import logist.plan.Action;
import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.*;
import java.util.stream.Collectors;

public class DeliberativeDispatcher {

    private DeliberativeTrained.Algorithm algo;

    public DeliberativeDispatcher(DeliberativeTrained.Algorithm algo) {
        this.algo = algo;
    }

    public Plan generatePlan(DeliberativeState inputState) {
        switch (algo) {
            case BFS:
                return BFSPlan(inputState);
            case ASTAR:
                return ASTARPlan(inputState);
            default:
                throw new AssertionError("Should not happen.");
        }
    }


    public Plan BFSPlan(DeliberativeState inputState) {
        City currentCity = inputState.getVehicle().getCurrentCity();
        ArrayList<DeliberativeState> stateSeq = this.bfsSearch(inputState);
        ArrayList<Action> actions = this.convertStateToAction(stateSeq);

        return new Plan(currentCity, actions);
    }

    public Plan ASTARPlan(DeliberativeState inputState) {
        City currentCity = inputState.getVehicle().getCurrentCity();
        ArrayList<DeliberativeState> stateSeq = this.astarSearch(inputState);
        ArrayList<Action> actions = this.convertStateToAction(stateSeq);


        return new Plan(currentCity, actions);

    }


    public ArrayList<Action> convertStateToAction(ArrayList<DeliberativeState> states) {
        ArrayList<Action> actions = new ArrayList<>();

        for (int i=0; i<states.size()-1; i++) {
            DeliberativeState cur = states.get(i);
            DeliberativeState next = states.get(i + 1);

            if (cur.getCurrentCity() != next.getCurrentCity()) {
                actions.add(new Action.Move(next.getCurrentCity()));
            }
            else if (cur.getTaskAvailable().size() != next.getTaskAvailable().size()) {
                HashSet<Task> oldTask = (HashSet<Task>) cur.getTaskAvailable().clone();
                HashSet<Task> newTask = (HashSet<Task>) next.getTaskAvailable().clone();
                oldTask.removeAll(newTask);
                actions.add(new Action.Pickup((Task) oldTask.toArray()[0]));
            }
            else {
                HashSet<Task> oldTask = (HashSet<Task>) cur.getTaskOnBoard().clone();
                HashSet<Task> newTask = (HashSet<Task>) next.getTaskOnBoard().clone();
                oldTask.removeAll(newTask);
                actions.add(new Action.Delivery((Task) oldTask.toArray()[0]));
            }
        }

        //System.out.println(Arrays.toString(actions.toArray()));

        return actions;
    }

    public ArrayList<DeliberativeState> bfsSearch(DeliberativeState inputState) {
        double costRecord = Double.MAX_VALUE;
        ArrayList<DeliberativeState> finalPath = new ArrayList<>();
        Queue<ArrayList<DeliberativeState>> tree = new LinkedList<>();

        ArrayList<DeliberativeState> start = new ArrayList<>();
        start.add(inputState);
        tree.add(start);

        HashSet<DeliberativeState> visited = new HashSet<>();

        while (!tree.isEmpty()) {
           ArrayList<DeliberativeState> currentPath = tree.poll();
           DeliberativeState currentState = currentPath.get(currentPath.size()-1);
           visited.add(currentState);

           if (currentState.getTaskAvailable().size() == 0 && currentState.getTaskOnBoard().size() == 0) {
               if (currentState.getCost() < costRecord) {
                   costRecord = currentState.getCost();
                   finalPath = currentPath;
               }
               continue;
           }

           ArrayList<DeliberativeState> children = this.getChildren(currentState);

           for (DeliberativeState s: children) {
               if (visited.contains(s)) {
                   continue;
               }
               ArrayList<DeliberativeState> newPath = new ArrayList<>(currentPath);
               newPath.add(s);
               tree.add(newPath);
           }
        }
        return finalPath;
    }

    public ArrayList<DeliberativeState> astarSearch(DeliberativeState inputState) {
        ArrayList<DeliberativeState> finalPath = new ArrayList<>();
        PriorityQueue<ArrayList<DeliberativeState>> tree = new PriorityQueue<>(
                new Comparator<ArrayList<DeliberativeState>>() {
                    @Override
                    public int compare(ArrayList<DeliberativeState> o1, ArrayList<DeliberativeState> o2) {
                        if (o1.isEmpty() && o2.isEmpty()) {
                            return 0;
                        }
                        else if (o1.isEmpty()) {
                            return -1;
                        }
                        else if (o2.isEmpty()) {
                            return 1;
                        }

                        Double cost1 = o1.get(o1.size()-1).totalCost;
                        Double cost2 = o2.get(o2.size()-1).totalCost;

                        return cost1.compareTo(cost2);
                    }
                }
        );


        HashMap<DeliberativeState, Double> visited = new HashMap<>();
        ArrayList<DeliberativeState> start = new ArrayList<>();

        // calculate cost
        inputState.calculateTotal(null);
        start.add(inputState);
        tree.add(start);


        while (!tree.isEmpty()) {
            ArrayList<DeliberativeState> currentPath = tree.poll();
            DeliberativeState currentState = currentPath.get(currentPath.size()-1);
            visited.put(currentState, currentState.totalCost);

            if (currentState.getTaskAvailable().size() == 0 && currentState.getTaskOnBoard().size() == 0) {
                return currentPath;
            }

            ArrayList<DeliberativeState> children = this.getChildren(currentState);

            for (DeliberativeState s: children) {
                s.calculateTotal(currentState);
                if (visited.containsKey(s) && visited.get(s) < s.totalCost) {
                    continue;
                }
                ArrayList<DeliberativeState> newPath = new ArrayList<>(currentPath);
                newPath.add(s);
                tree.add(newPath);
            }
        }

        System.out.println("ERROR");
        return finalPath;
    }

    public ArrayList<DeliberativeState> getChildren(DeliberativeState currentState) {
        ArrayList<DeliberativeState> childrenStates = new ArrayList<>();

        // deliver tasks
        ArrayList<Task> deliverStates = this.getDeliveryCurrentCity(currentState);

        if (deliverStates.size() != 0) {
            Task taskToDeliver = deliverStates.get(0);
            DeliberativeState nextState = this.cloneState(currentState);
            if (nextState.removeTask(taskToDeliver)) {
                childrenStates.add(nextState);
                return childrenStates;
            }
        }

        // pick up tasks
        ArrayList<Task> pickupStates = this.getPickupCurrentCity(currentState);

        for (Task t: pickupStates) {
            DeliberativeState nextState = this.cloneState(currentState);
            if (nextState.addTask(t)) {
                childrenStates.add(nextState);
            }
        }


        // move to next city
        ArrayList<City> nextCities = this.getNextCities(currentState);

        for (City city: nextCities) {
            DeliberativeState nextState = this.cloneState(currentState);
            double currCost = nextState.getCost();
            double newCost = nextState.getVehicle().costPerKm() * nextState.getCurrentCity().distanceTo(city);

            // update cost and currentCity
            nextState.setCost(currCost + newCost);
            nextState.setCurrentCity(city);

            childrenStates.add(nextState);
        }

        return childrenStates;

    }

    public DeliberativeState cloneState(DeliberativeState currentState) {
        return new DeliberativeState((HashSet<Task>) currentState.getTaskOnBoard().clone(),
                                        (HashSet<Task>) currentState.getTaskAvailable().clone(),
                                        currentState.getCurrentCity(),
                                        currentState.getSpaceLeft(),
                                        currentState.getCost(),
                                        currentState.getVehicle());
    }

    public ArrayList<Task> getPickupCurrentCity(DeliberativeState currentState) {
        return currentState.getTaskAvailable().stream().filter(x ->
            x.pickupCity.equals(currentState.getCurrentCity()) && (x.weight <= currentState.getSpaceLeft())
        ).collect(Collectors.toCollection(ArrayList::new));

    }

    public ArrayList<Task> getDeliveryCurrentCity(DeliberativeState currentState) {
        return currentState.getTaskOnBoard().stream().filter(x ->
                x.deliveryCity.equals(currentState.getCurrentCity()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public ArrayList<City> getNextCities(DeliberativeState currentState) {
        return currentState.getCurrentCity().neighbors().stream().filter(
                x -> this.isNeighborPickup(x, currentState) || this.isNeighborDelivery(x, currentState)
        ).collect(Collectors.toCollection(ArrayList::new));
    }

    public boolean isNeighborPickup(City city, DeliberativeState currentState) {
        City currCity = currentState.getCurrentCity();
        return currentState.getTaskAvailable().stream().anyMatch(x ->
                currCity.pathTo(x.pickupCity).size() > 0 && currCity.pathTo(x.pickupCity).get(0).equals(city));
    }

    public boolean isNeighborDelivery(City city, DeliberativeState currentState) {
        City currCity = currentState.getCurrentCity();
        return currentState.getTaskOnBoard().stream().anyMatch(x ->
                currCity.pathTo(x.deliveryCity).size() > 0 && currCity.pathTo(x.deliveryCity).get(0).equals(city));
    }

}
