import java.awt.Color;
import java.util.*;

import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.engine.BasicAction;

import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.gui.Value2DDisplay;

import uchicago.src.sim.analysis.DataSource;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;

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
	private static final int NUMINITRABBITS = 10;
	private static final int NUMINITGRASS = 1000;
	private static final int GRASSGROWTHRATE = 1000;
	private static final double BIRTHTHRESHOLD = 50.0;
	private static final int MINENERGYLEVEL = 0;
	private static final int BIRTHENERGY = 30;

	private int gridSize = GRIDSIZE;
	private int numInitRabbits = NUMINITRABBITS;
	private int numInitGrass = NUMINITGRASS;
	private int grassGrowthRate = GRASSGROWTHRATE;
	private double birthThreshold = BIRTHTHRESHOLD;
	private int minEnergyLevel = MINENERGYLEVEL;

	//private int energy = numInitGrass;

	private Schedule schedule;
	private RabbitsGrassSimulationSpace rabbitsGrassSimulationSpace;
	private DisplaySurface displaySurface;
	private List<RabbitsGrassSimulationAgent> agentList;

	private OpenSequenceGraph statisticsOfSpace;

	class energyInSpace implements DataSource, Sequence {

		public Object execute() {
			return getSValue();
		}

		public double getSValue() {
			return Math.log10((double) rabbitsGrassSimulationSpace.getTotalEnergy());
		}
	}

	class agentsInSpace implements DataSource, Sequence {

		public Object execute() {
			return getSValue();
		}

		public double getSValue() {
			return Math.log10((double) countLivingAgents());
		}
	}

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

		if (statisticsOfSpace != null){
			statisticsOfSpace.dispose();
		}
		statisticsOfSpace = null;

		displaySurface = new DisplaySurface(this, "Rabbits Model Window 1");
		statisticsOfSpace = new OpenSequenceGraph("Amount Of Energy In Space",this);
		statisticsOfSpace.setXRange(0, 200);
		statisticsOfSpace.setYRange(0, 5);

		registerDisplaySurface("Rabbits Model Window 1", displaySurface);
		this.registerMediaProducer("Plot", statisticsOfSpace);
	}


	public void begin() {
		// TODO Auto-generated method stub
		buildModel();
		buildSchedule();
		buildDisplay();

		displaySurface.display();
		statisticsOfSpace.display();
	}

	public void buildModel() {
		System.out.println("buildModel");
		rabbitsGrassSimulationSpace = new RabbitsGrassSimulationSpace(gridSize);
		rabbitsGrassSimulationSpace.spreadEnergy(numInitGrass);

		for (int i = 0; i < numInitRabbits; i++) {
			addNewAgent();
		}

		for (int i = 0; i < agentList.size(); i++) {
			RabbitsGrassSimulationAgent rabbitsGrassSimulationAgent = agentList.get(i);
			rabbitsGrassSimulationAgent.report();
		}
	}

	public void buildSchedule() {
		System.out.println("buildSchedule");

		class RabbitStep extends BasicAction {
			public void execute() {
				SimUtilities.shuffle(agentList);
				for (int i = 0; i < agentList.size(); i++) {
					RabbitsGrassSimulationAgent rabbitsGrassSimulationAgent = agentList.get(i);
					rabbitsGrassSimulationAgent.step();
				}
				reapDeadAgents();
				bornNewAgents();
				// Updates to the display elements must be scheduled.
				displaySurface.updateDisplay();
			}
		}

		class RabbitCountLiving extends BasicAction {
			public void execute() {
				countLivingAgents();
			}
		}

		class RabbitUpdateEnergyInSpace extends BasicAction {
			public void execute() {
				statisticsOfSpace.step();
			}
		}

		class AddMoreEnergyInSpace extends BasicAction {
			public void execute() {
				rabbitsGrassSimulationSpace.spreadEnergy(grassGrowthRate);
			}
		}

		schedule.scheduleActionBeginning(0, new RabbitStep());
		schedule.scheduleActionAtInterval(1, new AddMoreEnergyInSpace());
		schedule.scheduleActionAtInterval(1, new RabbitCountLiving());
		schedule.scheduleActionAtInterval(1, new RabbitUpdateEnergyInSpace());
	}


	public void buildDisplay() {
		System.out.println("buildDisplay");
		ColorMap colorMap = new ColorMap();

		for (int i = 1; i < 160; i++) {
			colorMap.mapColor(i, Color.green);
		}
		colorMap.mapColor(0, Color.white);

		//colorMap.mapColor(0, Color.white);

		Value2DDisplay displayEnergy =
				new Value2DDisplay(rabbitsGrassSimulationSpace.getGrassSpace(), colorMap);

		Object2DDisplay displayAgents =
				new Object2DDisplay(rabbitsGrassSimulationSpace.getAgentSpace());
		displayAgents.setObjectList(agentList);

		displaySurface.addDisplayableProbeable(displayEnergy, "Energy");
		displaySurface.addDisplayableProbeable(displayAgents, "Agents");

		statisticsOfSpace.addSequence("Energy In Space", new energyInSpace());
		statisticsOfSpace.addSequence("Agents In Space", new agentsInSpace());

	}

	private void addNewAgent() {
		RabbitsGrassSimulationAgent rabbitsGrassSimulationAgent = new RabbitsGrassSimulationAgent(BIRTHENERGY);
		agentList.add(rabbitsGrassSimulationAgent);
		rabbitsGrassSimulationSpace.addAgent(rabbitsGrassSimulationAgent);
	}

	private int reapDeadAgents() {
		int count = 0;
		for (int i = agentList.size() - 1; i >= 0 ; i--) {
			RabbitsGrassSimulationAgent rabbitsGrassSimulationAgent = agentList.get(i);
			if (rabbitsGrassSimulationAgent.getEnergy() <= minEnergyLevel) {
				rabbitsGrassSimulationSpace.removeAgentAt(rabbitsGrassSimulationAgent.getX(),
															rabbitsGrassSimulationAgent.getY());
				agentList.remove(i);
				count++;
			}
		}
		return count;
	}

	private void bornNewAgents() {
		for (int i = agentList.size() - 1; i >= 0 ; i--) {
			RabbitsGrassSimulationAgent rabbitsGrassSimulationAgent = agentList.get(i);
			if (rabbitsGrassSimulationAgent.getEnergy() >= birthThreshold) {
				rabbitsGrassSimulationAgent.setEnergy(rabbitsGrassSimulationAgent.getEnergy() - BIRTHENERGY);
				addNewAgent();
			}
		}
	}

	private int countLivingAgents() {
		int livingAgents = 0;
		for (int i = 0; i < agentList.size(); i++) {
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

	public int getGrassGrowthRate() {
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

	public void setGrassGrowthRate(int grassGrowthRate) {
		this.grassGrowthRate = grassGrowthRate;
	}

	public void setBirthThreshold(double birthThreshold) {
		this.birthThreshold = birthThreshold;
	}


	public static void main(String[] args) {
		SimInit init = new SimInit();
		RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
		// Do "not" modify the following lines of parsing arguments
		if (args.length == 0) // by default, you don't use parameter file nor batch mode
			init.loadModel(model, "", false);
		else
			init.loadModel(model, args[0], Boolean.parseBoolean(args[1]));

	}
}
