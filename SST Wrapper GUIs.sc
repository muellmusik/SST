AbstractSSTWrapperGUI {
	var wrapper, <parent, origin, name;
	var onClose;
	
	*new {|wrapper, parent, origin = (0@0), name|
		^super.newCopyArgs(wrapper, parent, origin, name).makeViews;
	}
	
	makeViews { ^this.subclassResponsibility(thisMethod); }
	
	front { parent.findWindow.front; }
	
	onClose_{|func|
		onClose = onClose.addFunc(func);
	}
	
	close { parent.findWindow.close }
	
}

SSTTextWrapperGUI : AbstractSSTWrapperGUI {
	var textView, cancelButton, applyButton;
	
	makeViews {
		parent.isNil.if({
			parent = Window(name, Rect(origin.x, origin.y, 600, 500)).front.view;
		});
		parent.findWindow.layout = VLayout(
			textView = TextView(),
			HLayout(nil, 
				[cancelButton = Button().states_([["Cancel"]]), align:\right], 
				[applyButton = Button().states_([["Apply"]]), align:\right]
			)
		);
		parent.findWindow.bounds = Rect(origin.x, origin.y, 600, 500);
		parent.findWindow.onClose = parent.findWindow.onClose.addFunc({onClose.value});
		textView.string = wrapper.text;
		cancelButton.action = { textView.string = wrapper.text; };
		applyButton.action = { wrapper.text = textView.string; };
	}	
}