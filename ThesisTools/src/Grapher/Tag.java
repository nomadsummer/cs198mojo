package grapher;

public class Tag {
	public String tag;
	public int index;
	public Object type;

	private boolean div;

	public Tag(String tag, int index, Object type) {
		this(tag, index, type, false);
	}

	public Tag(String tag, int index, Object type, boolean div) {
		super();
		this.div = div;
		this.tag = tag;
		this.index = index;
		this.type = type;
	}

	public Object process(int swarmSize, int helpingSize, Object o) {
		if (div) {
			return (Double) (((Number) o).doubleValue() / swarmSize);
		}
		return o;
	}

}
