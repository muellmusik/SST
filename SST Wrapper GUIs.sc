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
	
	itemFired { ^this.subclassResponsibility(thisMethod); } // visual indication
	
	update { arg changed, what ...args;
		switch(what,
			\itemFired, {
				if(args[0] == wrapper, { this.itemFired });
			}
		); 
	}
	
}

SSTTextWrapperGUI : AbstractSSTWrapperGUI {
	var textView, cancelButton, applyButton;
	
	makeViews {
		parent.isNil.if({
			parent = Window(name, Rect(origin.x, origin.y, 600, 500)).front.view;
		});
		parent.findWindow.layout = VLayout(
			textView = TextView().stringColor = Color.black,
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
	
	itemFired {
		{
			var fadeTime = 0, fadeDur = 0.3, interval = 0.04;
			var firedEnv = Env([0, 1], [fadeDur], \sine);
			while({fadeTime <= fadeDur }, {
				textView.background = textView.background.alpha_(firedEnv.at(fadeTime));
				textView.stringColor =textView.stringColor.alpha_(firedEnv.at(fadeTime));
				fadeTime = fadeTime + interval;
				interval.wait;
			});
		}.fork(AppClock);	
	}
}