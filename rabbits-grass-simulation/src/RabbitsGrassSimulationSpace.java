/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */
import uchicago.src.sim.space.Object2DGrid;


public class RabbitsGrassSimulationSpace {
    private Object2DGrid grassSpace;
    private Object2DGrid agentSpace;

    public RabbitsGrassSimulationSpace(int gridSize) {
        grassSpace = new Object2DGrid(gridSize, gridSize);
        agentSpace = new Object2DGrid(gridSize, gridSize);

        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                grassSpace.putObjectAt(i, j, 0);
            }
        }
    }

    public void spreadEnergy(int energy) {
        // Randomly place money in moneySpace
        for(int i = 0; i < energy; i++){
            // Choose coordinates
            int x = (int) (Math.random() * (grassSpace.getSizeX()));
            int y = (int) (Math.random() * (grassSpace.getSizeY()));

            // Get the value of the object at those coordinates
            int curr = getEnergyAt(x, y);

            // Replace the Integer object with another one with the new value
            grassSpace.putObjectAt(x, y, curr + 1);
        }
    }

    private int getEnergyAt(int x, int y) {
        return grassSpace.getObjectAt(x,y) != null ? (Integer) grassSpace.getObjectAt(x, y) : 0;
    }

    public Object2DGrid getGrassSpace() {
        return grassSpace;
    }

    public Object2DGrid getAgentSpace() {
        return agentSpace;
    }

    public boolean isOccupied(int x, int y) {
        return agentSpace.getObjectAt(x, y) != null;
    }

    public boolean addAgent(RabbitsGrassSimulationAgent agent) {
        boolean ret = false;
        int count = 0;
        int countLimit = 10 * agentSpace.getSizeX() * agentSpace.getSizeY();
        while (!ret && count < countLimit) {
            int x = (int) (Math.random() * (agentSpace.getSizeX()));
            int y = (int) (Math.random() * (agentSpace.getSizeY()));
            if (!isOccupied(x,y)) {
                agentSpace.putObjectAt(x, y, agent);
                agent.setXY(x, y);
                ret = true;
            }
            count++;
        }
        return ret;
    }

    public void removeAgentAt(int x, int y) {
        agentSpace.putObjectAt(x, y, null);
    }
}
