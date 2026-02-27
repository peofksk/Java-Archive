package ver_1;

public class Beat {
	private double time;
	private String noteName;
	
	//Getter, Setter
	public double getTime() {
		return time;
	}
	public void setTime(double time) {
		this.time = time;
	}
	public String getNoteName() {
		return noteName;
	}
	public void setNoteName(String noteName) {
		this.noteName = noteName;
	}
	
	//Constructor
	public Beat(double time, String noteName) {
		super();
		this.time = time;
		this.noteName = noteName;
	}
	
	public Beat() {
		
	}
}
