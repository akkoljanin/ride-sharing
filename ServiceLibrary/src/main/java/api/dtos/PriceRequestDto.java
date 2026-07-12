package api.dtos;

public class PriceRequestDto {
    private double pickupLat;
    private double pickupLng;
    private double dropoffLat;
    private double dropoffLng;
    private int activeDrivers;
    private int activeRequests;

    public PriceRequestDto() {}
    public PriceRequestDto(double pickupLat, double pickupLng, double dropoffLat, double dropoffLng,
                           int activeDrivers, int activeRequests) {
        this.pickupLat = pickupLat; this.pickupLng = pickupLng;
        this.dropoffLat = dropoffLat; this.dropoffLng = dropoffLng;
        this.activeDrivers = activeDrivers; this.activeRequests = activeRequests;
    }
    public double getPickupLat() { return pickupLat; }
    public void setPickupLat(double pickupLat) { this.pickupLat = pickupLat; }
    public double getPickupLng() { return pickupLng; }
    public void setPickupLng(double pickupLng) { this.pickupLng = pickupLng; }
    public double getDropoffLat() { return dropoffLat; }
    public void setDropoffLat(double dropoffLat) { this.dropoffLat = dropoffLat; }
    public double getDropoffLng() { return dropoffLng; }
    public void setDropoffLng(double dropoffLng) { this.dropoffLng = dropoffLng; }
    public int getActiveDrivers() { return activeDrivers; }
    public void setActiveDrivers(int activeDrivers) { this.activeDrivers = activeDrivers; }
    public int getActiveRequests() { return activeRequests; }
    public void setActiveRequests(int activeRequests) { this.activeRequests = activeRequests; }
}
