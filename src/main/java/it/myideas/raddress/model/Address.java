package it.myideas.raddress.model;

public class Address {

	private String idno;
	private String city;
	private String address;
	
	public Address(String idno, String city, String address) {
		super();
		this.idno = idno;
		this.city = city;
		this.address = address;
	}

	public String getIdno() {
		return idno;
	}

	public String getCity() {
		return city;
	}

	public String getAddress() {
		return address;
	}

	public String getFQAddress() {
	    return address + ", " + city;
	}
	
	@Override
	public String toString() {
		return getFQAddress();
	}
	
}
