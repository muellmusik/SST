// do we want/need names?
// should this auto number/name within a section?
// yes

// items should be able to be members of multiple groups?
// For now no, just show alignment

// use a priorityQueue to store, but sched on the SST's clock. Since we only ever sched the next event, it's no problem. One issue is clearing the clock's queue, as that may clear other scheduled items. So instead we check if the queue is the same object, and ignore otherwise

// might be simpler to just make it uneditable while playing

SuperSimpleTimeLine {
	var <clock, items, queue, clockStart, nextTime, playOffset;
	var playing = false;

	*new {|clock| ^super.new.init(clock); }
	
	init {|argclock| 
		clock = argclock ? TempoClock.default; 
		items = SortedList(128, {|a, b| a.time < b.time });
	}
	
	addItem {|item| 
		items.add(item);	
	}
	
	// we schedule one event at a time, that way we can ignore a scheduled event if its time has changed
	// calling this while already playing will rebuild the cue and jump to the new time
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
			playing = true;
		});
	}
	
	nextEvent {|eventsQueue|
		var lastTime, thisQueue = queue;
		// test that the queue hasn't been edited
		// unfortunately, there's no way to remove a single scheduled event from a clock
		if(eventsQueue === queue, {
			queue.pop.value;		
			lastTime = nextTime;
			if(queue.notEmpty, { 
				clock.sched((nextTime = queue.topPriority) - lastTime, 
					{this.nextEvent(thisQueue)}); 
			}, {playing = false});
		}, {"queue changed".warn;});
	}
			
	
	removeItem {|item| 
		items.remove(item);
		if(playing, {this.play(clock.beats - clockStart)}); // this will rebuild the queue
	}
	
	moveItem {|time, item|
		item.time = time;
		items.sort;
		if(playing, {this.play(clock.beats - clockStart)}); // this will rebuild the queue
	}
	
	shiftItemsLater {|firstItem, time| 
		var shiftAmount, ind;
		time = max(time, 0);
		shiftAmount = time - firstItem.time;
		ind = items.indexOf(firstItem);
		for(ind, items.size-1, {item.time = item.time + shiftAmount });
		items.sort;
		if(playing, {this.play(clock.beats - clockStart)}); // this will rebuild the queue
	}
	
	//shiftItemsBefore {|lastItem, time| } // don't think we need this
	
	clear {}
	
	createGroup {|groupName, names| } // names is optional initial setup
	
	addSection {|sectionName, time| } // is this also a kind of event? Probably yes!
	
}

// could contain text, a soundfile, whatever
SSTItemWrapper {
	var <>time;
	var <>wrapped; // the thing that's executed
	var <resources; // a collection of buffers, etc.
	
	*new {|time, wrapped| ^super.newCopyArgs(time, wrapped); }
	
	// a (probably) editable view which can pop up on the timeline
	gui { }
	
	// initialisation code for things like buffers and defs
	resourceCode { }
	
	// the code which causes the actual event
	eventCode { }
	
	//execute the event
	value { wrapped.value }
	
}
	