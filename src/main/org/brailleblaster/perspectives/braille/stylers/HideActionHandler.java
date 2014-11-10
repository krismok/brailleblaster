package org.brailleblaster.perspectives.braille.stylers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import nu.xom.Attribute;
import nu.xom.Element;

import org.brailleblaster.BBIni;
import org.brailleblaster.perspectives.braille.Manager;
import org.brailleblaster.perspectives.braille.document.BBSemanticsTable.StylesType;
import org.brailleblaster.perspectives.braille.eventQueue.Event;
import org.brailleblaster.perspectives.braille.eventQueue.EventFrame;
import org.brailleblaster.perspectives.braille.eventQueue.EventTypes;
import org.brailleblaster.perspectives.braille.mapping.elements.BrlOnlyMapElement;
import org.brailleblaster.perspectives.braille.mapping.elements.PageMapElement;
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement;
import org.brailleblaster.perspectives.braille.mapping.maps.MapList;
import org.brailleblaster.perspectives.braille.messages.Message;
import org.brailleblaster.perspectives.braille.messages.Sender;
import org.brailleblaster.perspectives.braille.views.tree.BBTree;
import org.brailleblaster.perspectives.braille.views.wp.BrailleView;
import org.brailleblaster.perspectives.braille.views.wp.TextView;
import org.brailleblaster.util.Notify;

public class HideActionHandler {

	Manager manager;
	MapList list;
	TextView text;
	BrailleView braille;
	BBTree tree;
	EventFrame eventFrame;
	
	public HideActionHandler(Manager manager, MapList list){
		this.manager = manager;
		this.list = list;
		text = manager.getText();
		braille = manager.getBraille();
		tree = manager.getTreeView();
	}
	
	public void hideText(){
		if(list.size() > 0 && text.view.getCharCount() > 0){
			if(text.isMultiSelected())
				hideMultipleElements();
			else if(!(list.getCurrent() instanceof BrlOnlyMapElement)){
				int index = list.getCurrentIndex();
				eventFrame = new EventFrame();
				hide(list.getCurrent());
				updateCurrentElement(index);
				manager.addEvent(eventFrame);
			}
			else {
				invalidSelection();
			}
		}
	}
	
	private void hideMultipleElements(){
		int start=text.getSelectedText()[0];
		int end=text.getSelectedText()[1];
		boolean invalid = false;
		
		Set<TextMapElement> itemSet = manager.getElementSelected(start, end);		
		Iterator<TextMapElement> itr = itemSet.iterator();
		invalid = checkSelection(itemSet, start, end);
		Integer index = null;
		if(!invalid){
			itr = itemSet.iterator();
			eventFrame = new EventFrame();
			while (itr.hasNext()) {
				TextMapElement tempElement= itr.next();
				if(index == null)
					index = list.indexOf(tempElement);
				
				hide(tempElement);
			}
			updateCurrentElement(index);
			manager.addEvent(eventFrame);
		}
	}
	
	private void updateCurrentElement(int index){
		if(index >= list.size())
			manager.dispatch(Message.createSetCurrentMessage(Sender.TREE, list.get(index - 1).start, false));
		else
			manager.dispatch(Message.createSetCurrentMessage(Sender.TREE, list.get(index).start, false));
		
		manager.dispatch(Message.createUpdateCursorsMessage(Sender.TREE));
	}

	/** Helper method for hideMultipleElemetns method to check whether selection is valid
	 * @param itemSet : set containing elements in selection
	 * @param start : start of selection
	 * @param end : end of selection
	 * @return true if valid selection, false if invalid
	 */
	private boolean checkSelection(Set<TextMapElement> itemSet, int start, int end){
		boolean invalid = false;
		ArrayList<TextMapElement> addToSet = new ArrayList<TextMapElement>();
		Iterator<TextMapElement> itr = itemSet.iterator();
		while(itr.hasNext()){
			TextMapElement tempElement= itr.next();
			if(tempElement instanceof BrlOnlyMapElement){
				BrlOnlyMapElement b = list.findJoiningBoxline((BrlOnlyMapElement)tempElement);
				if(b == null || b.start > end || b.end < start){
					invalid = true;
					invalidSelection();
					break;
				}
				else if(!itemSet.contains(b))
					addToSet.add(b);
			}
		}
		
		if(addToSet.size() > 0){
			for(int i = 0; i < addToSet.size(); i++)
				itemSet.add(addToSet.get(i));
		}
		
		return invalid;
	}
	
	private void hide(TextMapElement t){
		Message m = new Message(null);
		Element parent = getParent(t);
		ArrayList<TextMapElement> itemList = list.findTextMapElements(list.indexOf(t), parent, true);
		m.put("element", parent);
		m.put("type", "action");
		m.put("action", "skip");
		int treeIndex = manager.getTreeView().getTree().getSelection()[0].getParentItem().indexOf(manager.getTreeView().getTree().getSelection()[0]);
		
		int start = getStart(itemList.get(0), collapseSpaceBefore(itemList.get(0)));
		int end = getEnd(itemList.get(itemList.size() - 1), collapseSpaceAfter(itemList.get(itemList.size() - 1)));
		
		int textLength = end - start;	
		text.replaceTextRange(start, end - start, "");
		
		int brailleStart = getBrailleStart(itemList.get(0), collapseSpaceBefore(itemList.get(0)));
		int brailleEnd = getBrailleEnd(itemList.get(itemList.size() - 1), collapseSpaceAfter(itemList.get(itemList.size() - 1)));
		int brailleLength = brailleEnd - brailleStart;
		braille.replaceTextRange(brailleStart, brailleEnd - brailleStart, "");
		
		int index = list.indexOf(itemList.get(0));
		
		int startPos, brailleStartPos;
		int lastIndex = list.indexOf(itemList.get(itemList.size() - 1));
		if(collapseSpaceBefore(itemList.get(0)) && index != 0){
			if(lastIndex == list.size() - 1){
				startPos = text.view.getCharCount() + 1;
				brailleStartPos = braille.view.getCharCount() + 1;
			}
			else {
				startPos =  list.get(lastIndex + 1).start - textLength;
				brailleStartPos =  list.get(lastIndex + 1).brailleList.getFirst().start - brailleLength;
			}
		}
		else {
			startPos = itemList.get(0).start;
			brailleStartPos = itemList.get(0).brailleList.getFirst().start;
		}
		
		eventFrame.addEvent(new Event(EventTypes.Delete, parent, list.indexOf(itemList.get(0)),  startPos, brailleStartPos, treeIndex));
		
		for(int i = 0; i < itemList.size(); i++){
			Message message = new Message(null);
			message.put("removeAll", true);
			tree.removeItem(itemList.get(i), message);
			list.remove(itemList.get(i));
		}
	
		list.shiftOffsetsFromIndex(index, -textLength, -brailleLength, 0);
	
		manager.getDocument().applyAction(m);
	}
	
	private int getStart(TextMapElement t, boolean collapse){
		int index = list.indexOf(t);
		
		if(index > 0 && list.get(index - 1).end != t.start){
			int linesBefore= t.start - list.get(index - 1).end;
			if(linesBefore > 1 & !collapse)
				return list.get(index - 1).end + 1;
			else
				return list.get(index - 1).end;
		}
		else {
			if(t.start > 0)
				return t.start - 1;
			else
				return t.start;
		}
	}
	
	private int getEnd(TextMapElement t, boolean collapse){
		int index = list.indexOf(t);
		
		if(index < (list.size() - 1) && list.get(index + 1).start  > t.end){
			int linesAfter =  list.get(index + 1).start - t.end;
			if(linesAfter > 1 && !collapse)
				return t.end;
			else
				return list.get(index + 1).start - 1;
		}
		else {	
			if(isLastInList(index))
				return text.view.getCharCount();
			else
				return t.end;
		}
	}
	
	private int getBrailleStart(TextMapElement t, boolean collapse){
		int index = list.indexOf(t);
		
		if(index > 0 && list.get(index - 1).brailleList.getLast().end != t.brailleList.getFirst().start){
			int linesBefore = t.brailleList.getFirst().start -   list.get(index - 1).brailleList.getLast().end ;
			if(linesBefore > 1 && !collapse)
				return list.get(index - 1).brailleList.getLast().end + 1;
			else
				return list.get(index - 1).brailleList.getLast().end;
		}
		else{ 
			if(t.start > 0)
				return t.brailleList.getFirst().start - 1;
			else
				return  t.brailleList.getFirst().start;
		}
	}
	
	private int getBrailleEnd(TextMapElement t, boolean collapse){
		int index = list.indexOf(t);
		
		if(index < (list.size() - 1) && list.get(index + 1).brailleList.getFirst().start  > t.brailleList.getLast().end){
			int linesAfter = list.get(index + 1).brailleList.getFirst().start  - t.brailleList.getLast().end;
			if(linesAfter > 1 && !collapse)
				return t.brailleList.getLast().end;
			else
				return list.get(index + 1).brailleList.getFirst().start - 1;
		}
		else {
			if(isLastInList(list.indexOf(t)))
				return manager.getBrailleView().getCharCount();
			else
				return t.brailleList.getLast().end;
		}
	}
	
	private Element getParent(TextMapElement current) {
		Element parent;
		if(current instanceof PageMapElement || current instanceof BrlOnlyMapElement)
			parent = current.parentElement();
		else
			parent = manager.getDocument().getParent(current.n, true);
		
		return parent;
	}
	
	private void invalidSelection(){
		if(!BBIni.debugging())
			new Notify("In order to hide a boxline both opening and closing boxlines must be selected");
	}
	
	private boolean isHeading(Element e){
		Attribute atr = e.getAttribute("semantics");
		
		if(atr != null){
			if(atr.getValue().contains("heading"))
				return true;
		}
		
		return false;
	}
	
	private boolean collapseSpaceBefore(TextMapElement t){
		int index = list.indexOf(t);
		if(isFirstInList(index))
			return true;
		else {
			if(isHeading(manager.getDocument().getParent(t.n, true))){
				if(list.get(index -  1) instanceof PageMapElement)
					return true;
				
				Element prevParent = manager.getDocument().getParent(list.get(index - 1).n, true);
				String sem = getSemanticAttribute(prevParent);
				if(sem != null && !manager.getStyleTable().get(sem).contains(StylesType.linesAfter)){
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean collapseSpaceAfter(TextMapElement t){
		int index = list.indexOf(t);
		if(isLastInList(index))
			return true;
		else {
			if(isHeading(manager.getDocument().getParent(t.n, true))){
				Element nextParent = manager.getDocument().getParent(list.get(index + 1).n, true);
				String sem = getSemanticAttribute(nextParent);
				if(sem != null && !manager.getStyleTable().get(sem).contains(StylesType.linesBefore)){
					return true;
				}
			}
		}
		
		return false;
	}
	
	private boolean isFirstInList(int index){
		if(index == 0)
			return true;
		else
			return false;
	}
	
	private boolean isLastInList(int index){
		if(index == list.size() - 1)
			return true;
		else
			return false;
	}
	
	private String getSemanticAttribute(Element e){
		Attribute atr = e.getAttribute("semantics");
		if(atr != null){
			String val = atr.getValue();
			String[] tokens = val.split(",");
			if(tokens.length > 1)
				return tokens[1];
		}
		
		return null;
	}
}
