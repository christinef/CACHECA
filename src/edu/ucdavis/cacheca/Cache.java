package edu.ucdavis.cacheca;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

// @author Originally written in C++ by Zhaopeng; converted to Java by Christine

public class Cache {
	private Map<String, Record> mRecords;
	private int mMinOrder;
	private int mOrder;

	public Cache(int order, int minOrder){
		mRecords = new HashMap<String, Record>();
		init(order, minOrder);
	}

	private void init(int order, int minOrder){
		mMinOrder = minOrder;
		mOrder = order;
	}

	void clear(){
		mRecords.clear();
	}

	void build(String inputFile){
		ArrayList<String> tokens = new ArrayList<String>();
		tokens.add("<s>");
		String line, cachePrefix;
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(inputFile));
			line = br.readLine();
			while(line != null){
				String[] splitString = line.split("((?<=\\.)|(?=\\.))| |((?<=\\{)|(?=\\{))|((?<=\\()|(?=\\())|((?<=\\[)|(?=\\[))|((?<=\\;)|(?=\\;))");
				for(String token : splitString){
					if (token != null && !(token.equals(""))){
						token.trim();
						token = token.replaceAll("\t", ""); 
						token = token.replaceAll(" ", ""); 
						tokens.add(token);
					}
				}
				line = br.readLine();
			}
			br.close();
		} catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }

		for(int i = mMinOrder-1; i < tokens.size(); i++){
			int start = i-(mOrder-1) > 0 ? i-(mOrder-1) : 0;
			int end = i-1;
			StringBuilder mergedString = new StringBuilder();
			if (start < end){
				for(int j = start; j < end; j++){
					mergedString.append(tokens.get(j) + " ");				
				}
				mergedString.append(tokens.get(end));
			}
			cachePrefix = mergedString.toString();
			update(cachePrefix, tokens.get(i));
		}		
	}

	void update(String prefix, String token){
		int n = Utilities.countWords(prefix);
		for(int i = n; i >= mMinOrder-1; --i){
			String newPrefix = Utilities.getLastNWords(prefix, i);
			Record val = mRecords.get(newPrefix);
			if (val != null){
				val.update(token);
			}
			else{
				mRecords.put(newPrefix, new Record(token));
			}

		}
	}
	
	public ArrayList<Word> updateCandidates(String prefix, ArrayList<Word> candidates){
		int cacheCount = getCount(prefix);
		if(cacheCount != 0){
			float cache_discount = (float)cacheCount/(cacheCount+1);
			float ngram_discount = 1-cache_discount;

			// found cache records of the prefix
			Map<String, Integer> tokenCounts = getTokenCounts(prefix);

			// update the information of candidates from ngram model
			for (int i=0; i<(int)candidates.size(); ++i)
			{
				// discount the probability first
				candidates.get(i).mProb *= ngram_discount;

				Integer val = tokenCounts.get(candidates.get(i).mToken);
				if (val != null)
				{
					candidates.get(i).mProb += cache_discount * val/cacheCount;
					tokenCounts.remove(candidates.get(i).mToken);
				}
			}

			// add the left records in the cache to the candidates   

			// See http://stackoverflow.com/questions/46898/how-do-i-iterate-over-each-entry-in-a-map
			for (Map.Entry<String, Integer> entry : tokenCounts.entrySet())
			{
				candidates.add(new Word(entry.getKey(), cache_discount * ((float)entry.getValue()/cacheCount)));
			}

			// See: http://stackoverflow.com/questions/890254/how-can-i-sort-this-arraylist-the-way-that-i-want
			Collections.sort(candidates, new Comparator<Word>() {
				// first less than the second = neg, first greater than second = pos
				@Override
				public int compare(Word one, Word two) {
					if(one.mProb > two.mProb)
						return 1;
					else if(one.mProb < two.mProb)
						return -1;
					return 0;
				}
			});
			
		}
		return candidates;
	}

	/**
	 * get the possible suggestions from the cache
	 * @param prefix       the previous (n-1) tokens
	 * @return 
	 **/
	Map<String, Integer> getTokenCounts(String prefix)
	{
		int n = Utilities.countWords(prefix);
		for (int i=n; i>=mMinOrder-1; --i)
		{
			// use the prefix from longest to m_min_order until we match the prefix
			String newPrefix = Utilities.getLastNWords(prefix, i);
			
			Record val = mRecords.get(newPrefix);
			if (val != null)
			{
				return val.getTokenCounts();
			}
		}
		return null;
	}

	/**
	 * get the number of records for a given prefix (to calculate the discount)
	 * @param prefix       the previous (n-1) tokens
	 **/
	int getCount(String prefix)
	{
		int n = Utilities.countWords(prefix);
		for (int i=n; i>=mMinOrder-1; --i)
		{
			String newPrefix = Utilities.getLastNWords(prefix, i);
			Record val = mRecords.get(newPrefix);
			if (val != null)
			{
				return val.getCount();
			}
		}

		return 0;
	}


	//inner class "Record"
	private class Record{
		private int mCount;
		private Map<String, Integer> mTokens;

		private Record (){
			mCount = 0;
			mTokens = new HashMap<String, Integer>();
		}

		private Record (String token){
			mCount = 1;
			mTokens = new HashMap<String, Integer>();
			mTokens.put(token, 1);
		}

		void update(String token){
			mCount++;
			Integer val = mTokens.get(token);
			if(val != null){
				mTokens.put(token, ++val);
			}
			else{
				mTokens.put(token, 1);
			}
		}

		Map<String, Integer> getTokenCounts(){
			return mTokens;
		}

		int getCount(){
			return mCount;
		}	
	}	
}