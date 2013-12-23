package org.brailleblaster.perspectives.braille.stylepanel;

import org.brailleblaster.localization.LocaleHandler;
import org.brailleblaster.perspectives.braille.document.BBSemanticsTable.Styles;
import org.brailleblaster.perspectives.braille.document.BBSemanticsTable.StylesType;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

public class EditPanel {
	static LocaleHandler lh = new LocaleHandler();
	
	private final static int LEFT_MARGIN = 0;
	private final static int RIGHT_MARGIN = 15;
	private final static int TOP_MARGIN = 50;
	private final static int BOTTOM_MARGIN = 100;
	
	//For use in making localized UI
	final private String [] emphasisList = {lh.localValue("bold"), lh.localValue("italic"), lh.localValue("underline")};
	//For determining combo box position in relation to liblouis value
	protected static final int BOLD = 0;
	protected static final int ITALIC = 1;
	protected static final int UNDERLINE = 2;
			
	final private String [] alignmentList = {lh.localValue("left"), lh.localValue("center"), lh.localValue("right")};
	protected static final int LEFT = 0;
	protected static final int CENTER = 1;
	protected static final int RIGHT = 2;
		
	protected Styles originalStyle;
	protected Group group;
	protected StyleManager sm;
	protected Text styleName;

	private Label styleLabel, linesBeforeLabel, linesAfterLabel, marginLabel, indentLabel, alignmentLabel, emphasisLabel;
	protected Combo alignmentCombo, emphasisCombo;
	protected Spinner linesBeforeSpinner, linesAfterSpinner, marginSpinner, indentSpinner;
	
	public EditPanel(StyleManager sm, Group documentWindow, Styles style){
		this.sm = sm;
		originalStyle = style;
		this.group = new Group(documentWindow, SWT.FILL | SWT.BORDER);
		this.group.setText(lh.localValue("editStyle"));
		setLayoutData(this.group, LEFT_MARGIN, RIGHT_MARGIN, TOP_MARGIN, BOTTOM_MARGIN);
		this.group.setLayout(new FormLayout());   	
		
		styleLabel = makeLabel(lh.localValue("element"), 0, 50, 0, 10);
		styleName = new Text(group, SWT.BORDER);
		setLayoutData(styleName, 50, 100, 0, 10);
		
		linesBeforeLabel = makeLabel(lh.localValue("linesBefore"), 0, 50, 10, 20);
		linesBeforeSpinner = makeSpinner(50, 100, 10, 20);
		linesBeforeSpinner.setMinimum(0);
		
		linesAfterLabel = makeLabel(lh.localValue("linesAfter"), 0, 50, 20, 30);
		linesAfterSpinner = makeSpinner(50, 100, 20, 30);
		linesAfterSpinner.setMinimum(0);
		
		
		marginLabel = makeLabel(lh.localValue("margin"), 0, 50, 30, 40);
		marginSpinner = makeSpinner(50, 100, 30, 40);
		marginSpinner.setMinimum(0);
		
		indentLabel = makeLabel(lh.localValue("indent"), 0, 50, 40, 50);
		indentSpinner = makeSpinner(50, 100, 40, 50);
		indentSpinner.setMinimum(-100);
		
		alignmentLabel = makeLabel(lh.localValue("alignment"), 0, 50, 50, 60);
		alignmentCombo = makeCombo(alignmentList, 50, 100, 50, 60);
		
		emphasisLabel = makeLabel(lh.localValue("emphasis"), 0, 50, 60, 70);
		emphasisCombo = makeCombo(emphasisList, 50, 100, 60, 70);
	}
	
	protected void setLayoutData(Control c, int left, int right, int top, int bottom){
		FormData location = new FormData();
		
		location.left = new FormAttachment(left);
		location.right = new FormAttachment(right);
		location.top = new FormAttachment(top);
		location.bottom = new FormAttachment(bottom);
		
		c.setLayoutData(location);
	}
	
	protected void resetLayout(){
		group.pack();
		group.getParent().layout();
	}
	
	private Label makeLabel(String text, int left, int right, int top, int bottom){
		Label l = new Label(group, SWT.BORDER | SWT.CENTER);
		l.setText(text);
		setLayoutData(l, left, right, top, bottom);
		
		return l;
	}
	
	private Spinner makeSpinner(int left, int right, int top, int bottom){
		Spinner sp = new Spinner(group, SWT.BORDER);
		setLayoutData(sp, left, right, top, bottom);
		return sp;
	}
	
	protected void setSpinnerData(Spinner sp, Styles style, StylesType type){
		if(style.contains(type))
			sp.setSelection(Integer.valueOf((String)style.get(type)));
		else
			sp.setSelection(0);
	}
	
	private Combo makeCombo(String [] values, int left, int right, int top, int bottom){
		Combo cb = new Combo(group, SWT.BORDER);
		cb.setItems(values);
		setLayoutData(cb, left, right, top, bottom);
		
		return cb;
	}
	
	protected Group getGroup(){
        return group;
    }
	
	protected void dispose(){
		group.dispose();
	}
	
	protected boolean isVisible(){
        if(!group.isDisposed() && group.isVisible())
            return true;
        else
            return false;
    }
}
