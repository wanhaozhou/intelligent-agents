package template;

import java.util.ArrayList;
import java.util.Collections;
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


public class ReactiveControl {

    private HashMap<ReactiveState, Double> VTable;
    private HashMap<ReactiveState, HashMap<City, Double>> QTable;
    private HashMap<ReactiveState, City> policy;

    private ArrayList<City> cities;
    private Vehicle vehicle;
    private Topology topology;
    private double gamma;
    private TaskDistribution taskDistribution;
    private ArrayList<ReactiveState> states;
    private double epsilon;

    public HashMap<ReactiveState, City> getPolicy() {
        return policy;
    }

    public ReactiveControl(Vehicle vehicle, Topology topology, Double gamma,
                           TaskDistribution taskDistribution, ArrayList<ReactiveState> states, double epsilon) {

        this.vehicle = vehicle;
        this.topology = topology;
        this.gamma = gamma;
        this.epsilon = epsilon;
        this.taskDistribution = taskDistribution;
        this.states = states;
        this.cities = new ArrayList<>(this.topology.cities());
        this.VTable = new HashMap<>();
        this.QTable = new HashMap<>();
        this.policy = new HashMap<>();

        // train
        train();
    }

    private void train() {
        for (ReactiveState s : states) {
            // init the v table
            VTable.put(s, 0.0);

            // init the q table
            HashMap<City, Double> actionValueTable = QTable.getOrDefault(s, new HashMap<>());
            for (City action : s.getActions()) {
                actionValueTable.put(action, 0.0);
            }
            QTable.put(s, actionValueTable);

            // init the policy table
            policy.put(s, topology.randomCity(new Random()));
        }

        /**
         *  Reinforcement Learning Algorithm
         *
         *
         *	repeat
         *		for s ∈ S do
         *			for a ∈ A do
         *				Q(s,a) ← R(s,a) + γ × Σ(s'∈S) T(s,a,s') × V(s')
         *			end for
         *			V (S) ← max(a) Q(s,a)
         *		end for
         *	until good enough
         *
         *
         *  With our model, action a is the chosen next city to go,
         *  and T(s,a,s') = p(a,s'), and R(s,a) = I × r(s,a) − cost(s,a),
         *  with I = 1 if the task is accepted, 0 otherwise.
         *
         */

        double diff = Double.POSITIVE_INFINITY;

        while (diff > epsilon) {
            diff = 0;
            for (ReactiveState s : states) {
                for (City a : s.getActions()) {
                    double sum = 0.0;
                    // the prob that next state have task
                    double taskProb = 0.0;

                    for (City sPrime : cities) {
                        if (!a.equals(sPrime)) {
                            sum += taskDistribution.probability(a, sPrime) * VTable.get(new ReactiveState(a, sPrime));
                            taskProb += taskDistribution.probability(a, sPrime);
                        }
                    }

                    sum += (1 - taskProb) * VTable.get(new ReactiveState(a, null));
                    double reward = - s.getCurrentCity().distanceTo(a) * vehicle.costPerKm();

                    if (s.getPackageDest() != null && s.getPackageDest().equals(a)) {
                        reward += taskDistribution.reward(s.getCurrentCity(), a);
                    }

                    QTable.get(s).put(a, sum * gamma + reward);
                }

                double maxVal = Collections.max(QTable.get(s).values());
                diff += Math.abs(VTable.get(s) - maxVal);
                VTable.put(s, maxVal);

                HashMap<City, Double> actionValueTable = QTable.get(s);
                for (City action : actionValueTable.keySet()) {
                    if (actionValueTable.get(action) == maxVal) {
                        policy.put(s, action);
                        break;
                    }
                }
            }
        }
    }
}
