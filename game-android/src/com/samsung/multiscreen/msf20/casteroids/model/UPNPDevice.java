package com.samsung.multiscreen.msf20.casteroids.model;

/**
 * Contains data on a discovered UPNP device.
 *
 * Created by Ali Khan on 3/24/15.
 */
public class UPNPDevice {

    // The manufacturer of the device
    private String manufacturer;

    // The model name of the device
    private String modelName;

    // The model number of the device
    private String modelNumber;

    // The serial number of the device
    private String serialNumber;

    // The type of device
    private String deviceType;

    // The friendly device name
    private String friendlyName;

    /**
     * Returns the manufacturer of the device
     *
     * @return The manufacturer of the device
     */
    public String getManufacturer() {
        return manufacturer;
    }

    /**
     * Sets the manufacturer of the device
     *
     * @param manufacturer The manufacturer of the device
     */
    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    /**
     * Returns the model name of the device
     *
     * @return The model name of the device
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * Sets the model name of the device
     *
     * @param modelName The model name of the device
     */
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    /**
     * Returns the model number of the device
     *
     * @return The model number of the device
     */
    public String getModelNumber() {
        return modelNumber;
    }

    /**
     * Sets the model number of the device
     *
     * @param modelNumber The model number of the device
     */
    public void setModelNumber(String modelNumber) {
        this.modelNumber = modelNumber;
    }

    /**
     * Returns the serial number of the device
     *
     * @return The serial number of the device
     */
    public String getSerialNumber() {
        return serialNumber;
    }

    /**
     * Sets the serial number of the device
     *
     * @param serialNumber The serial number of the device
     */
    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    /**
     * Returns the type of device
     *
     * @return The type of device
     */
    public String getDeviceType() {
        return deviceType;
    }

    /**
     * Sets the type of device
     *
     * @param deviceType The type of device
     */
    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    /**
     * Returns the friendly name of the device
     *
     * @return The friendly name of the device
     */
    public String getFriendlyName() {
        return friendlyName;
    }

    /**
     * Sets the friendly name of the device
     *
     * @param friendlyName The friendly name of the device
     */
    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    @Override
    public String toString() {
        return "UPNPDevice{" +
                "manufacturer='" + manufacturer + '\'' +
                ", modelName='" + modelName + '\'' +
                ", modelNumber='" + modelNumber + '\'' +
                ", serialNumber='" + serialNumber + '\'' +
                ", deviceType='" + deviceType + '\'' +
                ", friendlyName='" + friendlyName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UPNPDevice that = (UPNPDevice) o;

        if (deviceType != null ? !deviceType.equals(that.deviceType) : that.deviceType != null) return false;
        if (manufacturer != null ? !manufacturer.equals(that.manufacturer) : that.manufacturer != null) return false;
        if (modelName != null ? !modelName.equals(that.modelName) : that.modelName != null) return false;
        if (modelNumber != null ? !modelNumber.equals(that.modelNumber) : that.modelNumber != null) return false;
        if (serialNumber != null ? !serialNumber.equals(that.serialNumber) : that.serialNumber != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = manufacturer != null ? manufacturer.hashCode() : 0;
        result = 31 * result + (modelName != null ? modelName.hashCode() : 0);
        result = 31 * result + (modelNumber != null ? modelNumber.hashCode() : 0);
        result = 31 * result + (serialNumber != null ? serialNumber.hashCode() : 0);
        result = 31 * result + (deviceType != null ? deviceType.hashCode() : 0);
        return result;
    }
}
