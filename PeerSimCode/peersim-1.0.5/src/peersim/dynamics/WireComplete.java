package peersim.dynamics;


import peersim.graph.*;


public class WireComplete extends WireGraph {

// ===================== initialization ==============================
// ===================================================================

public WireComplete(String prefix) { super(prefix); }

// ===================== public methods ==============================
// ===================================================================


public void wire(Graph g) {
	
	GraphFactory.wireComplete(g);
}

@Override
public boolean execute() {
	// TODO Auto-generated method stub
	return false;
}


}

