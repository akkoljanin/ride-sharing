package api.dtos;

public class DriverDto {
    private String email;
    private String name;
    private String vehiclePlate;
    private String vehicleModel;
    private boolean available;
    private double locationLat;
    private double locationLng;

    public DriverDto() {}
    public DriverDto(String email, String name, String vehiclePlate, String vehicleModel,
                     boolean available, double locationLat, double locationLng) {
        this.email = email; this.name = name; this.vehiclePlate = vehiclePlate;
        this.vehicleModel = vehicleModel; this.available = available;
        this.locationLat = locationLat; this.locationLng = locationLng;
    }
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
