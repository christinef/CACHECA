package edu.ucdavis.cacheca;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

// @author Originally written in C++ by Zhaopeng; converted to Java by Christine

public class Ngram {

	// the array list that stores the candidates word given all n-grams (i.e., 1-grams, 2-grams, ..., (n-1)-grams)
	ArrayList<Map<String, ArrayList<Word>>> mNgramsList;

	public Ngram(URL u, int order, int beam_size){
		
		String line;
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(u.openStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
			
		ArrayList<String> items = new ArrayList<String>();
		ArrayList<Word> words = new ArrayList<Word>();
		
		mNgramsList = new ArrayList<Map<String, ArrayList<Word>>>();
		
		String prefix, lastPrefix = null;
		int n = -1;

		try {
			while ((line = br.readLine()) != null)
			{
				if (line.endsWith("-grams:"))
				{
					// push the words into the list, before changing the "n"
					if (!words.isEmpty())
					{
						// sort the words according to their probabilities
						// See: http://stackoverflow.com/questions/890254/how-can-i-sort-this-arraylist-the-way-that-i-want
						Collections.sort(words, new Comparator<Word>() {
							// first less than the second = neg, first greater than second = pos
							@Override
							public int compare(Word one, Word two) {
								if(one.mProb > two.mProb)
									return -1;
								else if(one.mProb < two.mProb)
									return 1;
								return 0;
							}
						});
						
						if(mNgramsList.size() < n){
							while (mNgramsList.size() < n){
								Map<String, ArrayList<Word>> added = new HashMap<String, ArrayList<Word>>();
								mNgramsList.add(added);
							}
						}
						mNgramsList.get(n-1).put(lastPrefix, new ArrayList<Word>(words));
					}
					// here we don't need to update the last prefix because when "n" changes, the prefix must be different
					words.clear();
					// read the n of the current grams
					n = Integer.parseInt(line.substring(1, line.length()-7)); //See: http://stackoverflow.com/questions/5585779/how-to-convert-string-to-int-in-java
				}
				else
				{
					items = Utilities.split(line, "\t");
					if (items.size() > 1)
					{
						Word word = new Word();
						word.mProb = Float.parseFloat(items.get(0));

						if (items.size() > 2)
						{
							// back-off penalty
							word.mProb += Float.parseFloat(items.get(2));
						}

						word.mProb = (float) Math.pow(10.0, word.mProb);

						prefix = Utilities.getFirstNWords(items.get(1), n-1);
						word.mToken = Utilities.getLastNWords(items.get(1), 1);
						if (prefix.equals(lastPrefix)) 
						{
							words.add(word);
						}
						else
						{
							if (!words.isEmpty())
							{
								// sort the words according to their probabilities
								// See: http://stackoverflow.com/questions/890254/how-can-i-sort-this-arraylist-the-way-that-i-want
								Collections.sort(words, new Comparator<Word>() {
									// first less than the second = neg, first greater than second = pos
									@Override
									public int compare(Word one, Word two) {
										if(one.mProb > two.mProb)
											return -1;
										else if(one.mProb < two.mProb)
											return 1;
										return 0;
									}
								});
								
								if(mNgramsList.size() <= n){
									while (mNgramsList.size() <= n){
										Map<String, ArrayList<Word>> added = new HashMap<String, ArrayList<Word>>();
										mNgramsList.add(added);
									}
								}
								mNgramsList.get(n-1).put(lastPrefix, new ArrayList<Word>(words));
							}
							
							lastPrefix = prefix;
							words.clear();
							words.add(word);
						}
					}
				}
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			try {
				if(br != null)
					br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (!words.isEmpty())
		{
			if(mNgramsList.size() < n){
				while (mNgramsList.size() < n){
					Map<String, ArrayList<Word>> added = new HashMap<String, ArrayList<Word>>();
					mNgramsList.add(added);
				}
			}
			mNgramsList.get(n-1).put(lastPrefix, words);	
		}

	}

	/**
	 * get the candidate tokens when given the prefix
	 * @param prefix the previous (n-1) grams
	 * @param use_backoff Using back-off, when there is no candidates given (n-1) grams,
                        we will search the candidates given the previous (n-2) grams,
                        ...
                        until candidates are returned
	 * @param candidates the result candidates
	 **/
	public ArrayList<Word> getCandidates(String prefix, boolean useBackoff){
		ArrayList<Word> candidates = new ArrayList<Word>();

		int n = Utilities.countWords(prefix);
		// here n is the number of grams in the prefix, the real "n" should be n+1
		// therefore, here we use "n" rather than "n-1"

		Map<String, ArrayList<Word>> ngramMap = mNgramsList.get(n);
		ArrayList<Word> val = ngramMap.get(prefix);
		if (val != null)
		{
			candidates = val;
			return candidates;
		}
		else
		{
			if (useBackoff)
			{
				// when n is less or equal to 1, we cannot do the back-off operation
				if (n < 1)
					return null;

				String useBackoffPrefix;
				useBackoffPrefix = Utilities.getLastNWords(prefix, n-1);

				return getCandidates(useBackoffPrefix, useBackoff);
			}
			else
			{
				return null;
			}
		}
	}

}