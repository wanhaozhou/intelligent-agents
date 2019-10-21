package template;

/* import table */
import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeTrained implements DeliberativeBehavior {

    enum Algorithm { BFS, ASTAR, BFS_FAST }

    /* Environment */
    Topology topology;
    TaskDistribution td;

    /* the properties of the agent */
    Agent agent;
    int capacity;

    /* the planning class */
    Algorithm algorithm;

    @Override
    public void setup(Topology topology, TaskDistribution td, Agent agent) {
        this.topology = topology;
        this.td = td;
        this.agent = agent;

        // initialize the planner
        int capacity = agent.vehicles().get(0).capacity();
        String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");

        // Throws IllegalArgumentException if algorithm is unknown
        algorithm = Algorithm.valueOf(algorithmName.toUpperCase());

        // ...
    }

    @Override
    public Plan plan(Vehicle vehicle, TaskSet tasks) {
        Plan plan;
        DeliberativeState currState = new DeliberativeState(vehicle, tasks);

        // Compute the plan with the selected algorithm.
        switch (algorithm) {
            case ASTAR:
                DeliberativeDispatcher astarDispatcher = new DeliberativeDispatcher(Algorithm.ASTAR);

                long startTime1 = System.nanoTime();

                plan = astarDispatcher.generatePlan(currState);

                long endTime1 = System.nanoTime();
                long timeElapsed1 = endTime1 - startTime1;

                System.out.println("ASTAR Execution time in milliseconds : " + timeElapsed1 / 1000000);
                System.out.println("total distance of astar plan:" + plan.totalDistance());

                break;

            case BFS:
                // ...
                DeliberativeDispatcher bfsDispatcher = new DeliberativeDispatcher(Algorithm.BFS);

                long startTime2 = System.nanoTime();

                plan = bfsDispatcher.generatePlan(currState);

                long endTime2 = System.nanoTime();
                long timeElapsed2 = endTime2 - startTime2;


                System.out.println("BFS Execution time in milliseconds : " +  timeElapsed2 / 1000000);
                System.out.println("total distance of bfs plan: " + plan.totalDistance());

                break;

            case BFS_FAST:
                // ...
                DeliberativeDispatcher bfsFastDispatcher = new DeliberativeDispatcher(Algorithm.BFS_FAST);

                long startTime3 = System.nanoTime();

                plan = bfsFastDispatcher.generatePlan(currState);

                long endTime3 = System.nanoTime();
                long timeElapsed3 = endTime3 - startTime3;


                System.out.println("BFS Fast Execution time in milliseconds : " +  timeElapsed3 / 1000000);
                System.out.println("total distance of bfs fast plan: " + plan.totalDistance());

                break;

            default:
                throw new AssertionError("Should not happen.");
        }
        return plan;
    }


    @Override
    public void planCancelled(TaskSet carriedTasks) {

        if (!carriedTasks.isEmpty()) {
            // This cannot happen for this simple agent, but typically
            // you will need to consider the carriedTasks when the next
            // plan is computed.
            // Will recompute the plan

        }
    }
}
