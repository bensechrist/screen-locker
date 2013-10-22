package com.example.screenlocker;

public class Zone {
	
	private String macAddr;
	private String SSID;
	private int _id;
	
	public String getMacAddr() {
		return macAddr;
	}
	public void setMacAddr(String macAddr) {
		this.macAddr = macAddr;
	}
	public String getSSID() {
		return SSID;
	}
	public void setSSID(String SSID) {
		this.SSID = SSID;
	}
	public int get_id() {
		return _id;
	}
	public void set_id(int _id) {
		this._id = _id;
	}

}
