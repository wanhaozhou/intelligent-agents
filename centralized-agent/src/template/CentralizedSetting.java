package template;

import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.HashMap;


public class CentralizedSetting {
    public static Topology topology;
    public static TaskDistribution distribution;
    public static Agent agent;
    public static TaskSet tasks;

    public static void init(Topology topology,
                            TaskDistribution distribution,
                            Agent agent,
                            TaskSet tasks) {

        CentralizedSetting.topology = topology;
        CentralizedSetting.distribution = distribution;
        CentralizedSetting.agent = agent;
        CentralizedSetting.tasks = tasks;
    }

}
