package planner;

import helpers.VehicleModel;
import logist.agent.Agent;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.*;


public class PlanSolver {
    public final int NUM_ITER = 2000;
    private final double PROB = 0.3;
    public int NUM_ITER_MAX = 2000;


    PlanState planState;
    Double globalCost;
    PlanState globalSolution;

    Double nextCost;
    PlanState nextSolution;

    Double nextGlobalCost;
    PlanState nextGlobalSolution;

    public ArrayList<Double> costRecords = new ArrayList<>();

    Topology topology;
    TaskDistribution distribution;

    List<VehicleModel> vehicles;
    List<Task> tasks;

    Random random;

    public PlanSolver(List<VehicleModel> vms) {
        planState = new PlanState();

        this.topology = PlanSetting.topology;
        this.distribution = PlanSetting.distribution;
        this.vehicles = vms;
        this.random = new Random();
        this.globalCost = Double.MAX_VALUE;
        this.tasks = new ArrayList<>();

        initSolution();
    }

    private void initSolution() {

        // init HashMap A
        for (VehicleModel vehicle : vehicles) {
            planState.A.put(vehicle, new ArrayList<>());
        }

        planState.updateCost();
        globalCost = planState.cost;
        globalSolution = planState.copyState();

        nextSolution = globalSolution.copyState();
        nextCost = globalCost;

        nextGlobalSolution = globalSolution.copyState();
        nextGlobalCost = globalCost;

        costRecords.add(planState.cost);

    }

    public void initSolNewCar(List<Task> tasks) {
        for (Task task : tasks) {
            VehicleModel picked = vehicles.get(random.nextInt(vehicles.size()));
            planState.A.get(picked).add(new PlanAction(task, true));
            planState.A.get(picked).add(new PlanAction(task, false));
        }

        planState.updateCost();
        globalCost = planState.cost;
        globalSolution = planState.copyState();

        nextSolution = globalSolution.copyState();
        nextCost = globalCost;

        nextGlobalSolution = globalSolution.copyState();
        nextGlobalCost = globalCost;

        costRecords.add(planState.cost);

    }

    public double predictCost(Task task){
        // task list add new task
        // generate new plan
        // margin cost
        // remove task from the list

        predictSLS(NUM_ITER, PROB, task);

        return nextGlobalCost - globalCost;
    }

    public double predictNextAvgCost(List<Task> tasks) {
        return predictSLS(NUM_ITER, PROB, tasks);
    }

    public void acceptTask(Task task) {
        globalSolution = nextGlobalSolution.copyState();
        globalCost = nextGlobalCost;
        tasks.add(task);
    }

    public double getAvg() {
        return globalCost / tasks.size();
    }

    public void predictSLS(int numIter, double prob, Task task) {
        nextSolution = globalSolution.copyState();
        for (VehicleModel v: vehicles) {
            if (task.weight <= v.capacity()) {

                List<PlanAction> tmpAction = new ArrayList<PlanAction>(nextSolution.A.get(v));
                tmpAction.add(0, new PlanAction(task, false));
                tmpAction.add(0, new PlanAction(task, true));

                nextSolution.A.put(v, tmpAction);
                nextSolution.updateCost();
                nextCost = nextSolution.cost;
                break;
            }
        }

        // SLS update
        int count = 0;

        nextGlobalCost = nextSolution.cost;
        nextGlobalSolution = nextSolution.copyState();

        int taskNum = tasks.size() + 1;
        numIter = Math.min(NUM_ITER_MAX, 3 * taskNum * taskNum * taskNum);

        while (count++ < numIter) {
            PlanState oldState = nextSolution.copyState();
            List<PlanState> neighbours = chooseNeighbours(oldState);
            nextSolution = localChoiceUpdate(neighbours, prob);
            if (nextSolution.cost < nextGlobalCost) {
                nextGlobalCost = nextSolution.cost;
                nextGlobalSolution = nextSolution.copyState();
            }
        }
    }

    public double predictSLS(int numIter, double prob, List<Task> continuedTasks) {
        PlanState currentSolution = globalSolution.copyState();

        for (Task task: continuedTasks) {
            for (VehicleModel v: vehicles) {
                if (task.weight <= v.capacity()) {
                    List<PlanAction> tmpAction = new ArrayList<PlanAction>(currentSolution.A.get(v));
                    tmpAction.add(0, new PlanAction(task, false));
                    tmpAction.add(0, new PlanAction(task, true));

                    currentSolution.A.put(v, tmpAction);
                    currentSolution.updateCost();
                    break;
                }
            }
        }

        // SLS update
        int count = 0;

        double bestCost = Double.MAX_VALUE;

        int taskNum = tasks.size() + continuedTasks.size();
        numIter = Math.min(NUM_ITER_MAX, 3 * taskNum * taskNum * taskNum);

        while (count++ < numIter) {
            PlanState oldState = currentSolution.copyState();
            List<PlanState> neighbours = chooseNeighbours(oldState);
            currentSolution = localChoiceUpdate(neighbours, prob);
            if (currentSolution.cost < bestCost) {
                bestCost = currentSolution.cost;
            }
        }

        return bestCost / (this.tasks.size() + continuedTasks.size());
    }

    private PlanState localChoiceUpdate(List<PlanState> neighbours, double prob) {
        if (random.nextDouble() > prob) {
            return getBestNeighbour(neighbours);
        } else {
            return neighbours.get(random.nextInt(neighbours.size())).copyState();
        }
    }



    public List<Plan> generatePlan() {
        List<Plan> plans = new ArrayList<>();
        for (VehicleModel vehicle : vehicles) {
            List<PlanAction> actions = globalSolution.A.get(vehicle);
            plans.add(generatePlan(actions, vehicle));
        }
        return plans;
    }

    private Plan generatePlan(List<PlanAction> actions, VehicleModel vehicle) {
        City current = vehicle.homeCity();
        Plan plan = new Plan(current);

        for (PlanAction action : actions) {
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

    private void printSolution(HashMap<VehicleModel, List<PlanAction>> A) {
        List<Plan> plans = new ArrayList<>();
        for (VehicleModel vehicle : vehicles) {
            List<PlanAction> actions = A.get(vehicle);
            plans.add(generatePlan(actions, vehicle));
        }
        for (Plan plan : plans) {
            System.out.println(plan.toString());
        }
    }

    private List<PlanState> chooseNeighbours(PlanState oldState) {
        List<PlanState> neighbours = new ArrayList<>();

        // randomly pick one vehicle that has at least one task
        VehicleModel picked = vehicles.get(random.nextInt(vehicles.size()));
        while (oldState.A.get(picked).size() <= 0) {
            picked = vehicles.get(random.nextInt(vehicles.size()));
        }
//        System.out.println(neighbours.size());
//        changeVehicle(oldState, picked, neighbours);
//        System.out.println(neighbours.size());
        changeTaskOrder(oldState, picked, neighbours);
        changeVehicle(oldState, picked, neighbours);

//        System.out.println(neighbours.size());
//        for (planState s: neighbours) {
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

    private void changeVehicle(PlanState oldState,
                               VehicleModel picked,
                               List<PlanState> neighbours) {

        // get the first task to remove
        Task taskToRemove = oldState.A.get(picked).get(0).task;
        // get the index of the delivery
        int deliveryInd = getTaskDeliveryIndex(oldState.A.get(picked), taskToRemove);

        PlanState candidateState = oldState.copyState();
        List<PlanAction> oldActions = candidateState.A.get(picked);
        oldActions.remove(deliveryInd);
        oldActions.remove(0);
        candidateState.A.put(picked, oldActions);


        for (VehicleModel vehicle : vehicles) {
            if (vehicle.equals(picked))
                continue;
            PlanState newState = candidateState.copyState();
            List<PlanAction> tmpAction = new ArrayList<PlanAction>(newState.A.get(vehicle));


            tmpAction.add(0, new PlanAction(taskToRemove, false));
            tmpAction.add(0, new PlanAction(taskToRemove, true));

            newState.A.put(vehicle, tmpAction);
            newState.updateCost();

            neighbours.add(newState);
        }
    }

    private void changeTaskOrder(PlanState oldState,
                                 VehicleModel picked,
                                 List<PlanState> neighbours) {
        if (oldState.A.get(picked).size() <= 2) {
//            System.out.println("Size is smaller than 2");
            neighbours.add(oldState.copyState());
            return;
        }

        Map<Task, List<Integer>> taskMapOfPicked = oldState.getTaskMapForVehicle(picked);
        List<PlanAction> oldActions = oldState.A.get(picked);

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

    private boolean verify(List<PlanAction> actions, VehicleModel picked) {
        double curr = picked.capacity();
        for (PlanAction action : actions) {
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
                               PlanState oldState, VehicleModel picked,
                               List<PlanAction> oldActions,
                               Map<Task, List<Integer>> taskMapOfPicked,
                               List<PlanState> neighbors) {
        for (int i = pickupIndex - 1; i >= 0; i--) {
            if (oldActions.get(i).isPickUp) {
                int swapDelivery =  taskMapOfPicked.get(oldActions.get(i).task).get(1);
                if (swapDelivery < pickupIndex) {
                    continue;
                }
            }

            List<PlanAction> newActions = new ArrayList<>(oldActions);
            newActions.set(pickupIndex, oldActions.get(i));
            newActions.set(i, oldActions.get(pickupIndex));

            if (!verify(newActions, picked)) {
                continue;
            }

            PlanState newState = oldState.copyState();

            newState.A.put(picked, newActions);
            newState.updateCost();

            neighbors.add(newState);

        }

    }

    private void pickupBackward(int pickupIndex, int deliveryIndex,
                                PlanState oldState, VehicleModel picked,
                                List<PlanAction> oldActions,
                                Map<Task, List<Integer>> taskMapOfPicked,
                                List<PlanState> neighbors) {

        for (int i = pickupIndex + 1; i < deliveryIndex; i++) {
            if (!oldActions.get(i).isPickUp) {
                int swapPickup = taskMapOfPicked.get(oldActions.get(i).task).get(0);
                if (swapPickup > pickupIndex) {
                    continue;
                }
            }

            List<PlanAction> newActions = new ArrayList<>(oldActions);
            newActions.set(pickupIndex, oldActions.get(i));
            newActions.set(i, oldActions.get(pickupIndex));

            if (oldActions.get(i).isPickUp &&
                    oldActions.get(i).task.weight > oldActions.get(pickupIndex).task.weight) {
                if (!verify(newActions, picked))
                    continue;
            }

            PlanState newState = oldState.copyState();

            newState.A.put(picked, newActions);
            newState.updateCost();

            neighbors.add(newState);
        }
    }

    private void deliveryForward(int pickupIndex, int deliveryIndex,
                                 PlanState oldState, VehicleModel picked,
                                 List<PlanAction> oldActions,
                                 Map<Task, List<Integer>> taskMapOfPicked,
                                 List<PlanState> neighbors) {

        for (int i = deliveryIndex - 1; i > pickupIndex; i--) {
            if (oldActions.get(i).isPickUp) {
                int swapDelivery =  taskMapOfPicked.get(oldActions.get(i).task).get(1);
                if (swapDelivery < deliveryIndex) {
                    continue;
                }
            }
            List<PlanAction> newActions = new ArrayList<>(oldActions);
            newActions.set(deliveryIndex, oldActions.get(i));
            newActions.set(i, oldActions.get(deliveryIndex));

            if (!oldActions.get(i).isPickUp &&
                    oldActions.get(i).task.weight > oldActions.get(deliveryIndex).task.weight) {
                if (!verify(newActions, picked))
                    continue;
            }

            PlanState newState = oldState.copyState();

            newState.A.put(picked, newActions);
            newState.updateCost();

            neighbors.add(newState);
        }
    }

    private void deliberyBackward(int pickupIndex, int deliveryIndex,
                                  PlanState oldState, VehicleModel picked,
                                  List<PlanAction> oldActions,
                                  Map<Task, List<Integer>> taskMapOfPicked,
                                  List<PlanState> neighbors) {

        for (int i = deliveryIndex + 1; i < oldActions.size(); i++) {
            if (!oldActions.get(i).isPickUp) {
                int swapPickup = taskMapOfPicked.get(oldActions.get(i).task).get(0);
                if (swapPickup > deliveryIndex) {
                    continue;
                }
            }

            List<PlanAction> newActions = new ArrayList<>(oldActions);
            newActions.set(deliveryIndex, oldActions.get(i));
            newActions.set(i, oldActions.get(deliveryIndex));

            if (!verify(newActions, picked)) {
                continue;
            }

            PlanState newState = oldState.copyState();

            newState.A.put(picked, newActions);
            newState.updateCost();

            neighbors.add(newState);

        }

    }

    private void localChoice(List<PlanState> neighbours, double prob) {
        if (random.nextDouble() > prob) {
            planState = getBestNeighbour(neighbours);
        } else {
            planState = neighbours.get(random.nextInt(neighbours.size())).copyState();
        }
    }

    private PlanState getBestNeighbour(List<PlanState> neighbours) {
        Collections.shuffle(neighbours);
        Collections.sort(neighbours, new Comparator<PlanState>() {
            @Override
            public int compare(PlanState o1, PlanState o2) {
                if (o1.cost.equals(o2.cost)) return 0;
                else if (o1.cost < o2.cost) return -1;
                return 1;
            }
        });

        return neighbours.get(0).copyState();
    }

    private int getTaskDeliveryIndex(List<PlanAction> actions, Task target) {
        int res = -1;
        for (int i = 0; i < actions.size(); i++) {
            if (actions.get(i).task.equals(target) && !actions.get(i).isPickUp) return i;
        }
        return res;
    }

    public void SLS(int numIter, double prob) {
        int count = 0;

        int taskNum = tasks.size() + 1;
        numIter = Math.min(NUM_ITER_MAX, 3 * taskNum * taskNum * taskNum);

        while (count++ < numIter) {
            PlanState oldState = planState.copyState();
            List<PlanState> neighbours = chooseNeighbours(oldState);
            localChoice(neighbours, prob);
            if (planState.cost < globalCost) {
                globalCost = planState.cost;
                globalSolution = planState.copyState();
            }

            costRecords.add(planState.cost);
        }
        System.out.println("global cost:" + globalCost);
    }

    public void SLSGlobal(int numIter, double prob) {
        int count = 0;

        int taskNum = tasks.size() + 1;
        numIter = Math.min(NUM_ITER_MAX, 3 * taskNum * taskNum * taskNum);
        nextSolution = globalSolution.copyState();

        while (count++ < numIter) {
            PlanState oldState = nextSolution.copyState();
            List<PlanState> neighbours = chooseNeighbours(oldState);
            nextSolution = localChoiceUpdate(neighbours, prob);
            if (nextSolution.cost < globalCost) {
                globalCost = nextSolution.cost;
                globalSolution = nextSolution.copyState();
            }

//            costRecords.add(globalSolution.cost);
        }
//        System.out.println("global cost:" + globalCost);
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    public Map<VehicleModel, List<PlanAction>> getGlobalPlanAction(){
        SLSGlobal(2000, 0.5);
//        globalSolution.updateCost();
//        System.out.println("global cost 2:" + globalSolution.cost);
        return globalSolution.copyState().A;
    }

    public Double getNextGlobalCost() {
        return nextGlobalCost;
    }
}
