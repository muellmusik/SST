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
	var textView, resetButton, applyButton;
	
	makeViews {
		parent.isNil.if({
			parent = Window(name, Rect(origin.x, origin.y, 600, 500)).front.view;
		});
		parent.findWindow.layout = VLayout(
			textView = TextView().stringColor = Color.black,
			HLayout(nil, 
				[resetButton = Button().states_([["Reset"]]), align:\right], 
				[applyButton = Button().states_([["Apply"]]), align:\right]
			)
		);
		parent.findWindow.bounds = Rect(origin.x, origin.y, 600, 500);
		parent.findWindow.onClose = parent.findWindow.onClose.addFunc({onClose.value});
		textView.font = Font(Font.defaultMonoFace, 12);
		textView.string = wrapper.wrapped.asCompileString;
		resetButton.action = { textView.string = wrapper.wrapped.asCompileString; };
		applyButton.action = { wrapper.wrapped = textView.string.interpret; };
	}
	
	itemFired {
		{
			var fadeTime = 0, fadeDur = 0.3, interval = 0.04;
			var firedEnv = Env([0, 1], [fadeDur], \sine);
			var oldbackColor = textView.background;
			var oldStringColor = textView.stringColor;
			var fadeBackColor;
			fadeBackColor = wrapper.group.notNil.if({ wrapper.group.color }, Color.black);
			while({fadeTime <= fadeDur }, {
				textView.background = fadeBackColor.blend(oldbackColor, firedEnv.at(fadeTime));
				textView.stringColor = Color.white.blend(oldStringColor, firedEnv.at(fadeTime));
				fadeTime = fadeTime + interval;
				interval.wait;
			});
		}.fork(AppClock);	
	}
}

SSTTextWrapperGUI : AbstractSSTWrapperGUI {
	var textView, resetButton, applyButton;
	
	makeViews {
		parent.isNil.if({
			parent = Window(name, Rect(origin.x, origin.y, 600, 500)).front.view;
		});
		parent.findWindow.layout = VLayout(
			textView = TextView().stringColor = Color.black,
			HLayout(nil, 
				[resetButton = Button().states_([["Reset"]]), align:\right], 
				[applyButton = Button().states_([["Apply"]]), align:\right]
			)
		);
		parent.findWindow.bounds = Rect(origin.x, origin.y, 600, 500);
		parent.findWindow.onClose = parent.findWindow.onClose.addFunc({onClose.value});
		textView.font = Font(Font.defaultMonoFace, 12);
		textView.string = wrapper.text;
		resetButton.action = { textView.string = wrapper.text; };
		applyButton.action = { wrapper.text = textView.string; };
	}
	
	itemFired {
		{
			var fadeTime = 0, fadeDur = 0.3, interval = 0.04;
			var firedEnv = Env([0, 1], [fadeDur], \sine);
			var oldbackColor = textView.background;
			var oldStringColor = textView.stringColor;
			var fadeBackColor;
			fadeBackColor = wrapper.group.notNil.if({ wrapper.group.color }, Color.black);
			while({fadeTime <= fadeDur }, {
				textView.background = fadeBackColor.blend(oldbackColor, firedEnv.at(fadeTime));
				textView.stringColor = Color.white.blend(oldStringColor, firedEnv.at(fadeTime));
				fadeTime = fadeTime + interval;
				interval.wait;
			});
		}.fork(AppClock);	
	}
}

SSTEnvelopedBufferWrapperGUI : AbstractSSTWrapperGUI {
	var textView, <envView, resetButton, applyButton, sf, oscFunc;
	
	makeViews {
		parent.isNil.if({
			parent = Window(name, Rect(origin.x, origin.y, 600, 500)).front.view;
		});
		parent.findWindow.layout = VLayout(
			textView = TextView().stringColor = Color.black,
			this.makeEnvView,
			HLayout(nil, 
				[resetButton = Button().states_([["Reset"]]), align:\right], 
				[applyButton = Button().states_([["Apply"]]), align:\right]
			)
		);
		parent.findWindow.bounds = Rect(origin.x, origin.y, 600, 500);
		parent.findWindow.onClose = parent.findWindow.onClose.addFunc({onClose.value});
		textView.font = Font(Font.defaultMonoFace, 12);
		textView.string = wrapper.eventCode;
		resetButton.action = { 
			textView.string = wrapper.eventCode;
			envView.setEnv(wrapper.env);
		};
		applyButton.action = { 
			var vals;
			vals = envView.value;
			wrapper.eventCode = textView.string; 
			wrapper.env = Env(vals[1], (vals[0] * sf.duration).differentiate.copyToEnd(1));
		};
	}
	
	makeEnvView {
		var compView, sfCompView, sfView;
		compView = CompositeView.new(parent);
		compView.layout = StackLayout(
			envView = EnvelopeView(compView).setEnv(wrapper.env),
			sfCompView = CompositeView.new(compView);
		).mode_(\stackAll);
		
		envView.background_(Color.white.alpha_(0));
		envView.keepHorizontalOrder = true;
		// keep first and last nodes at boundaries
		envView.mouseUpAction = {
			var values = envView.value;
			if(values[0].first != 0.0, {
				values[0][0] = 0.0;
				envView.value = values;
				envView.refresh;
			});
			if(values[0].last != 1.0, {
				values[0][values[0].size - 1] = 1.0;
				envView.value = values;
			});
		};
		envView.mouseDownAction = {|view, x, y, modifiers, buttonNumber, clickCount|
			if(clickCount == 2, {
				var values;
				values = view.value.flop;
				values = values.add([(x - 5) / (view.bounds.width - 10), 1.0 - ((y - 5) / (view.bounds.height - 10))]);
				envView.value = values.sort({|a, b| a[0] <= b[0] }).flop;
			});
		};
		
		sfCompView.layout = HLayout(
		sfView = SoundFileView(sfCompView, sfCompView.bounds.insetBy(5, 5)).background_(Color.white)).margins_(10 ! 4);
		wrapper.group.notNil.if({ sfView.waveColors = [wrapper.group.color] });
		sf = SoundFile.new;
		sf.openRead(wrapper.wrapped.path);
		sfView.readFile(sf, 0, sf.numFrames, 64, true);
		sfView.timeCursorOn = true;
		
		oscFunc = OSCFunc({|msg| {sfView.timeCursorPosition = msg[3]}.defer; }, '/tr', argTemplate: [nil, wrapper.id, nil]);
		onClose = onClose.addFunc({oscFunc.free});
		^compView
	}
	
	itemFired {
		{
			var fadeTime = 0, fadeDur = 0.3, interval = 0.04;
			var firedEnv = Env([0, 1], [fadeDur], \sine);
			var oldbackColor = textView.background;
			var oldStringColor = textView.stringColor;
			var fadeBackColor;
			fadeBackColor = wrapper.group.notNil.if({ wrapper.group.color }, Color.black);
			while({fadeTime <= fadeDur }, {
				textView.background = fadeBackColor.blend(oldbackColor, firedEnv.at(fadeTime));
				textView.stringColor = Color.white.blend(oldStringColor, firedEnv.at(fadeTime));
				fadeTime = fadeTime + interval;
				interval.wait;
			});
		}.fork(AppClock);	
	}
}