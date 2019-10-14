package template;

import java.util.ArrayList;

import logist.topology.Topology.City;

public class ReactiveState {
    private City currentCity;
    private City packageDest;

    private ArrayList<City> actions;

    public ReactiveState(City currentCity, City packageDest) {
        this.currentCity = currentCity;
        this.packageDest = packageDest;
        this.actions = new ArrayList<>();
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public String toString() {
        return "(" + this.currentCity + "," + this.packageDest + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        ReactiveState s = (ReactiveState) o;

        if (this.getCurrentCity().equals(s.getCurrentCity())) {
            if (s.getPackageDest() == null && this.getPackageDest() == null) {
                return true;
            }
            if (s.getPackageDest() == null || this.getPackageDest() == null) {
                return false;
            }
            return s.getPackageDest().equals(this.getPackageDest());
        }
        return false;
    }

    public City getCurrentCity() {
        return currentCity;
    }


    public City getPackageDest() {
        return packageDest;
    }

    public ArrayList<City> getActions() {
        return actions;
    }
}
