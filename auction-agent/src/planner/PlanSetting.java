package planner;

import bidder.CityEstimator;
import logist.agent.Agent;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class PlanSetting {
    public static Topology topology;
    public static TaskDistribution distribution;
    public static double[][] cityToCity;

    public static void init(Topology topology,
                            TaskDistribution distribution) {

        PlanSetting.topology = topology;
        PlanSetting.distribution = distribution;

        List<City> cities = PlanSetting.topology.cities();

        cityToCity = new double[cities.size()][cities.size()];

        for (int i=0; i<cities.size();i++) {
            for (int j=0; j<cities.size();j++) {
                cityToCity[i][j] = distribution.probability(cities.get(i), cities.get(j));
            }
        }
    }

    public static List<Task> generateRandomTask(int startId, int size) {
        List<Task> res = new ArrayList<>();

        for (int i=0;i<size;i++) {
            int cityInd = (int) Math.random() * topology.cities().size();
            City randomPickUpCity = topology.cities().get(cityInd);
            double[] prob = cityToCity[cityInd];
            double citySum = Arrays.stream(prob).sum();
            double rdm = Math.random() * citySum;
            double acc = 0.0;
            int deliverInd = 0;
            for (int j = 0; j < prob.length; j++) {
                if (rdm > acc && rdm <= acc + prob[j]) {
                    deliverInd = j;
                    break;
                }
                acc += prob[j];
            }

            City randomDeliverCity = topology.cities().get(deliverInd);
            res.add(new Task(startId, randomPickUpCity, randomDeliverCity,
                    0, distribution.weight(randomPickUpCity, randomDeliverCity)));
            startId++;
        }

        return res;
    }

}
