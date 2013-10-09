package data;

import java.util.ArrayList;
import java.util.List;

public class DataSet {

	public List<DataBit> data;

	public DataSet() {
		data = new ArrayList<DataBit>();
	}

	public void add(DataBit d) {
		data.add(d);
	}

	public DataSet filterBy(Object o, int index) {
		DataSet d = new DataSet();
		for (int i = 0; i < data.size(); i++)
			if (index == -1) {
				if (o.equals(data.get(i).time)) d.add(data.get(i));
			} else {
				if (data.get(i).data[index].equals(o)) d.add(data.get(i));
			}
		return d;
	}

	public void debugLog() {
		for (int i = 0; i < data.size(); i++)
			System.out.println(data.get(i).toString());
	}

	// TODO: Support Non-Double values
	public static DataSet mergeSets(DataSet a, DataSet b) {
		DataSet d = new DataSet();

		int aSize = a.data.get(0).data.length;
		int bSize = b.data.get(0).data.length;
		int iA = 0, iB = 0;
		DataBit db = null;
		while (iA < a.data.size() || iB < b.data.size()) {
			Object[] o = new Object[aSize + bSize];
			if (iA == a.data.size()) {
				// System.out.println("-B-");
				// Use Only B.
				for (int i = 0; i < aSize; i++)
					o[i] = new Double(0);
				for (int i = aSize; i < aSize + bSize; i++)
					o[i] = b.data.get(iB).data[i - aSize];
				db = new DataBit(b.data.get(iB).time, o);

				iB++;
			} else if (iB == b.data.size()) {
				// System.out.println("-A-");
				// Use Only A.
				for (int i = 0; i < aSize; i++)
					o[i] = a.data.get(iA).data[i];
				for (int i = aSize; i < aSize + bSize; i++)
					o[i] = new Double(0);
				db = new DataBit(a.data.get(iA).time, o);

				iA++;
			} else if (a.data.get(iA).time < b.data.get(iB).time) {
				// System.out.println("-Ab-");
				// Use A, Extrap B.
				for (int i = 0; i < aSize; i++)
					o[i] = a.data.get(iA).data[i];
				for (int i = aSize; i < aSize + bSize; i++) {
					if (iB == 0) o[i] = new Double(0);
					else if (iB + 1 == b.data.size()) o[i] = b.data.get(iB).data[i - aSize];
					else o[i] = ((Double) b.data.get(iB).data[i - aSize] + (Double) b.data.get(iB + 1).data[i - aSize]) / 2.0;
				}
				db = new DataBit(a.data.get(iA).time, o);

				iA++;
			} else if (b.data.get(iB).time < a.data.get(iA).time) {
				// System.out.println("-Ba-");
				// Use B, Extrap A.
				for (int i = 0; i < aSize; i++) {
					if (iA == 0) o[i] = new Double(0);
					else if (iA + 1 == a.data.size()) o[i] = a.data.get(iA).data[i];
					else o[i] = ((Double) a.data.get(iA).data[i] + (Double) a.data.get(iA + 1).data[i]) / 2.0;
				}
				for (int i = aSize; i < aSize + bSize; i++)
					o[i] = b.data.get(iB).data[i - aSize];

				db = new DataBit(b.data.get(iB).time, o);

				iB++;
			} else if (a.data.get(iA).time == b.data.get(iB).time) {
				// System.out.println("-AB-");
				// Use Both, No Extrap
				for (int i = 0; i < aSize; i++)
					o[i] = a.data.get(iA).data[i];
				for (int i = aSize; i < aSize + bSize; i++)
					o[i] = b.data.get(iB).data[i - aSize];
				db = new DataBit(a.data.get(iA).time, o);

				iA++;
				iB++;
			}

			d.add(db);
		}
		return d;
	}
}
