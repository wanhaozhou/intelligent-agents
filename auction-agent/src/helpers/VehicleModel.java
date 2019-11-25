package helpers;

import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.List;

public class VehicleModel {
    private int id;
    private int capacity;
    private int costPerKm;
    private City homeCity;

    public static List<VehicleModel> convertToModels(List<Vehicle> vehicles) {
        List<VehicleModel> res = new ArrayList<>();

        for (Vehicle v: vehicles) {
            res.add(new VehicleModel(v));
        }

        return res;
    }

    public static VehicleModel convertToModel(Vehicle v) {
        return new VehicleModel(v);
    }

    public static List<VehicleModel> agentToVehicles(Agent agent) {
        return convertToModels(agent.vehicles());
    }

    public VehicleModel(int id, int capacity, int costPerKm, City homeCity) {
        this.id = id;
        this.capacity = capacity;
        this.costPerKm = costPerKm;
        this.homeCity = homeCity;
    }

    public VehicleModel(Vehicle v) {
        this(v.id(),v.capacity(), v.costPerKm(), v.homeCity());
    }

    // Getter
    public int id() {
        return id;
    }

    public int capacity() {
        return capacity;
    }

    public int costPerKm() {
        return costPerKm;
    }

    public City homeCity() {
        return homeCity;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        VehicleModel vm = (VehicleModel) obj;
        if (this.id != vm.id()) return false;
        if (this.capacity != vm.capacity()) return false;
        if (this.costPerKm != vm.costPerKm()) return false;
        if (!this.homeCity.equals(vm.homeCity())) return false;
        return true;
    }

    @Override
    public String toString() {
        return this.id + this.capacity + this.costPerKm + homeCity.toString();
    }
}
