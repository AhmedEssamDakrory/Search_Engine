package main.phraseSearching;

import java.util.ArrayList;
import java.util.List;

public class KMP {
	private int[] lps;
	List<String> p;
	
	public KMP(List<String> p) {
		this.p = p;
		this.lps = new int[p.size()+1];
	}
	
	public void buildLps() {
		int j = -1;
		lps[0] = -1;
		for(int i = 1; i < p.size(); ++i) {
			while(j > -1 && p.get(j+1).equals(p.get(i))) {
				j = lps[j];
			}
			if(p.get(j+1).equals(p.get(i))) ++j;
			lps[i] = j;
		}
	}
	
	public boolean match(List<String> t) {
		int i = 0;
		int j = -1;
		while(i < t.size()) {
			if(t.get(i).equals(p.get(j+1))) {
				++i;
				++j;
			} else {
				if(j != -1) j = lps[j];
				else ++i;
			}
			if(j == p.size()-1) {
				j = lps[j];
				return true;
			}
		}
		return false;
	}
	
//	public static void main(String[] args) {
//		List<String> p = new ArrayList<String>();
//		List<String> t = new ArrayList<String>();
//		p.add("hello");
//		p.add("ahmed");
//		p.add("its");
//		p.add("me");
//		t.add("wow");
//		t.add("ahmed");
//		t.add("is");
//		t.add("here");
//		t.add("hello");
//		t.add("ahmed");
//		t.add("its");
//		t.add("me");
//		t.add("your");
//		t.add("friend");
//		KMP k = new KMP(p);
//		k.buildLps();
//		if(k.match(t)) {
//			System.out.println("ok");
//		} else {
//			System.out.println("Not ok");
//		}
//	}
}
