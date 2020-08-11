package stb.localiser.depend;

import java.util.*;

public class Production {
	public String nonTerminal;
	public ArrayList<String> definitions = new ArrayList<String>();

	public Production() { }
	// public Productions(ing nT) { nonTerminal = nT; }
	public Production(String nT, ArrayList<String> defs) {
		nonTerminal = nT;
		definitions.addAll(defs);
	}
	public void display () {
		System.out.print(nonTerminal+" ");
		for(String def : definitions) 
			System.out.print(def+" ");
		System.out.println();
	}

}
