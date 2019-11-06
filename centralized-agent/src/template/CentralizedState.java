package template;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CentralizedState {
    HashMap<Vehicle, List<CentralizedAction>> A;
    Double cost;

    public CentralizedState() {
        A = new HashMap<>();
        cost = 0.0;
    }

    public CentralizedState(HashMap<Vehicle, List<CentralizedAction>> a,
                            Double cost) {
        A = a;
        this.cost = cost;
    }

    public CentralizedState copyState(CentralizedState oldState) {
        HashMap<Vehicle, List<CentralizedAction>> newA = new HashMap<>();
        for (Vehicle vehicle : oldState.A.keySet()) {
            List<CentralizedAction> newActions = new ArrayList<>(oldState.A.get(vehicle));
            newA.put(vehicle, newActions);
        }
        Double newCost = oldState.cost;
        return new CentralizedState(newA, newCost);
    }

    public CentralizedState copyState() {
        return copyState(this);
    }

    public void updateCost() {
        cost = 0.0;
        for (Vehicle vehicle : A.keySet()) {
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
    public Map<Task, List<Integer>> getTaskMapForVehicle(Vehicle vehicle) {
        Map<Task, List<Integer>> res = new HashMap<>();
        List<CentralizedAction> actions = A.get(vehicle);
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
