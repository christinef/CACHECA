
package edu.ucdavis.cacheca;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

// @author Christine Franks

public class JavaCompletionProposals implements IJavaCompletionProposalComputer{

	public JavaCompletionProposals(){
		IPartListener2 openListener = new IPartListener2() {
			@Override
			public void partActivated(IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(IWorkbenchPartReference partRef) {}

			@Override
			public void partDeactivated(IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(IWorkbenchPartReference partRef) {
				IEditorInput ip = partRef.getPage().getActiveEditor().getEditorInput();
				IPath path = ((FileEditorInput)ip).getPath();
				String p = path.toOSString();
				CachecaComputer.getInstance(p);
			}

			@Override
			public void partInputChanged(IWorkbenchPartReference partRef) {
			}
		};
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().addPartListener(openListener);
	}
	
	@Override
	public void sessionStarted() {
	}

	@Override
	public List<ICompletionProposal> computeCompletionProposals(ContentAssistInvocationContext context, IProgressMonitor monitor) {	
		JavaContentAssistInvocationContext ctx = (JavaContentAssistInvocationContext)context;
		
		int masterOffset = ctx.getInvocationOffset()-1;
			
		CompletionProposalCollector collector = new CompletionProposalCollector(ctx.getCompilationUnit());
        collector.setInvocationContext(ctx);
         
		try {
			ctx.getCompilationUnit().codeComplete(ctx.getInvocationOffset(), collector, new NullProgressMonitor());
		} catch (JavaModelException e2) {
			e2.printStackTrace();
		}
		
		IJavaCompletionProposal[] jProps = collector.getJavaCompletionProposals();
		
		Arrays.sort(jProps, new Comparator<IJavaCompletionProposal>(){
			@Override
			public int compare(IJavaCompletionProposal one, IJavaCompletionProposal two){
				if(one.getRelevance() < two.getRelevance())
					return 1;
				else if(one.getRelevance() > two.getRelevance())
					return -1;
				else {
					return one.getDisplayString().compareTo(two.getDisplayString());
				}
			}
		});        
		
		List<ICompletionProposal> eclipseProposals = new ArrayList<ICompletionProposal>();
		
		int proposalFractionFromEclipse = 50;
		if(jProps.length < proposalFractionFromEclipse){
			proposalFractionFromEclipse = jProps.length;
		}
		
		for (int j = 0; j < proposalFractionFromEclipse; j++) {
			eclipseProposals.add(jProps[j]);
		}
		
		//custom completions
		
		//get offset
		int replacementLength = 0;
		try {
			int offset = masterOffset;
			if(offset < 0) return eclipseProposals;
			char c = ctx.getDocument().getChar(offset);
			while (c != '.'){
				offset--;
				replacementLength++;
				c = ctx.getDocument().getChar(offset);
			}
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		
		//get prefix
		StringBuilder b = new StringBuilder();
		try {
			int numberOfSeparators = 0;
			int offset = masterOffset;
			offset -= replacementLength;
			char c = ctx.getDocument().getChar(offset);
			boolean prevCharWasWhitespace = false;
			while (numberOfSeparators < 10 && offset >= 0){
				if (!(Character.isWhitespace(ctx.getDocument().getChar(offset)))){
					prevCharWasWhitespace = false;
					b.append(c);
				}
				else {
					if(prevCharWasWhitespace == false)
						b.append(" ");
					prevCharWasWhitespace = true;
				}
				if(c == ' ' || c == '.' || c == '(' || c == ')' || c == '{' || c == '}' || c == ';' || c == '[' || c == ']' || c == '\n') {
					numberOfSeparators++;
				}
				offset--;
				if (offset < 0) {
					break;
				}
				c = ctx.getDocument().getChar(offset);
			}
		} catch (BadLocationException e) {
			e.printStackTrace();
		}		
		String pref = b.reverse().toString();
		
		//get inputs
		StringBuilder input = new StringBuilder();
		try {
			int offset = masterOffset;
			char c = ctx.getDocument().getChar(offset);
			while (c != '.'){
				input.append(c);
				offset--;
				c = ctx.getDocument().getChar(offset);
			}
		} catch (BadLocationException e) {
			e.printStackTrace();
		}		
		String inp = input.reverse().toString().trim();
				
		IPath path = ctx.getCompilationUnit().getResource().getRawLocation();
		String realPath = path.toOSString();
		
		List<ICompletionProposal> cachecaProposals = new ArrayList<ICompletionProposal>();
		
		CachecaComputer comp = CachecaComputer.getInstance(realPath);
		if(CachecaComputer.isInitialized() == true){
			ArrayList<Word> p = comp.getCandidates(pref);
			int found = 0;
			
			for (int i = p.size()-1; i >= 0 && found < 100; i--){
				String token = p.get(i).mToken;
				if(token.length() < inp.length())
					continue;
				if(inp.length() >= 1 ? p.get(i).mToken.substring(0, inp.length()).equals(inp) : true){
					cachecaProposals.add(new CompletionProposal(token, masterOffset-replacementLength+1, replacementLength, token.length(), null,
							token, null, "Suggested by CACHECA with probability " + p.get(i).mProb));
					found++;
				}
			}			
		}
		
		// form blended proposal list
		
		List<ICompletionProposal> finalProposals = new ArrayList<ICompletionProposal>();
		
		// merge common proposals and add to top of final list
		int counter = 0;
		for (int j = 0; j < eclipseProposals.size(); j++) {
			ICompletionProposal cp = eclipseProposals.get(counter);
			String eclipseProposalString = cp.getDisplayString();
			int parenIndex = eclipseProposalString.indexOf('(');
			int spaceIndex = eclipseProposalString.indexOf(' ');
			if (parenIndex > 1 || spaceIndex > 1) {
				int index;
				if (spaceIndex < 1) {
					index = parenIndex;
				} else if (parenIndex < 1) {
					index = spaceIndex;
				} else {
					index = Math.min(parenIndex, spaceIndex);
				}
				eclipseProposalString = eclipseProposalString.substring(0, index);
			}
			counter++;
			for (int i = 0; i < cachecaProposals.size(); i++) {
				if (eclipseProposalString.equals(cachecaProposals.get(i).getDisplayString())){
					finalProposals.add(cp);
					counter--; 
					eclipseProposals.remove(counter); 
					cachecaProposals.remove(i);
				}
			}
		}
		
		// take top suggestions, if the number of decalca ones is short
		while (cachecaProposals.size() > 0 && cachecaProposals.size() < 3) {
			String cachecaProposalString = cachecaProposals.get(0).getDisplayString();
			
			boolean foundFlag = false;
			int checkSentinel = 0;
			for(ICompletionProposal eclipseProposal : eclipseProposals) {
				if (checkSentinel > 100) {
					break;
				}
				String eclipseProposalString = eclipseProposal.getDisplayString();
				int parenIndex = eclipseProposalString.indexOf('(');
				int spaceIndex = eclipseProposalString.indexOf(' ');
				if (parenIndex > 1 || spaceIndex > 1) {
					int index;
					if (spaceIndex < 1) {
						index = parenIndex;
					} else if (parenIndex < 1) {
						index = spaceIndex;
					} else {
						index = Math.min(parenIndex, spaceIndex);
					}
					eclipseProposalString = eclipseProposalString.substring(0, index);
				}
				if (eclipseProposalString.equals(cachecaProposalString)) {
					finalProposals.add(eclipseProposal);
					cachecaProposals.remove(0);
					eclipseProposals.remove(eclipseProposal);
					foundFlag = true;
					break;
				}
				checkSentinel++;
			}	
			
			if (foundFlag == false) {
				finalProposals.add(cachecaProposals.get(0));
				cachecaProposals.remove(0);
			}
		}
		
		// interleave the rest
		while(eclipseProposals.size() != 0 || cachecaProposals.size() != 0) { 
			if (eclipseProposals.size() != 0) {
				finalProposals.add(eclipseProposals.get(0));
				eclipseProposals.remove(0);		
			}
			if (cachecaProposals.size() != 0) {
				finalProposals.add(cachecaProposals.get(0));
				cachecaProposals.remove(0);
			}
		}
		
		// since we added them in a stack-like way, we need to reverse it to get correct ordering
		Collections.reverse(finalProposals);
		
		return finalProposals;	
	}

	@Override
	public List<IContextInformation> computeContextInformation(
			ContentAssistInvocationContext context, IProgressMonitor monitor) {
		return null;
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public void sessionEnded() {
	}
	
}