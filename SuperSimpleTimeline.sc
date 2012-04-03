// do we want/need names?
// should this auto number/name within a section?
// yes

// items should be able to be members of multiple groups?
// For now no, just show alignment

// use a priorityQueue to store, but sched on the SST's clock. Since we only ever sched the next event, it's no problem. One issue is clearing the clock's queue, as that may clear other scheduled items. So instead we check if the queue is the same object, and ignore otherwise

// might be simpler to just make it uneditable while playing

SuperSimpleTimeline {
	var <clock, <items, queue, clockStart, nextTime, playOffset;
	var <groups, <groupOrder;
	var <timeUpdate = 0.04;
	var <playing = false;
	var <pauseTime = 0;

	*new {|clock| ^super.new.init(clock); }
	
	init {|argclock| 
		clock = argclock ? TempoClock.default; 
		items = SortedList(128, {|a, b| 
			a.time < b.time || (a.time == b.time && { a.group.order < b.group.order }) 
		});
		groupOrder = List.new;
		groups = IdentityDictionary.new;
		this.createGroup('Ungrouped', []);
	}
	
	addItem {|item| 
		items.add(item);
		groups['Ungrouped'].addItem(item);
	}
	
	// we schedule one event at a time, that way we can ignore a scheduled event if its time has changed
	// calling this while already playing will rebuild the queue and jump to the new time
	play {|startTime = 0| // startTime is relative to the event list
		var i = items.lastIndex, item, time, thisQueue;
		if(items.size > 0, {
			playOffset = startTime;
			queue = PriorityQueue.new;
			while({(item = items[i]).notNil and: {(time = item.time) >= startTime}}, {
				queue.put(time, item);
				i = i - 1;
			});
			// need to deal with beats and quant here...
			clockStart = clock.beats;
			thisQueue = queue;
			clock.sched((nextTime = queue.topPriority) - playOffset, {this.nextEvent(thisQueue)});
			if(playing.not, {this.startTimeUpdate });
			playing = true;
		});
	}
	
	pause { pauseTime = this.currentTime; playing = false; queue = nil; }
	
	togglePlay { if(playing, { this.pause }, { this.play(pauseTime) }); }
	
	stop { playing = false; queue = nil; pauseTime = 0; this.changed(\time, 0) }
	
	startTimeUpdate {
		clock.sched(0, { 
			this.changed(\time, this.currentTime);
			if(playing, timeUpdate, nil);
		})
	}
	
	nextEvent {|eventsQueue|
		var lastTime, thisQueue = queue, thisItem;
		// test that the queue hasn't been edited
		// unfortunately, there's no way to remove a single scheduled event from a clock
		if(eventsQueue === queue, {
			thisItem = queue.pop;
			thisItem.value;
			this.changed(\itemFired, thisItem);
			lastTime = nextTime;
			if(queue.notEmpty, { 
				clock.sched((nextTime = queue.topPriority) - lastTime, 
					{this.nextEvent(thisQueue)}); 
			}, {playing = false});
		}, {"queue changed".warn;});
	}

	itemAtTime {|time|
		^items.detect({|item| item.time == time});
	}
	
	removeItem {|item| 
		items.remove(item);
		groups.do({|items| items.remove(item) });
		if(playing, {this.play(clock.beats - clockStart)}); // this will rebuild the queue
	}
	
	moveItem {|time, item|
		item.time = time;
		items.sort;
		if(playing, {this.play(clock.beats - clockStart)}); // this will rebuild the queue
		this.changed(\itemTimes);
	}
	
	shiftItemsLater {|time, firstItem| 
		var shiftAmount, ind, item;
		time = max(time, 0);
		shiftAmount = time - firstItem.time;
		ind = items.indexOf(firstItem);
		for(ind, items.size-1, {|i| item = items[i]; item.time = item.time + shiftAmount });
		items.sort;
		if(playing, {this.play(clock.beats - clockStart)}); // this will rebuild the queue
		this.changed(\itemTimes);
	}
	
	//shiftItemsBefore {|lastItem, time| } // don't think we need this
	
	clear {}
	
	createGroup {|groupName, groupItems| // Symbol, Array;
		var exists;
		exists = groups[groupName];
		if(exists.notNil, { 
			exists.items.do({|item| groups['Ungrouped'].addItem(item) });
			exists.items = groupItems;
		}, {
			groupOrder.add(groupName); // if new add last
		 	groups[groupName] = SSTGroup(groupName, groupItems, groupOrder.indexOf(groupName));
		}); 
		this.changed(\groupAdded, groupName);
	} // very simple for now
	
	orderGroup {|groupName, index|
		groupOrder.remove(groupName);
		groupOrder.clipPut(index, groupName);
		this.resetGroupOrders;
	}
	
	resetGroupOrders {
		groupOrder.do({|key, i| groups[key].order = i; });
	}
	
	removeGroup {|groupName|
		groups[groupName].items.do({|item| groups['Ungrouped'].addItem(item) });
		groups[groupName] = nil;
		groupOrder.remove(groupName);
		this.resetGroupOrders;
		this.changed(\groupRemoved, groupName);
	}
	
	addSection {|sectionName, time| } // is this also a kind of event? Probably yes!
	
	lastEventTime { ^if(items.size > 0, {items.last.time}, {0}); } // more meaningful than duration
	
	currentTime { ^if(playing, {clock.beats - clockStart + playOffset}, { pauseTime }) }
	
	currentTime_ {|time| if(playing, {this.play(time)}, { pauseTime = time }); this.changed(\time, time) }
	
}

SSTGroup {
	var <name, <items, <>order;
	
	*new {|name, items, order| 
		items = SortedList(items.size, {|a, b| a.time <= b.time}).addAll(items);
		^super.newCopyArgs(name, items, order).init;
	}
	
	init { items.do(_.group = this) }
	
	items_ {|newItems|
		items = SortedList(newItems.size, {|a, b| a.time <= b.time}).addAll(newItems);
		this.init
	}
	
	addItem {|newItem| newItem.group = this; items.add(newItem) }
	
	removeItem {|itemToRemove| items.remove(itemToRemove) }
	
	sort { items.sort }
	
}

// could contain text, a soundfile, whatever
SSTItemWrapper {
	var <time;
	var <>wrapped; // the thing that's executed
	var <group;
	var <resources; // a collection of buffers, etc.
	
	*new {|time, wrapped| ^super.newCopyArgs(max(time, 0), wrapped); }
	
	time_ {|newTime| time = max(newTime, 0); group.sort;}
	
	group_ {|newGroup| group !? {|oldGroup| oldGroup.removeItem(this)}; group = newGroup }
	
	gui {|parent, origin, name| ^SSTItemWrapperGUI(this, parent, origin, name) }
	
	// initialisation code for things like buffers and defs
	resourceCode { }
	
	// the code which causes the actual event
	eventCode { }
	
	//execute the event
	value { wrapped.value }
	
}

// text to be evaluated
SSTTextWrapper : SSTItemWrapper {
	var <text;
	
	// text must be properly escaped if you supply a String literal
	*new {|time, text| ^super.new(time, nil).text_(text) }
	
	compileText { wrapped = text.compile }
	
	text_ {|newText| text = newText; this.compileText; }
	
	gui {|parent, origin, name| ^SSTTextWrapperGUI(this, parent, origin, name) }
	
	// initialisation code for things like buffers and defs
	resourceCode { }
	
	// the code which causes the actual event
	eventCode { ^text }
	
	//execute the event
	value { wrapped.value }
	
}

SSTGUI {
	var sst, eventsView, cursorView, window, name, onClose;
	var path, sf, durInv, sfView, scrollView, selectView, backView, timesView;
	var selectedItem, selectedStartX, selectXOffset, timePerPixel, itemRects, visOriginOnSelected, selectedRect;
	var dependees;
	var time, curSSTime, refTime, cursorLoc;
	var zoomSlider, labelFont, labelBounds;
	var inMove = false, groupDragItem, groupDragRect, groupDraggedTo, groupDragStartX, groupDragStartY;
	var firedItems, firedEnv, fadeDur = 0.3;
	var <groupColours, colorStream;
	var <eventGUIs;
	
	*new {|sst, name, origin|
		^super.new.init(sst, name ? "SuperSimpleTimeline").makeWindow(origin ? (200@200));
	}
	
	init {|argSST, argName|
		var hexColours;
		sst = argSST;
		name = argName;
		dependees = [sst.addDependant(this)]; // sst is the time ref
		firedItems = IdentityDictionary.new;
		firedEnv = Env([1, 0], [fadeDur], \sine);
		labelFont = Font( Font.defaultSansFace, 10 ).boldVariant;
		labelBounds = IdentityDictionary.new;
		itemRects = IdentityDictionary.new;
		groupColours = IdentityDictionary.new; 
		/*
			http://www.colourlovers.com/palette/2066112/purplexed#
			Author: Artsplay http://www.colourlovers.com/lover/artsplay/loveNote
		*/
		//hexColours = ["#DD9D25", "#C9C748", "#8B1A92", "#DDB137", "#D8D649"];
		/*
			http://www.colourlovers.com/palette/2066046/MH_Palette
			Author: SpotlightNC http://www.colourlovers.com/lover/SpotlightNC/loveNote
		*/
		hexColours = ["#FF7A17", "#FF1C59", "#FFC424", "#A132CF", "#13945F"];
		colorStream = Pseq(hexColours.collect({|hex|
			Color.fromHexString(hex);
		}), inf).asStream;
		sst.groupOrder.do({|grpname|
			groupColours[grpname] = colorStream.next;
		});
		eventGUIs = IdentityDictionary.new;
	}
	
	groupColours_ { |coloursArray|
		colorStream = Pseq(coloursArray, inf).asStream;
		sst.groupOrder.do({|grpname|
			groupColours[grpname] = colorStream.next;
		});
		eventsView.refresh;
	}
	
	groupColoursFromHex_ {|hexArray|
		colorStream = Pseq(hexArray.collect({|hex| Color.fromHexString(hex);}), inf).asStream;
		sst.groupOrder.do({|grpname|
			groupColours[grpname] = colorStream.next;
		});
		eventsView.refresh;
	}
	
	makeWindow { |origin|
		var width = 1008;
		
		durInv = sst.lastEventTime.reciprocal;
		
		window = Window.new(name, Rect(origin.x, origin.y, width, 400), false);
		window.view.decorator = FlowLayout(window.view.bounds);
		
		window.view.keyDownAction = { arg view,char,modifiers,unicode,keycode;
			if(unicode == 32, {sst.togglePlay});
			if(unicode == 13, {sst.stop});
		};
		
		scrollView = ScrollView(window, Rect(0, 0, width - 8, 334));
		scrollView.hasBorder = true;
		scrollView.resize = 2;
		scrollView.background = Color.clear;
		scrollView.hasVerticalScroller = false;
		scrollView.canFocus_(false);
		
		backView = CompositeView(scrollView, Rect(0, 20,  width - 10, 294)).background_(Color.clear);
		
		this.makeTimesView;
		
		window.onClose = {
			dependees.do({|dee| dee.removeDependant(this)});
			onClose.value(this);	
		};
		
		window.view.decorator.shift(0, 5);
		StaticText(window, Rect(0, 0, 5, 10)).string_("-").font_(Font("Helvetica-Bold", 12));
		zoomSlider = Slider(window, Rect(0, 5, 100, 10)).action_({|view| 
			var pixelsPerSecond, newWidth;
			// pixels per second from whole sequence to 1 second
			pixelsPerSecond = [(scrollView.bounds.width - 4) * durInv, (scrollView.bounds.width - 4), \cos].asSpec.map(view.value);
			newWidth = (sst.lastEventTime * pixelsPerSecond).round;
			backView.bounds = backView.bounds.width_(newWidth);
			eventsView.bounds = eventsView.bounds.width_(newWidth);
			cursorView.bounds = cursorView.bounds.width_(newWidth);
			timesView.bounds = timesView.bounds.width_(newWidth);
			timePerPixel = sst.lastEventTime / newWidth;
		}).canFocus_(false).enabled_(true);
		StaticText(window, Rect(0, 0, 10, 10)).string_("+").font_(Font("Helvetica-Bold", 10));
		window.view.decorator.shift(0, -5);

		window.front;
		
		this.makeEventsView;
		
		timePerPixel = sst.lastEventTime / eventsView.bounds.width;
		
		window.view.decorator.nextLine.nextLine;
		window.view.decorator.shift(10, 0);
		refTime = StaticText(window, Rect(0, 0, 200, 25))
			.string_("Source Time:") // initialise
			.font_(Font("Helvetica-Bold", 16));
		//time = BMTimeReferences.currentTime(ca.timeReference);
		time = sst.currentTime;
		cursorLoc = time * durInv * cursorView.bounds.width;
		
		refTime.string_("Source Time:" + time.getTimeString);
					
		window.view.decorator.shift(0, 4);
	//	curSSTime = SCStaticText(window, Rect(0, 0, 300, 20))
//			.string_("Selected Snapshot Time:") // initialise
//			.font_(Font("Helvetica-Bold", 12));
//		
//		if(activeSnapshot.notNil, {
//			curSSTime.string_("Selected Snapshot Time:" + activeSnapshot.time.asTimeString);
//		});

		//zoomSlider.doAction; // hack to make eventsView take mouseDown initially
	}
	
	makeTimesView {
		
		timesView.notNil.if({timesView.remove});
		timesView = UserView(scrollView, Rect(0, 0, backView.bounds.width, 20));
		timesView.background = Color.clear;
		timesView.canFocus_(false);
		
		timesView.drawFunc = {
			var oneSec, tenSecs, thirtySecs, bounds;
			bounds = timesView.bounds;
			Pen.addRect(Rect(0, 0, bounds.width, 20));
			Pen.fillColor = Color.black.alpha_(0.6);
			Pen.fill;

			tenSecs = timesView.bounds.width * durInv * 10;
			Pen.beginPath;
			Pen.strokeColor = Color.grey(0.8);
			(sst.lastEventTime / 10).floor.do({|i|
				var x;
				if((i + 1)%3 == 0, {
					Pen.width = 2;
					Pen.lineDash_(FloatArray[1.0, 0]);
				}, {
					Pen.width = 1;
					Pen.lineDash_(FloatArray[3,3]);
				});
				x = (i + 1) * tenSecs;
				Pen.line(x@20, x@0);
				Pen.stroke;
			});
			Pen.width = 1;
			Pen.lineDash_(FloatArray[]);
			thirtySecs = bounds.width * durInv * 30;
			(sst.lastEventTime / 10).floor.do({|i|
				((i + 1) * 10).asTimeString(1).drawLeftJustIn(
					Rect((i+1) * tenSecs + 2, 0, 70, 20),
					Font("Helvetica-Bold", 11), 
					Color.grey(0.8)
				); 
			});
			if(thirtySecs >= scrollView.bounds.width, {
				oneSec = timesView.bounds.width * durInv;
				Pen.strokeColor = Color.grey(0.8);
				sst.lastEventTime.floor.do({|i|
				var x;
				if(i%10 != 0, {
					Pen.lineDash_(FloatArray[1.0, 0.0]);
					x = i * oneSec;
					Pen.line(x@20, x@10);
					Pen.stroke;
				});
			});
			});
			Pen.strokeColor = Color.black;
			Pen.lineDash_(FloatArray[3,3]);
			Pen.line(0@20, bounds.width@20);
			Pen.stroke;
			Pen.lineDash_(FloatArray[1.0, 0.0]);
		};
		
		timesView.mouseDownAction = {|view, x, y| sst.currentTime = x * timePerPixel; };
		timesView.mouseMoveAction = timesView.mouseDownAction;
	}
	
	makeEventsView {
		cursorView.notNil.if({cursorView.remove});
		eventsView.notNil.if({eventsView.remove});
		
		scrollView.action = { eventsView.refresh }; // for now. Could but labels in a separate lower view
		
		cursorView = UserView(backView, Rect(0, 0, backView.bounds.width, backView.bounds.height))
			.canFocus_(false)
			.background_(Color.clear);
			
		eventsView = UserView(backView, Rect(0, 0, backView.bounds.width, backView.bounds.height))
			.canFocus_(false)
			.background_(Color.clear);
			
		cursorView.drawFunc = {
			// draw grid
			DrawGrid(eventsView.bounds, [0, sst.lastEventTime, 'lin', 1.0].asSpec.grid, BlankGridLines()).draw;
			
			// draw time cursor
			Pen.strokeColor = Color.grey;
			Pen.width = 2;
			Pen.line(cursorLoc@0, cursorLoc@eventsView.bounds.height);
			Pen.stroke;
			
			// draw fired rings
			firedItems.copy.keysValuesDo({|firedItem, itemSpecs|
				var x, rect, alpha;
				x = durInv * firedItem.time * eventsView.bounds.width;
				rect = Rect.aboutPoint(x@itemSpecs.groupY, 14, 14);
				alpha = itemSpecs.alpha;
				if(alpha == 0, { firedItems[firedItem] = nil; }); // remove if done
				Pen.fillColor = Color.white.alpha_(alpha);
				//Pen.width = 3;
				Pen.fillOval(rect);
				//Pen.strokeOval(rect);
			});
				
		};
			
		eventsView.drawFunc = {
			var stringX = scrollView.visibleOrigin.x + 4;
			groupDraggedTo = nil;
			Pen.strokeColor = Color.black;
			//Pen.fillColor = Color.grey;
			sst.groupOrder.do({|name, i|
				var groupY, labelPoint, label, thisLabelBounds;
				
				groupY = ((i * 40) + 30);
				
				// draw labels
				label = (i + 1).asString ++ ". " ++ name.asString;
				labelPoint = stringX@(i * 40 + 4);
				labelBounds[name] = thisLabelBounds = GUI.current.stringBounds(label, labelFont).moveToPoint(labelPoint);
				if(groupDragItem.notNil && { thisLabelBounds.intersects(groupDragRect) }, {
					Pen.fillColor = Color.white.alpha_(0.6);
					Pen.fillRect(thisLabelBounds.resizeBy(4, 4).moveBy(-2, -2));
					groupDraggedTo = name;
				});
				Pen.stringAtPoint(label, stringX@(i * 40 + 4), labelFont, Color.grey(0.3));
				
				// draw lines
				if(name != 'Ungrouped', {
					Pen.lineDash = FloatArray[4.0, 2.0];
					sst.groups[name].items.doAdjacentPairs({|a, b|
						var x1, x2;
						x1 = durInv * a.time * eventsView.bounds.width;
						x2 = durInv * b.time * eventsView.bounds.width;
						Pen.width = 0.5;
						Pen.line(x1@groupY, x2@groupY);
						Pen.stroke;
					});
					Pen.lineDash = FloatArray[1.0, 0];
				});
				
				// draw events
				Pen.fillColor = groupColours[name];
				sst.groups[name].items.reverseDo({|item| // draw earlier items on top
					var x, rect;
					x = durInv * item.time * eventsView.bounds.width;
					rect = Rect.aboutPoint(x@groupY, 10, 10);
					Pen.width = 1;
					Pen.fillOval(rect);
					Pen.strokeOval(rect);
					itemRects[item] = rect;
				});
			});
			// draw group drag if needed
			if(groupDragItem.notNil, {
				Pen.width = 1;
				Pen.strokeColor = Color.grey;
				Pen.lineDash = FloatArray[3.0, 1.0];
				Pen.strokeOval(groupDragRect);
				Pen.lineDash = FloatArray[1.0, 0.0];
			});
		};

		eventsView.mouseMoveAction = {|view, x, y, modifiers|
			var time;
			var visRange, newX, lastX, maxX;
			if(selectedItem.notNil, {
				if(groupDragItem.notNil, {	// drag item to a group
					groupDragRect = selectedRect.moveBy(x - groupDragStartX, y - groupDragStartY);
					eventsView.refresh;
				}, {	// vanilla move or shift
					inMove = true;
					// get some info
					// moving can cause scrolling, so calc distance of x from vis origin
					lastX = sst.lastEventTime * durInv * eventsView.bounds.width;
					visRange = Range(scrollView.visibleOrigin.x, scrollView.bounds.width);
					newX = (x - visRange.start - selectXOffset) + selectedStartX; // could be more than lastX
					time = newX * timePerPixel;
					
					// move or shift
					if(modifiers.isShift, {
						sst.shiftItemsLater(time, selectedItem);
					}, {
						sst.moveItem(time, selectedItem);
					});
					
					// now check if we can see newX and scroll if needed
					if(visRange.start > newX, {
						scrollView.visibleOrigin = newX@scrollView.visibleOrigin.y;
					});
					if(visRange.end < newX, {
						scrollView.visibleOrigin = (newX - scrollView.bounds.width + 25)@scrollView.visibleOrigin.y;
					});
					
					if(modifiers.isShift, { timesView.refresh; cursorView.refresh; });
				});
			});
		};
		
		eventsView.mouseUpAction = {|view|
			inMove = false; 
			if(groupDragItem.notNil, {
				if(groupDraggedTo.notNil, { sst.groups[groupDraggedTo].addItem(groupDragItem); });
				groupDragItem = nil;
				groupDragRect = nil;
				eventsView.refresh;
			});
		};

		eventsView.mouseDownAction = {|view, x, y, modifiers, buttonNumber, clickCount|
			// find selected event
			selectedRect = nil;
			selectedItem = nil;
			itemRects.keysValuesDo({|item, rect|
				if(rect.contains((x@y)), {selectedRect = rect; selectedItem = item;})
			});
			
			// single (maybe drag) or double (open event gui) click
			if(clickCount < 2, {
				selectedRect.notNil.if({
					visOriginOnSelected = scrollView.visibleOrigin;
					selectXOffset = x - visOriginOnSelected.x;
					selectedStartX = durInv * selectedItem.time * eventsView.bounds.width;
					if(modifiers.isCtrl, {
						groupDragStartX = x; 
						groupDragStartY = y;
						groupDragItem = selectedItem;
						groupDragRect = selectedRect;
					});
				});
			}, {
				selectedItem.notNil.if({
					var thisGUI;
					thisGUI = eventGUIs[selectedItem]; 
					if(thisGUI.notNil, { thisGUI.front }, { 
						eventGUIs[selectedItem] = thisGUI = selectedItem.gui;
						thisGUI.notNil.if({
							sst.addDependant(thisGUI);
							thisGUI.onClose = { 
								eventGUIs[selectedItem] = nil;
								sst.removeDependant(thisGUI);
							};
						});
					});
				});
			});
		};
	}
	
	recalcZoom {
		// don't fire action, just get it in the right place
		zoomSlider.value = [(scrollView.bounds.width - 4) * durInv, (scrollView.bounds.width - 4), \cos]
			.asSpec
			.unmap(timePerPixel.reciprocal);
	}
	
	resizeInternalViewsIfNeeded {
		var lastX;
		lastX = sst.lastEventTime * durInv * eventsView.bounds.width;
		// now check if we need to extend and recalc durInv
		// if we comment this out we get a zooming behaviour with no jumps
		if(lastX != eventsView.bounds.width, {
			lastX = max(scrollView.bounds.width - 4, lastX);
			backView.bounds = backView.bounds.width_(lastX);
			eventsView.bounds = eventsView.bounds.width_(lastX);
			cursorView.bounds = eventsView.bounds.width_(lastX);
			backView.bounds = backView.bounds.width_(lastX); 
			timesView.bounds = timesView.bounds.width_(lastX);
			durInv =  sst.lastEventTime.reciprocal;
			timePerPixel = sst.lastEventTime / eventsView.bounds.width;
			timesView.refresh;
			cursorView.refresh;
			this.recalcZoom;
		});	
	}
		


	
	update { arg changed, what ...args;
				
		switch(what,
			\itemTimes, { 
				this.resizeInternalViewsIfNeeded;
				{eventsView.refresh; cursorView.refresh;}.defer;
			},
			
			\time, {
				{
					time = args[0];
					refTime.string_("Source Time:" + time.asTimeString);
					cursorLoc = time * durInv * eventsView.bounds.width;
					// scroll to see cursor
//					if(args[1] != 0, { // not paused or stopped
						if(cursorLoc > (scrollView.visibleOrigin.x + 
								scrollView.bounds.width - 2), {
							scrollView.visibleOrigin = cursorLoc@0;
						}, {
							if(cursorLoc < scrollView.visibleOrigin.x, {
								scrollView.visibleOrigin = 
									(cursorLoc - scrollView.bounds.width - 2)@0;
							});
						});
					//});
					cursorView.refresh;
				}.defer;
			},
			
			\itemFired, {
				var itemFired, firedTime, interval;
				itemFired = args[0];
				firedTime = itemFired.time;
				interval = sst.timeUpdate;
				firedItems[itemFired] = (alpha: 1.0, groupY: (sst.groupOrder.indexOf(itemFired.group.name) * 40) + 30);
				{
					var fadeTime = 0;
					while({fadeTime <= fadeDur }, {
						firedItems[itemFired].alpha = firedEnv.at(fadeTime);
						sst.playing.not.if({cursorView.refresh}); // could have finished
						fadeTime = fadeTime + interval;
						interval.wait;
					});
					firedItems[itemFired] = nil;
				}.fork(AppClock);
			},
			
			\groupAdded, {
				groupColours[args[0]] = colorStream.next;
				eventsView.refresh;
			},
			
			\groupRemoved, {
				groupColours[args[0]] = nil;
			}
			
		);
		
//			\stop, {
//				{sfView.timeCursorPosition = 0;}.defer;
//			},
//			\base, {
//				path = ca.timeReference.path; // How best to do this?
//				path.notNil.if({
//					{
//					zoomSlider.value = 0;
//					sf.close;
//					sf.openRead(path);
//					durInv = sf.duration.reciprocal;
//					sfView.soundfile = sf;
//					//sfView.read(block: 256);
//					this.setWaveColors;
//					sfView.refresh;
//					this.makeTimesView;
//					sfView.read(block: 256);
//					ca.sequences.do({|sq, i| sequenceLevels[sq] = (0.1 * (i + 1))%1.0});
//					this.makeeventsView;
//					zoomSlider.enabled = true;
//					//zoomSlider.valueAction = 0; 
//					zoomSlider.doAction;
//					}.defer;
//				}, {zoomSlider.value = 0; zoomSlider.enabled = false;});
//				
//			}
//
//		)
//	
	}
}

	