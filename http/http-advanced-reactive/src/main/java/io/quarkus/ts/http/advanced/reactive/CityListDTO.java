package io.quarkus.ts.http.advanced.reactive;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CityListDTO {

    @XmlElement(name = "city")
    private List<City> cities;

    public CityListDTO() {
      this.cities = new ArrayList<>();
    }

    public CityListDTO(List<City> cities) {
        this.cities = cities;
    }

    public List<City> getCityList() {
        return cities;
    }

    public void setCities(List<City> cities) {
        this.cities = cities;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("CityListDTO{cities=[");
      for (City city : cities) {
        sb.append(city.toString()).append(", ");
      }
      if (!cities.isEmpty()) {
        sb.setLength(sb.length() - 2);
      }
      sb.append("]}");
      return sb.toString();
    }
}
