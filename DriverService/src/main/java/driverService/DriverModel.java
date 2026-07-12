package driverService;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "drivers")
public class DriverModel implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String vehiclePlate;

    @Column(nullable = false)
    private String vehicleModel;

    @Column(nullable = false)
    private boolean available = true;

    @Column
    private double locationLat;

    @Column
    private double locationLng;

    public DriverModel() {}

    public DriverModel(String email, String name, String vehiclePlate,
                       String vehicleModel, boolean available,
                       double locationLat, double locationLng) {
        this.email = email;
        this.name = name;
        this.vehiclePlate = vehiclePlate;
        this.vehicleModel = vehicleModel;
        this.available = available;
        this.locationLat = locationLat;
        this.locationLng = locationLng;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVehiclePlate() { return vehiclePlate; }
    public void setVehiclePlate(String vehiclePlate) { this.vehiclePlate = vehiclePlate; }
    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public double getLocationLat() { return locationLat; }
    public void setLocationLat(double locationLat) { this.locationLat = locationLat; }
    public double getLocationLng() { return locationLng; }
    public void setLocationLng(double locationLng) { this.locationLng = locationLng; }
}
