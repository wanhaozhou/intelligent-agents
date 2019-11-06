package template;

import logist.agent.Agent;
import logist.plan.Action;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.*;

public class CentralizedSolver {
    CentralizedState centralizedState;
    Double globalCost;
    CentralizedState globalSolution;

    public ArrayList<Double> costRecords = new ArrayList<>();

    Topology topology;
    TaskDistribution distribution;
    Agent agent;

    List<Vehicle> vehicles;
    List<Task> tasks;

    Random random;

    public CentralizedSolver() {
        centralizedState = new CentralizedState();

        this.topology = CentralizedSetting.topology;
        this.distribution = CentralizedSetting.distribution;
        this.agent = CentralizedSetting.agent;
        this.random = new Random();
        this.globalCost = Double.MAX_VALUE;

        initSolution();
    }

    private void initSolution() {

        vehicles = new ArrayList<>(agent.vehicles());
        tasks = new ArrayList<>(CentralizedSetting.tasks);

        // init HashMap A
        for (Vehicle vehicle : vehicles) {
            centralizedState.A.put(vehicle, new ArrayList<>());
        }

        for (Task task : tasks) {
            Vehicle picked = vehicles.get(random.nextInt(vehicles.size()));
            centralizedState.A.get(picked).add(new CentralizedAction(task, true));
            centralizedState.A.get(picked).add(new CentralizedAction(task, false));
        }

        centralizedState.updateCost();
        globalCost = centralizedState.cost;
        globalSolution = centralizedState.copyState();

        costRecords.add(centralizedState.cost);

    }

    public void SLS(int numIter, double prob) {
        int count = 0;
        while (count++ < numIter) {
            if (count % 1000 == 0)
                System.out.println("iteration: " + count);
            CentralizedState oldState = centralizedState.copyState();
            List<CentralizedState> neighbours = chooseNeighbours(oldState);
            localChoice(neighbours, prob);
            if (centralizedState.cost < globalCost) {
                globalCost = centralizedState.cost;
                globalSolution = centralizedState.copyState();
            }

            costRecords.add(centralizedState.cost);
        }
        System.out.println("global cost:" + globalCost);
    }

    public List<Plan> generatePlan() {
        List<Plan> plans = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            List<CentralizedAction> actions = globalSolution.A.get(vehicle);
            plans.add(generatePlan(actions, vehicle));
        }
//        for (Plan p: plans) {
//            System.out.println("========");
//            for (Action a: p) {
//                System.out.println(a);
//            }
//        }
        return plans;
    }

    private Plan generatePlan(List<CentralizedAction> actions, Vehicle vehicle) {
        City current = vehicle.homeCity();
        Plan plan = new Plan(current);

        for (CentralizedAction action : actions) {
            if (action.isPickUp) {
                // move: current city => pickup location
                for (City city : current.pathTo(action.task.pickupCity)) {
                    plan.appendMove(city);
                }
                plan.appendPickup(action.task);
                current = action.task.pickupCity;
            } else {
                // move: pickup location => delivery location
                for (City city : current.pathTo(action.task.deliveryCity)) {
                    plan.appendMove(city);
                }
                plan.appendDelivery(action.task);
                current = action.task.deliveryCity;
            }
        }

        return plan;
    }

    private List<CentralizedState> chooseNeighbours(CentralizedState oldState) {
        List<CentralizedState> neighbours = new ArrayList<>();

        // randomly pick one vehicle that has at least one task
        Vehicle picked = vehicles.get(random.nextInt(vehicles.size()));
        while (oldState.A.get(picked).size() <= 0) {
            picked = vehicles.get(random.nextInt(vehicles.size()));
        }
//        System.out.println(neighbours.size());
//        changeVehicle(oldState, picked, neighbours);
//        System.out.println(neighbours.size());
        changeTaskOrder(oldState, picked, neighbours);
        changeVehicle(oldState, picked, neighbours);

//        System.out.println(neighbours.size());
//        for (CentralizedState s: neighbours) {
//            System.out.println("+++++++++");
//            for (Map.Entry<Vehicle, List<CentralizedAction>> V: s.A.entrySet()) {
//                System.out.println("*********");
//                for (CentralizedAction a: V.getValue()) {
//                    System.out.println(a.task);
//                }
//            }
//        }

        return neighbours;
    }

    private void changeVehicle(CentralizedState oldState,
                               Vehicle picked,
                               List<CentralizedState> neighbours) {

        // get the first task to remove
        Task taskToRemove = oldState.A.get(picked).get(0).task;
        // get the index of the delivery
        int deliveryInd = getTaskDeliveryIndex(oldState.A.get(picked), taskToRemove);

        CentralizedState candidateState = oldState.copyState();
        List<CentralizedAction> oldActions = candidateState.A.get(picked);
        oldActions.remove(deliveryInd);
        oldActions.remove(0);
        candidateState.A.put(picked, oldActions);


        for (Vehicle vehicle : vehicles) {
            if (vehicle.equals(picked))
                continue;
            CentralizedState newState = candidateState.copyState();
            List<CentralizedAction> tmpAction = new ArrayList<CentralizedAction>(newState.A.get(vehicle));


            tmpAction.add(0, new CentralizedAction(taskToRemove, false));
            tmpAction.add(0, new CentralizedAction(taskToRemove, true));

            newState.A.put(vehicle, tmpAction);
            newState.updateCost();

            neighbours.add(newState);
        }
    }

    private void changeTaskOrder(CentralizedState oldState,
                                 Vehicle picked,
                                 List<CentralizedState> neighbours) {
        if (oldState.A.get(picked).size() <= 2) {
//            System.out.println("Size is smaller than 2");
            neighbours.add(oldState.copyState());
            return;
        }

        Map<Task, List<Integer>> taskMapOfPicked = oldState.getTaskMapForVehicle(picked);
        List<CentralizedAction> oldActions = oldState.A.get(picked);

        /**
         * consider four cases for changing the task order:
         * 1. move pickup backward
         * 2. move delivery forward
         * These two we need to satisfy that a given task is delivered after we pick it up
         *
         * 3. move pickup forward
         * 4. move delivery backward
         *
         * These two we need to satisfy that the load does not exceed the maximum of a car
         *
         */
        for (Task task : taskMapOfPicked.keySet()) {
            int taskPickupInd = taskMapOfPicked.get(task).get(0);
            int taskDeliverInd = taskMapOfPicked.get(task).get(1);
            pickupForward(taskPickupInd, taskDeliverInd, oldState, picked, oldActions, taskMapOfPicked, neighbours);
            pickupBackward(taskPickupInd, taskDeliverInd, oldState, picked, oldActions, taskMapOfPicked, neighbours);
            deliveryForward(taskPickupInd, taskDeliverInd, oldState, picked, oldActions, taskMapOfPicked, neighbours);
            deliberyBackward(taskPickupInd, taskDeliverInd, oldState, picked, oldActions, taskMapOfPicked, neighbours);
        }


    }

    private boolean verify(List<CentralizedAction> actions, Vehicle picked) {
        double curr = picked.capacity();
        for (CentralizedAction action : actions) {
            if (action.isPickUp) {
                curr -= action.task.weight;
            } else {
                curr += action.task.weight;
            }
            if (curr < 0) return false;
        }
        return true;
    }

    private void pickupForward(int pickupIndex, int deliveryIndex,
                               CentralizedState oldState, Vehicle picked,
                               List<CentralizedAction> oldActions,
                               Map<Task, List<Integer>> taskMapOfPicked,
                               List<CentralizedState> neighbors) {
        for (int i = pickupIndex - 1; i >= 0; i--) {
            if (oldActions.get(i).isPickUp) {
                int swapDelivery =  taskMapOfPicked.get(oldActions.get(i).task).get(1);
                if (swapDelivery < pickupIndex) {
                    continue;
                }
            }

            List<CentralizedAction> newActions = new ArrayList<>(oldActions);
            newActions.set(pickupIndex, oldActions.get(i));
            newActions.set(i, oldActions.get(pickupIndex));

            if (!verify(newActions, picked)) {
                continue;
            }

            CentralizedState newState = oldState.copyState();

            newState.A.put(picked, newActions);
            newState.updateCost();

            neighbors.add(newState);

        }

    }

    private void pickupBackward(int pickupIndex, int deliveryIndex,
                                CentralizedState oldState, Vehicle picked,
                                List<CentralizedAction> oldActions,
                                Map<Task, List<Integer>> taskMapOfPicked,
                                List<CentralizedState> neighbors) {

        for (int i = pickupIndex + 1; i < deliveryIndex; i++) {
            if (!oldActions.get(i).isPickUp) {
                int swapPickup = taskMapOfPicked.get(oldActions.get(i).task).get(0);
                if (swapPickup > pickupIndex) {
                    continue;
                }
            }

            List<CentralizedAction> newActions = new ArrayList<>(oldActions);
            newActions.set(pickupIndex, oldActions.get(i));
            newActions.set(i, oldActions.get(pickupIndex));

            if (oldActions.get(i).isPickUp &&
                    oldActions.get(i).task.weight > oldActions.get(pickupIndex).task.weight) {
                if (!verify(newActions, picked))
                    continue;
            }

            CentralizedState newState = oldState.copyState();

            newState.A.put(picked, newActions);
            newState.updateCost();

            neighbors.add(newState);
        }
    }

    private void deliveryForward(int pickupIndex, int deliveryIndex,
                                 CentralizedState oldState, Vehicle picked,
                                 List<CentralizedAction> oldActions,
                                 Map<Task, List<Integer>> taskMapOfPicked,
                                 List<CentralizedState> neighbors) {

        for (int i = deliveryIndex - 1; i > pickupIndex; i--) {
            if (oldActions.get(i).isPickUp) {
                int swapDelivery =  taskMapOfPicked.get(oldActions.get(i).task).get(1);
                if (swapDelivery < deliveryIndex) {
                    continue;
                }
            }
            List<CentralizedAction> newActions = new ArrayList<>(oldActions);
            newActions.set(deliveryIndex, oldActions.get(i));
            newActions.set(i, oldActions.get(deliveryIndex));

            if (!oldActions.get(i).isPickUp &&
                    oldActions.get(i).task.weight > oldActions.get(deliveryIndex).task.weight) {
                if (!verify(newActions, picked))
                    continue;
            }

            CentralizedState newState = oldState.copyState();

            newState.A.put(picked, newActions);
            newState.updateCost();

            neighbors.add(newState);
        }
    }

    private void deliberyBackward(int pickupIndex, int deliveryIndex,
                                  CentralizedState oldState, Vehicle picked,
                                  List<CentralizedAction> oldActions,
                                  Map<Task, List<Integer>> taskMapOfPicked,
                                  List<CentralizedState> neighbors) {

        for (int i = deliveryIndex + 1; i < oldActions.size(); i++) {
            if (!oldActions.get(i).isPickUp) {
                int swapPickup = taskMapOfPicked.get(oldActions.get(i).task).get(0);
                if (swapPickup > deliveryIndex) {
                    continue;
                }
            }

            List<CentralizedAction> newActions = new ArrayList<>(oldActions);
            newActions.set(deliveryIndex, oldActions.get(i));
            newActions.set(i, oldActions.get(deliveryIndex));

            if (!verify(newActions, picked)) {
                continue;
            }

            CentralizedState newState = oldState.copyState();

            newState.A.put(picked, newActions);
            newState.updateCost();

            neighbors.add(newState);

        }

    }

    private void localChoice(List<CentralizedState> neighbours, double prob) {
        if (random.nextDouble() > prob) {
            centralizedState = getBestNeighbour(neighbours);
        } else {
            centralizedState = neighbours.get(random.nextInt(neighbours.size())).copyState();
        }
    }

    private CentralizedState getBestNeighbour(List<CentralizedState> neighbours) {
        Collections.shuffle(neighbours);
        Collections.sort(neighbours, new Comparator<CentralizedState>() {
            @Override
            public int compare(CentralizedState o1, CentralizedState o2) {
                if (o1.cost.equals(o2.cost)) return 0;
                else if (o1.cost < o2.cost) return -1;
                return 1;
            }
        });

        return neighbours.get(0).copyState();
    }

    private int getTaskDeliveryIndex(List<CentralizedAction> actions, Task target) {
        int res = -1;
        for (int i = 0; i < actions.size(); i++) {
            if (actions.get(i).task.equals(target) && !actions.get(i).isPickUp) return i;
        }
        return res;
    }
}
