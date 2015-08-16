package it.myideas.raddress.model;

public class AddressResult extends Address {

	private double score;
	public AddressResult(String idno, String city, String address, double score) {
		super(idno, city, address);
		this.score = score;
	}
	
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return score + "# " + super.toString();
	}
	

}
