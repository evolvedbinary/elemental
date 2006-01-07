/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-06 Wolfgang M. Meier
 *  wolfgang@exist-db.org
 *  http://exist.sourceforge.net
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  $Id$
 */
package org.exist.xquery.functions;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.exist.EXistException;
import org.exist.dom.ExtArrayNodeSet;
import org.exist.dom.Match;
import org.exist.dom.NodeProxy;
import org.exist.dom.NodeSet;
import org.exist.storage.NativeTextEngine;
import org.exist.storage.analysis.TextToken;
import org.exist.storage.analysis.Tokenizer;
import org.exist.util.GlobToRegex;
import org.exist.xquery.Constants;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.util.ExpressionDumper;
import org.exist.xquery.value.Sequence;

/**
 *  phrase() function : search an exact phrase in a NodeSet
 *
 *@author     Bruno Chatel <bcha@chadocs.com>
 *@created    March 30, 2005
 */
public class ExtPhrase extends ExtFulltext {


	/**
	 * 
	 * @param context
	 */
	public ExtPhrase(XQueryContext context) {
		super(context, Constants.FULLTEXT_AND);
	}

    
    /**
     * 
     * @param searchArg
     * @param nodes 
     * @return 
     */
    public Sequence evalQuery(String searchArg,	NodeSet nodes) throws XPathException {
		try {
			getSearchTerms(context, searchArg);
		} catch (EXistException e) {
			throw new XPathException(e.getMessage(), e);
		}
		NodeSet hits = processQuery(nodes);
		if (hits == null)
			return Sequence.EMPTY_SEQUENCE;
				
		boolean hasWildcards = false;
		for(int i = 0; i < terms.length; i++) {
			hasWildcards |=
				NativeTextEngine.containsWildcards(terms[i]);
		}
		return hasWildcards
			? patternMatch(context, hits)
			: exactMatch(context, hits);
	}

	/**
	 * 
	 * @param context
	 * @param result
	 * @return
	 */
	private Sequence exactMatch(XQueryContext context, NodeSet result) {
		String value;
		String term;
		String word;
		TextToken token;
		NodeProxy current;
		NodeSet r = new ExtArrayNodeSet();
		Tokenizer tok = context.getBroker().getTextEngine().getTokenizer();
		int j;
		long gid=0;
		int frequency = 0;
		// define search phrase for matches
		String matchTerm="";
		for(int k=0;k<terms.length;k++) {
			matchTerm=matchTerm+terms[k];
			if(k!=terms.length-1)
				matchTerm=matchTerm+"\\W*"; 
		}
		// iterate on results
		for (Iterator i = result.iterator(); i.hasNext();) {
			Match nextMatch;
			Vector matchGid = new Vector();
			current = (NodeProxy) i.next();
			// get first match
			nextMatch = current.getMatches();
			// remove previously found matches on current
			current.setMatches(null);
			// iterate on attach matches, with unicity of related nodeproxy gid
			while(nextMatch != null) {
				gid=nextMatch.getNodeId(); 
				// if current gid has not been previously processed
				if(!matchGid.contains(new Long(gid))) {
					NodeProxy mcurrent = new NodeProxy(current.getDocument(), gid);
                    Match match = null;
                    int firstOffset = -1;
					// add it in gid array
					matchGid.add(new Long(gid));
					value = mcurrent.getNodeValue();
					tok.setText(value);
					j = 0;
					if (j < terms.length)
						term = terms[j];
					else
						break;
					frequency = 0;
					while ((token = tok.nextToken()) != null) {
						word = token.getText().toLowerCase();
						if (word.equalsIgnoreCase(term)) {
							j++;
							if (j == terms.length) {
								// all terms found
                                if (match == null)
                                    match = new Match(gid, matchTerm);
                                if (firstOffset < 0)
                                    firstOffset = token.startOffset();
                                match.addOffset(firstOffset, token.endOffset() - firstOffset);
								frequency++;
								// start again on fist term
								j=0;
								term = terms[j];
								continue;
							} else {
								term = terms[j];
                                if (firstOffset < 0)
                                    firstOffset = token.startOffset();
                            }
						} else if (j > 0 && word.equalsIgnoreCase(terms[0])) {
							// first search term found: start again
							j=1;
							term = terms[j];
                            firstOffset = token.startOffset();
							continue;
						} else {
							//	reset
							j = 0;
                            firstOffset = -1;
							term = terms[j];
						}
					}
					// if phrase found
					if(frequency!=0) {
						// add new match to current
						current.addMatch(match);
						// add current to result
						r.add(current);
						// reset frequency
						frequency=0;
					}				
				}
				// process next match
				nextMatch = nextMatch.getNextMatch();
			}
		}
//		LOG.debug("found " + r.getLength());
		return r;
	}

	/**
	 * 
	 * @param context
	 * @param result
	 * @return
	 */
	private Sequence patternMatch(XQueryContext context, NodeSet result) {
		// generate list of search term patterns
	    Pattern patterns[] = new Pattern[terms.length];
        Matcher matchers[] = new Matcher[terms.length];
        for (int i = 0; i < patterns.length; i++)
            try {
                patterns[i] = Pattern.compile(GlobToRegex.globToRegexp(terms[i]), 
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
                matchers[i] = patterns[i].matcher("");
            } catch (PatternSyntaxException e) {
                LOG.warn("malformed pattern", e);
                return Sequence.EMPTY_SEQUENCE;
            }
		
		// walk through hits 
		String value;
		Pattern term;
		String word;
		TextToken token;
		NodeProxy current;
		ExtArrayNodeSet r = new ExtArrayNodeSet();
		Matcher matcher;
		Tokenizer tok = context.getBroker().getTextEngine().getTokenizer();
		int j;
		long gid=0;
		String matchTerm=null;
		
		for (Iterator i = result.iterator(); i.hasNext();) {
			Match nextMatch;
			Vector matchGid = new Vector();
			current = (NodeProxy) i.next();
			// get first match
			nextMatch = current.getMatches();
			// remove previously found matches on current
			current.setMatches(null);
			// iterate on attach matches, with unicity of related nodeproxy gid
			while(nextMatch != null) {
				Hashtable matchTable = new Hashtable();
				gid=nextMatch.getNodeId(); 
				// if current gid has not been previously processed
				if(!matchGid.contains(new Long(gid))) {
					NodeProxy mcurrent = new NodeProxy(current.getDocument(), gid);
					// add it in gid array
					matchGid.add(new Long(gid));
					value = mcurrent.getNodeValue();
					tok.setText(value);
					j = 0;
					if (j < patterns.length) {
						term = patterns[j];
                        matcher = matchers[j];
                    } else
						break;
					matchTerm=null;
					while ((token = tok.nextToken()) != null) {
						word = token.getText().toLowerCase();
                        matcher.reset(word);
                        matchers[0].reset(word);
						if (matcher.matches()) {
							j++;
							if(matchTerm==null)
								matchTerm=word;
							else
								matchTerm=matchTerm+"\\W*"+word;  
							if (j == patterns.length) {
								// all terms found
								if(matchTable.containsKey(matchTerm)) {
									// previously found matchTerm
									Match match = (Match)(matchTable.get(matchTerm));
                                    match.addOffset(token.startOffset(), matchTerm.length());
								} else {
									Match match = new Match(gid, matchTerm);
                                    match.addOffset(token.startOffset(), matchTerm.length());
									matchTable.put(matchTerm,match);
								}
								// start again on fist term
								j=0;
								term = patterns[j];
                                matcher = matchers[j];
								matchTerm=null;
								continue;
							} else {
								term = patterns[j];
                                matcher = matchers[j];
                            }
						} else if (j > 0 && matchers[0].matches()) {
							// first search term found: start again
							j=1;
							term = patterns[j];
                            matcher = matchers[j];
							matchTerm=word;
							continue;
						} else {
							// reset
							j = 0;
							term = patterns[j];
                            matcher = matchers[j];
							matchTerm=null;
							continue;
						}
					}
					// one or more match found
					if(matchTable.size()!=0) {
						Enumeration eMatch = matchTable.elements();
						while(eMatch.hasMoreElements()){
							Match match = (Match)(eMatch.nextElement());
							current.addMatch(match);
					     }
						// add current to result
						r.add(current);
					}			
				}
				// process next match
				nextMatch = nextMatch.getNextMatch();
			}
		}
		return r;	
	}

	/* (non-Javadoc)
     * @see org.exist.xquery.functions.ExtFulltext#dump(org.exist.xquery.util.ExpressionDumper)
     */
    public void dump(ExpressionDumper dumper) {
        dumper.display("phrase(");
        path.dump(dumper);
        dumper.display(", ");
        searchTerm.dump(dumper);
        dumper.display(")");
    }
    

}
