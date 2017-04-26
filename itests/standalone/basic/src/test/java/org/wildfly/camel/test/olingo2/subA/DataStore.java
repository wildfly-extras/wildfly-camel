/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2016 RedHat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wildfly.camel.test.olingo2.subA;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class DataStore {

    //Data accessors
    public Map<String, Object> getCar(int id) {
        Map<String, Object> data = null;

        Calendar updated = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

        switch (id) {
            case 1:
                updated.set(2012, 11, 11, 11, 11, 11);
                data = createCar(1, "F1 W03", 1, 189189.43, "EUR", "2012", updated, "file://imagePath/w03");
                break;

            case 2:
                updated.set(2013, 11, 11, 11, 11, 11);
                data = createCar(2, "F1 W04", 1, 199999.99, "EUR", "2013", updated, "file://imagePath/w04");
                break;

            case 3:
                updated.set(2012, 12, 12, 12, 12, 12);
                data = createCar(3, "F2012", 2, 137285.33, "EUR", "2012", updated, "http://pathToImage/f2012");
                break;

            case 4:
                updated.set(2013, 12, 12, 12, 12, 12);
                data = createCar(4, "F2013", 2, 145285.00, "EUR", "2013", updated, "http://pathToImage/f2013");
                break;

            case 5:
                updated.set(2011, 11, 11, 11, 11, 11);
                data = createCar(5, "F1 W02", 1, 167189.00, "EUR", "2011", updated, "file://imagePath/wXX");
                break;

            default:
                break;
        }

        return data;
    }


    private Map<String, Object> createCar(int carId, String model, int manufacturerId, double price, String currency, String modelYear, Calendar updated, String imagePath) {
        Map<String, Object> data = new HashMap<>();

        data.put("Id", carId);
        data.put("Model", model);
        data.put("ManufacturerId", manufacturerId);
        data.put("Price", price);
        data.put("Currency", currency);
        data.put("ModelYear", modelYear);
        data.put("Updated", updated);
        data.put("ImagePath", imagePath);

        return data;
    }

    public Map<String, Object> getManufacturer(int id) {
        Map<String, Object> data = null;
        Calendar date = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

        switch (id) {
            case 1:
                Map<String, Object> addressStar = createAddress("Star Street 137", "Stuttgart", "70173", "Germany");
                date.set(1954, 7, 4);
                data = createManufacturer(1, "Star Powered Racing", addressStar, date);
                break;

            case 2:
                Map<String, Object> addressHorse = createAddress("Horse Street 1", "Maranello", "41053", "Italy");
                date.set(1929, 11, 16);
                data = createManufacturer(2, "Horse Powered Racing", addressHorse, date);
                break;

            default:
                break;
        }

        return data;
    }

    private Map<String, Object> createManufacturer(int id, String name, Map<String, Object> address, Calendar updated) {
        Map<String, Object> data = new HashMap<>();
        data.put("Id", id);
        data.put("Name", name);
        data.put("Address", address);
        data.put("Updated", updated);
        return data;
    }

    private Map<String, Object> createAddress(String street, String city, String zipCode, String country) {
        Map<String, Object> address = new HashMap<>();
        address.put("Street", street);
        address.put("City", city);
        address.put("ZipCode", zipCode);
        address.put("Country", country);
        return address;
    }


    public List<Map<String, Object>> getCars() {
        List<Map<String, Object>> cars = new ArrayList<>();
        cars.add(getCar(1));
        cars.add(getCar(2));
        cars.add(getCar(3));
        cars.add(getCar(4));
        cars.add(getCar(5));
        return cars;
    }

    public List<Map<String, Object>> getManufacturers() {
        List<Map<String, Object>> manufacturers = new ArrayList<>();
        manufacturers.add(getManufacturer(1));
        manufacturers.add(getManufacturer(2));
        return manufacturers;
    }


    public List<Map<String, Object>> getCarsFor(int manufacturerId) {
        List<Map<String, Object>> cars = getCars();
        List<Map<String, Object>> carsForManufacturer = new ArrayList<>();

        for (Map<String, Object> car : cars) {
            if (Integer.valueOf(manufacturerId).equals(car.get("ManufacturerId"))) {
                carsForManufacturer.add(car);
            }
        }

        return carsForManufacturer;
    }

    public Map<String, Object> getManufacturerFor(int carId) {
        Map<String, Object> car = getCar(carId);
        if (car != null) {
            Object manufacturerId = car.get("ManufacturerId");
            if (manufacturerId != null) {
                return getManufacturer((Integer) manufacturerId);
            }
        }
        return null;
    }
}
