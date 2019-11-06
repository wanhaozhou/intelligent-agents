package template;

//the list of imports

import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.CentralizedBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 *
 */
@SuppressWarnings("unused")
public class CentralizedTrained implements CentralizedBehavior {

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private long timeout_setup;
    private long timeout_plan;
    private CentralizedSolver centralizedSolver;

    @Override
    public void setup(Topology topology, TaskDistribution distribution,
            Agent agent) {
        
        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_default.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
        
        // the setup method cannot last more than timeout_setup milliseconds
        timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
        
        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;

    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long time_start = System.currentTimeMillis();

        CentralizedSetting.init(topology, distribution, agent, tasks);
        centralizedSolver = new CentralizedSolver();

        Double prob = 0.3;

        centralizedSolver.SLS(50000, prob);

        List<Plan> plans = centralizedSolver.generatePlan();
        
        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("The plan was generated in " + duration + " milliseconds.");

        double cost = 0;
        for (int i = 0; i < plans.size(); i++) {
            cost += plans.get(i).totalDistance() * vehicles.get(i).costPerKm();
        }
        System.out.println("cost: " + cost);


//        String filename = Integer.toString((int)(prob * 10))+ ".txt";
//        BufferedWriter out = null;
//        try {
//            out = new BufferedWriter(new FileWriter(filename));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        for (Double numero : centralizedSolver.costRecords) {
//            try {
//                out.write(Double.toString(numero) + "\n");
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        try {
//            out.flush();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        try {
//            out.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        return plans;
    }

}
