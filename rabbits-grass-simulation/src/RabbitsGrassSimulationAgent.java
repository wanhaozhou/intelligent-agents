import java.awt.*;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;

/**
 * Class that implements the simulation agent for the rabbits grass simulation.
 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {

	private int x;
	private int y;
	private int vX;
	private int vY;
	private double energy;

	private static int IDNumber = 0;
	private int ID;

	private RabbitsGrassSimulationSpace rabbitsGrassSimulationSpace;

	public RabbitsGrassSimulationAgent(double energy) {
		this.energy = energy;
		x = -1;
		y = -1;
		setVXVY();
		ID = ++IDNumber;
	}

	private void setVXVY() {
		vX = 0;
		vY = 0;
		while ((vX == 0 && vY == 0) || (Math.abs(vX) == 1 && Math.abs(vY) == 1)) {
			vX = (int) Math.floor(Math.random() * 3) - 1;
			vY = (int) Math.floor(Math.random() * 3) - 1;
		}
	}

	public void draw(SimGraphics arg0) {
		// TODO Auto-generated method stub
		//if (energy > 5)
			//arg0.drawFastRoundRect(Color.green);
		//else
			arg0.drawFastRoundRect(Color.blue);
	}

	public int getX() {
		// TODO Auto-generated method stub
		return x;
	}

	public int getY() {
		// TODO Auto-generated method stub
		return y;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void setY(int y) {
		this.y = y;
	}

	public void setRabbitsGrassSimulationSpace(RabbitsGrassSimulationSpace rabbitsGrassSimulationSpace) {
		this.rabbitsGrassSimulationSpace = rabbitsGrassSimulationSpace;
	}

	public void setXY(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public void setEnergy(double energy) {
		this.energy = energy;
	}

	public String getID(){
		return "A-" + ID;
	}

	public double getEnergy() {
		return energy;
	}

	public void report() {
		System.out.println(getID() +
				" at " +
				x + ", " + y +
				" has " +
				getEnergy() + " energy.");
	}

	public void step() {
		int newX = x + vX;
		int newY = y + vY;

		Object2DGrid grid = rabbitsGrassSimulationSpace.getAgentSpace();
		newX = (newX + grid.getSizeX()) % grid.getSizeX();
		newY = (newY + grid.getSizeY()) % grid.getSizeY();

		//TODO check
		if (tryMove(newX, newY)) {
			energy += rabbitsGrassSimulationSpace.takeEnergyAt(x, y);
		}
		setVXVY();
		//energy += rabbitsGrassSimulationSpace.takeEnergyAt(x, y);
		energy -= 0.5;
	}

	private boolean tryMove(int newX, int newY){
		return rabbitsGrassSimulationSpace.moveAgentAt(x, y, newX, newY);
	}

}
