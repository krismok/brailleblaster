package org.brailleblaster.perspectives.braille.stylers;

import java.util.ArrayList;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.ParentNode;
import nu.xom.Text;

import org.brailleblaster.BBIni;
import org.brailleblaster.document.SemanticFileHandler;
import org.brailleblaster.perspectives.braille.Manager;
import org.brailleblaster.perspectives.braille.document.BrailleDocument;
import org.brailleblaster.perspectives.braille.eventQueue.Event;
import org.brailleblaster.perspectives.braille.mapping.elements.BrailleMapElement;
import org.brailleblaster.perspectives.braille.mapping.elements.BrlOnlyMapElement;
import org.brailleblaster.perspectives.braille.mapping.elements.PageMapElement;
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement;
import org.brailleblaster.perspectives.braille.mapping.maps.MapList;
import org.brailleblaster.perspectives.braille.messages.Message;
import org.brailleblaster.perspectives.braille.messages.Sender;
import org.brailleblaster.perspectives.braille.viewInitializer.ViewInitializer;
import org.brailleblaster.perspectives.braille.views.tree.BBTree;
import org.brailleblaster.perspectives.braille.views.wp.BrailleView;
import org.brailleblaster.perspectives.braille.views.wp.TextView;
import org.brailleblaster.util.FileUtils;

public class ElementInserter {

	BrailleDocument doc;
	MapList list;
	Manager manager;
	ViewInitializer vi;
	TextView text;
	BrailleView braille;
	BBTree tree;
	
	public ElementInserter(ViewInitializer vi, BrailleDocument doc, MapList list, Manager manager){
		this.vi = vi;
		this.doc = doc;
		this.list = list;
		this.manager = manager;
		this.text = manager.getText();
		this.braille = manager.getBraille();
		this.tree = manager.getTreeView();
	}
	
	public void insertElement(Message m){
		if(m.getValue("atStart").equals(true))
			insertElementAtBeginning(m);
		else
			insertElementAtEnd(m);
	}
	
	private void insertElementAtBeginning(Message m){
		if(list.getCurrentIndex() > 0 && list.getCurrent().start != 0)
			doc.insertEmptyTextNode(vi, list, list.getCurrent(),  list.getCurrent().start - 1, list.getCurrent().brailleList.getFirst().start - 1,list.getCurrentIndex(),(String) m.getValue("elementName"));
		else
			doc.insertEmptyTextNode(vi, list, list.getCurrent(), list.getCurrent().start, list.getCurrent().brailleList.getFirst().start, list.getCurrentIndex(),(String) m.getValue("elementName"));
			
		if(list.size() - 1 != list.getCurrentIndex() - 1){
			if(list.getCurrentIndex() == 0)
				list.shiftOffsetsFromIndex(list.getCurrentIndex() + 1, 1, 1);
			else
				list.shiftOffsetsFromIndex(list.getCurrentIndex(), 1, 1);
		}
		int index = tree.getSelectionIndex();
		
		m.put("length", 1);
		m.put("newBrailleLength", 1);
		m.put("brailleLength", 0);

		braille.insertLineBreak(list.getCurrent().brailleList.getFirst().start - 1);
			
		tree.newTreeItem(list.get(list.getCurrentIndex()), index, 0);
	}
	
	private void insertElementAtEnd(Message m){
		doc.insertEmptyTextNode(vi, list, list.getCurrent(), list.getCurrent().end + 1, list.getCurrent().brailleList.getLast().end + 1, list.getCurrentIndex() + 1,(String) m.getValue("elementName"));
		if(list.size() - 1 != list.getCurrentIndex() + 1)
			list.shiftOffsetsFromIndex(list.getCurrentIndex() + 2, 1, 1);
		
		int index = tree.getSelectionIndex();
		
		m.put("length", 1);
		m.put("newBrailleLength", 1);
		m.put("brailleLength", 0);

		braille.insertLineBreak(list.getCurrent().brailleList.getLast().end);
		tree.newTreeItem(list.get(list.getCurrentIndex() + 1), index, 1);
	}
	
	public void resetElement(Event f){
		if(vi.getStartIndex() != f.getFirstSectionIndex())
			list = vi.resetViews(f.getFirstSectionIndex());
		
		Element replacedElement = replaceElement(f);
		updateSemanticEntry(replacedElement, f.getElement());
		
		ArrayList<TextMapElement> elList = constructMapElements(f.getElement());
		setViews(elList, f.getListIndex(), f.getTextOffset(), f.getBrailleOffset());
		
		manager.getTreeView().rebuildTree(f.getTreeIndex());
		manager.dispatch(Message.createSetCurrentMessage(Sender.TREE, list.get(f.getListIndex()).start, false));
		manager.dispatch(Message.createUpdateCursorsMessage(Sender.TREE));
		
		if(!onScreen(f.getTextOffset()))
			setTopIndex(f.getTextOffset());
	}
	
	private ArrayList<TextMapElement> constructMapElements(Element e){
		ArrayList<TextMapElement> elList = new ArrayList<TextMapElement>();
		if(e.getAttributeValue("semantics").contains("pagenum"))
			elList.add(makePageMapElement(e));
		else {
			for(int i = 0; i < e.getChildCount(); i++){
				if(e.getChild(i) instanceof Text)
					elList.add(new TextMapElement(e.getChild(i)));
				else if(e.getChild(i) instanceof Element && ((Element)e.getChild(i)).getLocalName().equals("brl") && !isBoxline(e)){
					for(int j = 0; j < e.getChild(i).getChildCount(); j++){
						if(e.getChild(i).getChild(j) instanceof Text)
							elList.get(elList.size() - 1).brailleList.add(new BrailleMapElement(e.getChild(i).getChild(j)));
					}
				}
				else if(e.getChild(i) instanceof Element && ((Element)e.getChild(i)).getLocalName().equals("brl") && isBoxline(e))
					elList.add(new BrlOnlyMapElement(e.getChild(i), e));
				else if(e.getChild(i) instanceof Element)
					elList.addAll(constructMapElements((Element)e.getChild(i)));
			}
		}
		
		return elList;
	}
	
	private void setViews(ArrayList<TextMapElement> elList, int index, int textOffset, int brailleOffset ){
		Message m = new Message(null);
		int count = elList.size();
		
		if(shouldInsertBlankLine(elList))
			createBlankLine(textOffset, brailleOffset, index);
		
		for(int i = 0; i < count; i++){
			if(i > 0 && (isBlockElement(elList.get(i)) || afterLineBreak(elList.get(i)))){
				createBlankLine(textOffset, brailleOffset, index);
				textOffset++;
				brailleOffset++;
			}
			
			int brailleLength = 0;
			
			manager.getText().resetElement(m, vi, list, index, textOffset, elList.get(i));
			textOffset = elList.get(i).end;
			
			for(int j = 0; j < elList.get(i).brailleList.size(); j++){
				manager.getBraille().resetElement(m, list, list.get(index), elList.get(i).brailleList.get(j), brailleOffset);
				brailleOffset = (Integer)m.getValue("brailleOffset");
				brailleLength += (Integer)m.getValue("brailleLength");
			}
			
			int textLength =list.get(index).end - list.get(index).start;
			
			textLength = (Integer)m.getValue("textLength");
			textOffset = (Integer)m.getValue("textOffset");
			list.shiftOffsetsFromIndex(index + 1, textLength, brailleLength);
			index++;
		}
	}
	
	private boolean hasId(Element e){
		if(e.getAttribute("id") != null)
			return true;
		else
			return false;
	}
	
	private boolean hasSameSemantics(Element e, Element newElement){
		Attribute sem1 = e.getAttribute("semantics");
		Attribute sem2 = newElement.getAttribute("semantics");
		if(sem1.equals(sem2))
			return true;
		else
			return false;
	}
	
	private boolean firstInLineElement(Element e){
		Element parent = (Element)e.getParent();
		if(parent.getAttribute("semantics") != null && parent.getAttributeValue("semantics").contains("style")){
			if(parent.indexOf(e) == 0)
				return true;
		}
		
		return false;
	}
	
	//returns element removed from DOM
	private Element replaceElement(Event f){
		ParentNode parent = f.getParent();
		Element replacedElement = (Element)parent.getChild(f.getParentIndex());
		parent.replaceChild(replacedElement, f.getElement());
		
		return replacedElement;
	}
	
	private void updateSemanticEntry(Element replacedElement, Element elementToInsert){
		if((hasId(replacedElement) && !hasId(elementToInsert) && !hasSameSemantics(replacedElement, elementToInsert)))
			removeSemanticEntry(replacedElement);
		else if(hasId(replacedElement) && hasId(elementToInsert) && !hasSameSemantics(replacedElement,elementToInsert))
			appendSemanticEntry(elementToInsert);
	}
	
	private void removeSemanticEntry(Element e){
		FileUtils fu = new FileUtils();
		SemanticFileHandler sfh = new SemanticFileHandler(manager.getCurrentConfig());
		String file = BBIni.getTempFilesPath() + BBIni.getFileSep() + fu.getFileName(manager.getWorkingPath()) + ".sem";
		String id = e.getAttributeValue("id");
		sfh.removeSemanticEntry(file, id);
	}
	
	private void appendSemanticEntry(Element e){
		FileUtils fu = new FileUtils();
		SemanticFileHandler sfh = new SemanticFileHandler(manager.getCurrentConfig());
		String file = BBIni.getTempFilesPath() + BBIni.getFileSep() + fu.getFileName(manager.getWorkingPath()) + ".sem";
		String id = e.getAttributeValue("id");
		sfh.removeSemanticEntry(file, id);
		String [] tokens = e.getAttributeValue("semantics").split(",");
		sfh.writeEntry(file, tokens[1], e.getLocalName(), id);
	}
	
	private PageMapElement makePageMapElement(Element e){
		Node textNode = doc.findPrintPageNode(e);
		Node brailleNode = doc.findBraillePageNode(e);
		PageMapElement p = new PageMapElement(e, textNode);
		p.setBraillePage(brailleNode);
		return p;
	}
	
	private boolean shouldInsertBlankLine(ArrayList<TextMapElement>elList){
		return elList.get(elList.size() - 1).parentElement().getAttributeValue("semantics").contains("style") 
				|| firstInLineElement(elList.get(0).parentElement()) || elList.get(0) instanceof PageMapElement || elList.get(0) instanceof BrlOnlyMapElement;
	}
	
	private boolean isBlockElement(TextMapElement t){
		if( t instanceof PageMapElement || t instanceof BrlOnlyMapElement)
			return true;
		else {
			if(t.parentElement().getAttributeValue("semantics").contains("style") && t.parentElement().indexOf(t.n) == 0)
				return true;
			else if(firstInLineElement(t.parentElement()) && t.parentElement().indexOf(t.n) == 0)
				return true;
		}
		return false;
	}
	
	//checks for a rare case if a line break element occurs within a block element
	private boolean afterLineBreak(TextMapElement t){
		if(t instanceof PageMapElement || t instanceof BrlOnlyMapElement)
			return false;
		else if(t.parentElement().indexOf(t.n) > 0){
			int index = t.parentElement().indexOf(t.n);
			if(t.parentElement().getChild(index - 1) instanceof Element && ((Element)t.parentElement().getChild(index - 1)).getLocalName().equals("br"))
				return true;
		}
		
		return false;
	}
	
	private void createBlankLine(int textOffset, int brailleOffset, int index){
		manager.getText().insertText(textOffset, "\n");
		manager.getBraille().insertText(brailleOffset, "\n");
		list.shiftOffsetsFromIndex(index, 1, 1);
	}
	
	private boolean isBoxline(Element e){
		Attribute attr = e.getAttribute("semantics");
		if(attr != null){
			if(attr.getValue().contains("boxline") || attr.getValue().contains("topBox") || attr.getValue().contains("bottomBox") || attr.getValue().contains("middlebox") || attr.getValue().contains("fullBox"))
				return true;
		}
		
		return false;
	}
	
	private boolean onScreen(int pos){
		int textPos = manager.getText().view.getLineAtOffset(pos) * manager.getText().view.getLineHeight();
		int viewHeight = manager.getText().view.getClientArea().height;
		if(textPos > viewHeight)
			return false;
		
		return true;
	}
	
	private void setTopIndex(int pos){
		int line = manager.getTextView().getLineAtOffset(pos);
		manager.getTextView().setTopIndex(line);
	}
}