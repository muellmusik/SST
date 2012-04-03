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

// basic version just posts item asCompileString; may not be editable
SSTItemWrapperGUI : AbstractSSTWrapperGUI {
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
		textView.font = Font(Font.defaultMonoFace, 12);
		textView.string = wrapper.wrapped.asCompileString;
		cancelButton.action = { textView.string = wrapper.wrapped.asCompileString; };
		applyButton.action = { wrapper.wrapped = textView.string.interpret; };
	}
	
	itemFired {
		{
			var fadeTime = 0, fadeDur = 0.3, interval = 0.04;
			var firedEnv = Env([0, 1], [fadeDur], \sine);
			var oldbackColor = textView.background;
			var oldStringColor = textView.stringColor;
			while({fadeTime <= fadeDur }, {
				textView.background = Color.black.blend(oldbackColor, firedEnv.at(fadeTime));
				textView.stringColor = Color.white.blend(oldStringColor, firedEnv.at(fadeTime));
				fadeTime = fadeTime + interval;
				interval.wait;
			});
		}.fork(AppClock);	
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
		textView.font = Font(Font.defaultMonoFace, 12);
		textView.string = wrapper.text;
		cancelButton.action = { textView.string = wrapper.text; };
		applyButton.action = { wrapper.text = textView.string; };
	}
	
	itemFired {
		{
			var fadeTime = 0, fadeDur = 0.3, interval = 0.04;
			var firedEnv = Env([0, 1], [fadeDur], \sine);
			var oldbackColor = textView.background;
			var oldStringColor = textView.stringColor;
			while({fadeTime <= fadeDur }, {
				textView.background = Color.black.blend(oldbackColor, firedEnv.at(fadeTime));
				textView.stringColor = Color.white.blend(oldStringColor, firedEnv.at(fadeTime));
				fadeTime = fadeTime + interval;
				interval.wait;
			});
		}.fork(AppClock);	
	}
}