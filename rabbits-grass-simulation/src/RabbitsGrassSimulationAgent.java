import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;

import java.awt.*;


/**
 * Class that implements the simulation agent for the rabbits grass simulation.
 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {

	private int x;
	private int y;
	private double energy;

	private static int IDNumber = 0;
	private int ID;

	public RabbitsGrassSimulationAgent(double energy) {
		this.energy = energy;
		x = -1;
		y = -1;
		ID = ++IDNumber;
	}

	public void draw(SimGraphics arg0) {
		// TODO Auto-generated method stub
		if (energy > 5)
			arg0.drawFastRoundRect(Color.green);
		else
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

	public void setXY(int x, int y) {
		this.x = x;
		this.y = y;
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
		energy -= 0.5;
	}

}
