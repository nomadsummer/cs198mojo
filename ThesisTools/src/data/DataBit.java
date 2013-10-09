package data;

public class DataBit {

	public long time;
	public Object[] data;

	public DataBit(long time, Object[] data) {
		this.time = time;
		this.data = data;
	}

	public String toString() {
		String s = time + "\t";
		for (int i = 0; i < data.length; i++)
			s += data[i].toString() + "\t";
		return s;
	}
}
