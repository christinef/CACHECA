package edu.ucdavis.cacheca;

import java.net.URL;

//Author: This class was originally written in C++ by Zhaopeng; converted to Java by Christine

public class Data {

	//data wrapper class
	
	static boolean USE_BACKOFF = true;

	static int NGRAM_ORDER = 3;
	
	static boolean USE_CACHE = true;
	static int CACHE_ORDER = 10;
	static int CACHE_MIN_ORDER = 3;
	static boolean USE_FILE_CACHE = true;
	
	static int BEAM_SIZE = 10;
		
	static Ngram NGRAM;
	static Cache CACHE;


	static boolean Init(URL u, int ngram_order){
		NGRAM_ORDER = ngram_order;
		NGRAM = new Ngram(u, NGRAM_ORDER, BEAM_SIZE);
		CACHE = new Cache(CACHE_ORDER, CACHE_MIN_ORDER);
		return true;
	}
	
}
