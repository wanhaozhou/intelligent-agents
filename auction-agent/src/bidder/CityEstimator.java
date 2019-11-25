package bidder;

import logist.task.Task;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.List;

public class CityEstimator {

    private List<City> cities;
    private List<City> distantCities;
    private List<City> nearCities;

    public CityEstimator(List<City> cities) {
        this.cities = cities;
        this.distantCities = new ArrayList<>();
        this.nearCities = new ArrayList<>();
    }

    public City estimateCity(Long bid, Task prevTask, int costPerKM) {
        //bids[i], previous, bidderSelf.getAvgCostPerKM()
        this.distantCities = new ArrayList<>();
        this.nearCities = new ArrayList<>();

        if (bid == null) return null;

        double diff = Double.MAX_VALUE;
        City ret = null;
        for (City city : cities) {
            double currCost = (city.distanceTo(prevTask.pickupCity) + prevTask.pathLength()) * costPerKM;
            if (currCost >= bid) {
                distantCities.add(city);
            } else {
                nearCities.add(city);
            }
            double currDiff = Math.abs(currCost - bid);
            if (currDiff < diff) {
                diff = currDiff;
                ret = city;
            }
        }
        return ret;
    }

    public List<City> getDistantCities() {
        return distantCities;
    }

    public List<City> getNearCities() {
        return nearCities;
    }
}
