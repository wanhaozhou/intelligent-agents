package template;

//the list of imports
import java.util.*;

import helpers.VehicleModel;
import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.agent.Agent;
import logist.plan.Action;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import planner.PlanAction;
import planner.PlanSetting;

import bidder.Bidder;
import bidder.CityEstimator;


public class AuctionTrained implements AuctionBehavior {

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;

    private Bidder bidderSelf;
    private Bidder bidderOp;

    private CityEstimator estimator;

    private double selfRatio;
    private double opRatio;

    private int round;
    private int acceptedTask = 0;

    private final double DEFAULT_RATIO = 1;
    private final double UPPER_RATIO = 1.5;
    private final double LOWER_RATIO = 0.8;

    private double marginSelf;
    private double marginOp;

//    private int ESTIMATE = 0;
    private final double GROWTH_COEF = 1.2;
    private final double GROWTH_RATE = 0.2;
    private final double SIN_COEF = 0.15;
    private final int SIN_T = 15;


    @Override
    public void setup(Topology topology, TaskDistribution distribution,
                      Agent agent) {

        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;

        PlanSetting.init(topology, distribution);
        this.estimator = new CityEstimator(topology.cities());

        this.bidderSelf = new Bidder(VehicleModel.agentToVehicles(agent));
        this.selfRatio = DEFAULT_RATIO;
        this.opRatio = DEFAULT_RATIO;
        this.round = 0;

//        Bidder dummySelf = new Bidder(VehicleModel.agentToVehicles(agent));
//        ESTIMATE = (int) dummySelf.dummyEstimateCost(25, 10000);

    }

    @Override
    public Long askPrice(Task task) {
        System.out.println("ask price:" + round);

        // if the task is larger than ny capacity, return
        if (task.weight > this.bidderSelf.getMaxCapacity()) {
            System.out.println("The task is too heavy!");
            return null;
        }

        if (this.round == 0) {
            marginSelf = bidderSelf.predictMC(task);
            round++;
//            System.out.println("For the first round, bid= " + Math.round(0.8 * marginSelf));
            double taskCost = task.pathLength() * bidderSelf.getMinCostPerKm();
            System.out.println("For the first round, bid= " + Math.round(0.7 * taskCost));
            return Math.round(0.7 * taskCost);
        }


        double bidSelf = estimateSelfMC(task);;
        double bidOp = estimateOpMC(task, bidSelf);

        System.out.println("========================");
        System.out.println("Round " + round);
        System.out.println("self bid is " + bidSelf);
        System.out.println("op bid is " + bidOp);

        bidSelf = Math.min(bidSelf, bidOp*1.2);

        System.out.println();
        System.out.println("Math.min(bidSelf, bidOp*1.2)");
        System.out.println("self bid is " + bidSelf);

        double growth = GROWTH_COEF / (1 + Math.exp(-round * GROWTH_RATE));
        double periodic = SIN_COEF * (Math.abs(Math.sin(round * 2 * Math.PI / SIN_T))) + 1;

        bidSelf *= growth;
        bidSelf *= periodic;

        System.out.println("growth:" + growth);
        System.out.println("periodic:" + periodic);

        if (bidSelf <= 10)
            bidSelf = task.pathLength() * bidderSelf.getMinCostPerKm() * 1;

        System.out.println("update self bid 2");
        System.out.println("self bid is " + bidSelf);
        System.out.println();
        round++;

        bidSelf = Math.max(bidSelf, 0.2 * (bidderSelf.getNextCost() - bidderSelf.totalBid));

        System.out.println("bidderSelf.getNextCost()" + bidderSelf.getNextCost());
        System.out.println("bidderSelf.totalBid" + bidderSelf.totalBid);
        System.out.println("==========================");

//        if (round >= 10) {
//            return Math.round((bidSelf + bidOp) / 2);
//        }

        return Math.round(bidSelf);

    }

    @Override
    public void auctionResult(Task previous, int winner, Long[] bids) {
        System.out.println("begin auction result, round: " + round);
        if (this.round == 1) {
            for (int i = 0; i < bids.length; i++) {
                if (i != agent.id()) {
                    City opInitialCity = this.estimator.estimateCity(bids[i], previous, bidderSelf.getAvgCostPerKM());
                    bidderOp = Bidder.createOpBidder(i, agent.vehicles().size(),
                                                    bidderSelf.getAvgCapacity(),
                                                    bidderSelf.getAvgCostPerKM(),
                                                    opInitialCity,
                                                    estimator.getDistantCities());
                }
            }
        } else {
            for (int i = 0; i < bids.length; i++) {
                if (i != agent.id()) {
                    double bidOp = bids[i];
                    double marginCost = marginOp;
                    if (marginCost < bidOp) {
                        System.out.println("marginCost < bidOp");
                        this.opRatio = Math.min(opRatio + 0.1, UPPER_RATIO);
                    } else if (bidOp <= marginCost && marginCost * opRatio < bidOp) {
                        // close
                        System.out.println("bidOp <= marginCost && marginCost * opRatio < bidOp");
                        double avgCost = 0.5 * (marginCost + marginCost*opRatio);
                        if (bidOp > avgCost) {
                            System.out.println("bidOp > avgCost");
                            opRatio = Math.min(opRatio + 0.03, UPPER_RATIO);
                        }
                        else {
                            System.out.println("else");
                            opRatio = Math.max(opRatio - 0.03, LOWER_RATIO);
                        }
                    } else {
//                      if our prediction of op is far from the true case, we need to re-predict
                        System.out.println("if our prediction of op is far from the true case, we need to re-predict");
                        if (!bidderOp.getHomecities().contains(previous.pickupCity)) {
                            System.out.println("!bidderOp.getHomecities().contains(previous.pickupCity");
                            bidderOp.addVehicleAt(previous.pickupCity);
                            System.out.println("finish add vehicle");
                            bidderOp.predictMC(previous);
                            opRatio = LOWER_RATIO;
                        }
                    }
                }
            }
        }

        if (winner == agent.id()) {
            bidderSelf.acceptTask(previous);
            bidderSelf.totalBid += bids[winner];
            acceptedTask++;
            selfRatio = Math.min(selfRatio + 0.05, UPPER_RATIO);
        }
        else {
            bidderOp.acceptTask(previous);
            bidderOp.totalBid += bids[winner];
            opRatio = Math.max(selfRatio - 0.05, LOWER_RATIO);
        }
        double acceptedPercentage = (double) acceptedTask / (double) round;

        if (acceptedPercentage > 0.55) {
            selfRatio = Math.min(selfRatio + 0.03, UPPER_RATIO);
        }
        else {
            selfRatio = Math.max(selfRatio - 0.03, LOWER_RATIO);
        }
    }


    private double estimateSelfMC(Task task) {
        double marginCost = bidderSelf.predictMC(task);
        double avgCost = bidderSelf.predictAvgCost();

        System.out.println("marginCost: " + marginCost);
        System.out.println("avgCost: " + avgCost);

        double alpha = Math.pow(Math.E, -round*0.1);
        double bidSelf = (marginCost * (1-alpha) + avgCost * alpha) * selfRatio;

        return bidSelf;
    }

    private double estimateOpMC(Task task, double bidSelf) {
        double marginCost = bidderOp.predictMC(task);
        double avgCost = bidderOp.predictAvgCost();
        //marginOp = marginCost;

        double alpha = Math.pow(Math.E, -round*0.1);
        double bidOp = (marginCost * (1-alpha) + avgCost * alpha) * opRatio;

        bidOp = Math.min(bidSelf, bidOp*1.2);

        double growth = GROWTH_COEF / (1+ Math.exp(-round * GROWTH_RATE));
        double periodic = SIN_COEF * (Math.abs(Math.sin(round * 2 * Math.PI / SIN_T))) + 1;
        bidOp *= growth;
        bidOp *= periodic;

        if (bidOp <= 10)
            bidOp = task.pathLength() * bidderOp.getMinCostPerKm() * 1;

        bidOp = Math.max(bidOp, 0.3 * (bidderOp.getNextCost() - bidderOp.totalBid));
        marginOp = bidOp;

        return bidOp;
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        HashMap<Integer, Task> taskMap = new HashMap<>();
        for (Task task : tasks) {
            taskMap.put(task.id, task);
        }
        List<Plan> plans = new ArrayList<>();
        Map<VehicleModel, List<PlanAction>> planActions = bidderSelf.getPlanAction();

        for (int i = 0; i < planActions.size(); i++) {
            VehicleModel v = VehicleModel.convertToModel(vehicles.get(i));
            List<PlanAction> actions = planActions.get(v);
            Plan plan = new Plan(v.homeCity());
            City current = v.homeCity();

            for (PlanAction action : actions) {
                if (action.isPickUp) {
                    // move: current city => pickup location
                    for (City city : current.pathTo(action.task.pickupCity)) {
                        plan.appendMove(city);
                    }
                    plan.appendPickup(taskMap.get(action.task.id));
                    current = action.task.pickupCity;
                } else {
                    // move: pickup location => delivery location
                    for (City city : current.pathTo(action.task.deliveryCity)) {
                        plan.appendMove(city);
                    }
                    plan.appendDelivery(taskMap.get(action.task.id));
                    current = action.task.deliveryCity;
                }
            }
            plans.add(plan);
        }
        return plans;
    }
}
