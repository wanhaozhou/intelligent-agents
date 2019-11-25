package bidder;

import helpers.VehicleModel;
import logist.plan.Action;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;
import planner.PlanAction;
import planner.PlanSetting;
import planner.PlanSolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Bidder {
    private List<VehicleModel> vehicles;
    private List<City> homecities;

    private Topology topology;
    private TaskDistribution taskDistribution;

    private List<Task> tasks;

    private PlanSolver planSolver;

    private double cost;
    public double totalBid;
    private int minCostPerKm = Integer.MAX_VALUE;
    private int maxCapacity = 0;

    private final int MINNUM_TASK = 10;

    public Bidder(List<VehicleModel> vehicles) {
        this.vehicles = vehicles;
        this.homecities = new ArrayList<>();
        this.tasks = new ArrayList<>();

        for (VehicleModel v: this.vehicles) {
            this.homecities.add(v.homeCity());
        }

        this.topology = PlanSetting.topology;
        this.taskDistribution = PlanSetting.distribution;

        this.planSolver = new PlanSolver(this.vehicles);

        for (VehicleModel v: this.vehicles) {
            this.minCostPerKm = Math.min(v.costPerKm(), this.minCostPerKm);
            this.maxCapacity = Math.max(v.capacity(), this.maxCapacity);
        }

        this.cost = 0.0;
        this.totalBid = 0.0;
    }

    public Bidder(List<City> cities, int capacity, int costPerKM) {
        this.vehicles = new ArrayList<>();
        this.homecities = new ArrayList<>();
        this.tasks = new ArrayList<>();

        int id = 0;
        for (City city : cities) {
            this.homecities.add(city);
            this.vehicles.add(new VehicleModel(id++, capacity, costPerKM, city));
        }

        this.topology = PlanSetting.topology;
        this.taskDistribution = PlanSetting.distribution;

        this.planSolver = new PlanSolver(this.vehicles);

        this.minCostPerKm = costPerKM;
        this.maxCapacity = capacity;

        this.cost = 0.0;
        this.totalBid = 0.0;
    }

    public double predictMC(Task task) {
        return this.planSolver.predictCost(task);
    }

    public double predictAvgCost() {
        int numTask = tasks.size();

        if (numTask < MINNUM_TASK) {
            if (numTask == 0) {
                List<Task> randomTasks = PlanSetting.generateRandomTask(1, MINNUM_TASK-numTask);
                return planSolver.predictNextAvgCost(randomTasks);
            }
            int startID = tasks.get(tasks.size() - 1).id + 1;
            List<Task> randomTasks = PlanSetting.generateRandomTask(startID, MINNUM_TASK-numTask);
            return planSolver.predictNextAvgCost(randomTasks);
        }
        else {
            return planSolver.getAvg();
        }
    }

    public double dummyEstimateCost(int numTasks, int numIter) {
        planSolver.NUM_ITER_MAX = numIter;
        List<Task> randomTasks = PlanSetting.generateRandomTask(1, numTasks);
        return planSolver.predictNextAvgCost(randomTasks);
    }

    public void acceptTask(Task task) {
        tasks.add(task);
        planSolver.acceptTask(task);
    }

    public double getCost() {
        return cost;
    }

    public double getTotalBid() {
        return totalBid;
    }

    public int getMinCostPerKm() {
        return minCostPerKm;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }


    public int getAvgCostPerKM() {
        int currCost = 0;
        for (VehicleModel v : vehicles) {
            currCost += v.costPerKm();
        }
        return currCost / vehicles.size();
    }

    public int getAvgCapacity() {
        int capacity = 0;
        for (VehicleModel v : vehicles) {
            capacity += v.capacity();
        }
        return capacity / vehicles.size();
    }

    public static Bidder createOpBidder(int id, int numVehicles, int capacity, int cost,
                                        City initialCity, List<City> distantCities) {
        List<City> opCities = new ArrayList<>();

        int numCity = distantCities.size();
        List<Integer> indices = new ArrayList<>();

        for (int i = 0; i < numCity; i++) {
            indices.add(i);
        }

        for (int i = 0; i < Math.min(numCity, numVehicles); i++) {
            int index = (int) Math.random()*indices.size();
            opCities.add(distantCities.get(indices.get(index)));
            indices.remove(index);
        }

        if ( initialCity!=null && !opCities.contains(initialCity))
            opCities.add(initialCity);

        return new Bidder(opCities, capacity, cost);

    }

    public void addVehicleAt(City city) {
        //int id, int capacity, int costPerKm, City homeCity
        System.out.println("add vehicle");
        this.vehicles.add(new VehicleModel(this.vehicles.size(), this.getAvgCapacity(), this.getAvgCostPerKM(), city));
        this.homecities.add(city);
        planSolver = new PlanSolver(vehicles);
        planSolver.setTasks(new ArrayList<>(this.tasks));
        for (Task task : tasks) {
            System.out.println(task.toString());
        }
        planSolver.initSolNewCar(this.tasks);
        System.out.println("begin SLS add vehicle");
        if (tasks.size() > 0) {
            planSolver.SLS(2000, 0.3);
        }
    }

    public List<City> getHomecities() {
        return homecities;
    }

    public List<Plan> generatePlan() {
        return planSolver.generatePlan();
    }

    public Map<VehicleModel, List<PlanAction>> getPlanAction() {
        return planSolver.getGlobalPlanAction();
    }

    public Double getNextCost() {
        return this.planSolver.getNextGlobalCost();
    }

}
