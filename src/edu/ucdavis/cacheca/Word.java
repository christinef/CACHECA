package edu.ucdavis.cacheca;

//@author Originally written in C++ by Zhaopeng; converted to Java by Christine

//Note: originally an inner class of Ngram.cpp

public class Word {
	String mToken;
	float mProb;

	public Word(){
		mProb = 0.0f;
	}

	public Word(String token, float prob){
		mProb = prob;
		mToken = token;
	}
}
