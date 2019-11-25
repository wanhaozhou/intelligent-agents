package planner;

import helpers.VehicleModel;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlanState {
    HashMap<VehicleModel, List<PlanAction>> A;
    Double cost;

    public PlanState() {
        A = new HashMap<>();
        cost = 0.0;
    }

    public PlanState(HashMap<VehicleModel, List<PlanAction>> a,
                     Double cost) {
        A = a;
        this.cost = cost;
    }

    public PlanState copyState(PlanState oldState) {
        HashMap<VehicleModel, List<PlanAction>> newA = new HashMap<>();
        for (VehicleModel vehicle : oldState.A.keySet()) {
            List<PlanAction> newActions = new ArrayList<>(oldState.A.get(vehicle));
            newA.put(vehicle, newActions);
        }
        Double newCost = oldState.cost;
        return new PlanState(newA, newCost);
    }

    public PlanState copyState() {
        return copyState(this);
    }

    public void updateCost() {
        cost = 0.0;
        for (VehicleModel vehicle : A.keySet()) {
            if (A.get(vehicle).size() != 0) {
                City current = vehicle.homeCity();
                for (int i = 0; i < A.get(vehicle).size(); i++) {
                    if (A.get(vehicle).get(i).isPickUp) {
                        cost += (current.distanceTo(A.get(vehicle).get(i).task.pickupCity)) * vehicle.costPerKm();
                        current = A.get(vehicle).get(i).task.pickupCity;
                    } else {
                        cost += (current.distanceTo(A.get(vehicle).get(i).task.deliveryCity)) * vehicle.costPerKm();
                        current = A.get(vehicle).get(i).task.deliveryCity;
                    }
                }
            }
        }
    }

    /**
     * task : [pickupInd, deliverInd]
     * @param vehicle
     * @return
     */
    public Map<Task, List<Integer>> getTaskMapForVehicle(VehicleModel vehicle) {
        Map<Task, List<Integer>> res = new HashMap<>();
        List<PlanAction> actions = A.get(vehicle);
        for (int i = 0; i < actions.size(); i++) {
            if (!res.containsKey(actions.get(i).task)) {
                // add the pickup action index
                res.put(actions.get(i).task, new ArrayList<>());
                res.get(actions.get(i).task).add(i);
            } else {
                res.get(actions.get(i).task).add(i);
            }
        }
        return res;
    }

}
