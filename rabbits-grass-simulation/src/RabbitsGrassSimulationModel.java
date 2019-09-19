import java.awt.Color;
import java.util.*;

import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.util.SimUtilities;


/**
 *
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author 
 */


public class RabbitsGrassSimulationModel extends SimModelImpl {

	private static final int GRIDSIZE = 20;
	private static final int NUMINITRABBITS = 50;
	private static final int NUMINITGRASS = 50;
	private static final double GRASSGROWTHRATE = 0.1;
	private static final double BIRTHTHRESHOLD = 10.0;
	private static final int TOTALENERGY = 1000;
	private static final int MINENERGYLEVEL = 0;

	private int gridSize = GRIDSIZE;
	private int numInitRabbits = NUMINITRABBITS;
	private int numInitGrass = NUMINITGRASS;
	private double grassGrowthRate = GRASSGROWTHRATE;
	private double birthThreshold = BIRTHTHRESHOLD;
	private int minEnergyLevel = MINENERGYLEVEL;
	// public?
	private int energy = TOTALENERGY;

	private Schedule schedule;
	private RabbitsGrassSimulationSpace rabbitsGrassSimulationSpace;
	private DisplaySurface displaySurface;
	private List<RabbitsGrassSimulationAgent> agentList;


	/**
	 * called when the button with the two curved arrows is pressed.
	 *
	 * DisplaySurface objects are basically windows.
	 * A ColorMap object is a linking of particular values to particular colors- we will
	 * use it to say that a '0' is black, a '1' is dark red,
	 * a '2' is lighter red, a '3' is even lighter,
	 * and so forth to indicate how much money is in a given cell.
	 *
	 * Value2DDisplay is an object that links a map to someplace
	 * in your model that provides values to display, and that when added to a DisplaySurface
	 * links the window, the function, and the color map together.
	 *
	 * To use these objects we must:
	 *
	 * Create a variable of type DisplaySurface to hold it
	 * Tear the display surface down in the 'set up' method- this is done by executing the 'dispose' method (but only if the object has been instantiated, so a check to see if the object != null is appropriate), and then setting the object to null
	 * Instantiate a new display surface; to do this you must pass two arguments:
	 * The model object for which this display surface will be serving
	 * The name of the display surface to be created (which will appear in the window's title bar)
	 * Register the display surface- tell RePast that it exists and what its name is
	 * Create a color map and specify its values
	 * Create a Value2DDisplay object that links the set of values we want with the color map
	 * Add the Value2DDisplay object to the DisplaySurface object
	 */
	public void setup() {
		// TODO Auto-generated method stub
		System.out.println("setup");
		rabbitsGrassSimulationSpace = null;
		agentList = new ArrayList<>();
		schedule = new Schedule(1);

		if (displaySurface != null) {
			displaySurface.dispose();
		}
		displaySurface = null;
		displaySurface = new DisplaySurface(this, "Rabbits Model Window 1");
		registerDisplaySurface("Rabbits Model Window 1", displaySurface);
	}


	public void begin() {
		// TODO Auto-generated method stub
		buildModel();
		buildSchedule();
		buildDisplay();

		displaySurface.display();
	}

	public void buildModel(){
		System.out.println("buildModel");
		rabbitsGrassSimulationSpace = new RabbitsGrassSimulationSpace(gridSize);
		rabbitsGrassSimulationSpace.spreadEnergy(energy);

		for (int i = 0; i < numInitRabbits; i++) {
			addNewAgent();
		}

		for (int i = 0; i < agentList.size(); i++) {
			RabbitsGrassSimulationAgent rabbitsGrassSimulationAgent = agentList.get(i);
			rabbitsGrassSimulationAgent.report();
		}

	}

	public void buildSchedule(){
		System.out.println("buildSchedule");

		class RabbitStep extends BasicAction {
			public void execute() {
				SimUtilities.shuffle(agentList);
				for(int i =0; i < agentList.size(); i++){
					RabbitsGrassSimulationAgent rabbitsGrassSimulationAgent = agentList.get(i);
					rabbitsGrassSimulationAgent.step();
				}
				// Updates to the display elements must be scheduled.
				displaySurface.updateDisplay();
			}
		}

		class RabbitCountLiving extends BasicAction {
			public void execute(){
				countLivingAgents();
			}
		}

		schedule.scheduleActionBeginning(0, new RabbitStep());
		schedule.scheduleActionAtInterval(10, new RabbitCountLiving());
	}


	public void buildDisplay(){
		System.out.println("buildDisplay");
		ColorMap colorMap = new ColorMap();

		// TODO: change the color map
		for(int i = 1; i < 16; i++){
			colorMap.mapColor(i, new Color((int) (i * 8 + 127), 0, 0));
		}
		colorMap.mapColor(0, Color.white);

		Value2DDisplay displayEnergy =
				new Value2DDisplay(rabbitsGrassSimulationSpace.getGrassSpace(), colorMap);

		Object2DDisplay displayAgents =
				new Object2DDisplay(rabbitsGrassSimulationSpace.getAgentSpace());
		displayAgents.setObjectList(agentList);

		displaySurface.addDisplayable(displayEnergy, "Energy");
		displaySurface.addDisplayable(displayAgents, "Agents");
	}

	private void addNewAgent() {
		RabbitsGrassSimulationAgent rabbitsGrassSimulationAgent = new RabbitsGrassSimulationAgent(5.0);
		agentList.add(rabbitsGrassSimulationAgent);
		rabbitsGrassSimulationSpace.addAgent(rabbitsGrassSimulationAgent);
	}

	private int reapDeadAgents(){
		int count = 0;
		for (int i = agentList.size() - 1; i >= 0 ; i--) {
			RabbitsGrassSimulationAgent rabbitsGrassSimulationAgent = agentList.get(i);
			if (rabbitsGrassSimulationAgent.getEnergy() <= 0) {
				rabbitsGrassSimulationSpace.removeAgentAt(rabbitsGrassSimulationAgent.getX(),
															rabbitsGrassSimulationAgent.getY());
				agentList.remove(i);
				count++;
			}
		}

		return count;
	}

	private int countLivingAgents() {
		int livingAgents = 0;
		for(int i = 0; i < agentList.size(); i++){
			RabbitsGrassSimulationAgent rabbitsGrassSimulationAgent = agentList.get(i);
			if (rabbitsGrassSimulationAgent.getEnergy() > 0) livingAgents++;
		}
		System.out.println("Number of living agents is: " + livingAgents);
		return livingAgents;
	}

	public String[] getInitParam() {
		// TODO Auto-generated method stub
		// Parameters to be set by users via the Repast UI slider bar
		// Do "not" modify the parameters names provided in the skeleton code, you can add more if you want
		String[] params = { "GridSize", "NumInitRabbits",
							"NumInitGrass", "GrassGrowthRate",
							"BirthThreshold"};
		return params;
	}

	public String getName() {
		// TODO Auto-generated method stub
		return "rabbits simulation";
	}

	public Schedule getSchedule() {
		return schedule;
	}

	public int getGridSize() {
		return gridSize;
	}

	public int getNumInitRabbits() {
		return numInitRabbits;
	}

	public int getNumInitGrass() {
		return numInitGrass;
	}

	public double getGrassGrowthRate() {
		return grassGrowthRate;
	}

	public double getBirthThreshold() {
		return birthThreshold;
	}

	public void setSchedule(Schedule schedule) {
		this.schedule = schedule;
	}

	public void setGridSize(int gridSize) {
		this.gridSize = gridSize;
	}

	public void setNumInitRabbits(int numInitRabbits) {
		this.numInitRabbits = numInitRabbits;
	}

	public void setNumInitGrass(int numInitGrass) {
		this.numInitGrass = numInitGrass;
	}

	public void setGrassGrowthRate(double grassGrowthRate) {
		this.grassGrowthRate = grassGrowthRate;
	}

	public void setBirthThreshold(double birthThreshold) {
		this.birthThreshold = birthThreshold;
	}


	public static void main(String[] args) {

		//System.out.println("Rabbit skeleton");

		SimInit init = new SimInit();
		RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
		// Do "not" modify the following lines of parsing arguments
		if (args.length == 0) // by default, you don't use parameter file nor batch mode
			init.loadModel(model, "", false);
		else
			init.loadModel(model, args[0], Boolean.parseBoolean(args[1]));

	}
}
