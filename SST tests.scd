

// simple playing
t = SST.new;
t.addItem(SSTItemWrapper(1, {t.clock.beats.postln}));
t.addItem(x = SSTItemWrapper(5, {t.clock.beats.postln}));
t.play
t.play(4)


// move an item after playback
t.play;t.clock.beats.postln;t.moveItem(3, x);



t.play; t.clock.queue
t.inspect

t.clock.beats

t.clock.sched(1, {\foo.postln})


// shifting items
t = SST.new;
t.addItem(y = SSTItemWrapper(1, {t.clock.beats.postln}));
t.addItem(x = SSTItemWrapper(2, {t.clock.beats.postln}));
t.itemAtTime(1);
t.shiftItemsLater(8, y)
t.play;t.clock.beats.postln;


// Text Wrapper
t = SST.new;
t.addItem(y = SSTItemWrapper(1, {t.clock.beats.postln}));
t.addItem(x = SSTTextWrapper(2, "t.clock.beats.postln"));
t.play;t.clock.beats.postln;


// GUI
(
t = SST.new;
t.addItem(w = SSTItemWrapper(0, {t.clock.beats.postln}));
t.addItem(x = SSTItemWrapper(1, {t.clock.beats.postln}));
t.addItem(y = SSTTextWrapper(2, "t.clock.beats.postln"));
t.addItem(z = SSTItemWrapper(12, {t.clock.beats.postln}));
t.createGroup(\xy, [x, y]);

g = SSTGUI(t)
)

t.createGroup(\z, [z]);

t.createGroup(\1, []);
t.createGroup(\2, []);

t.moveItem(5, w);

[w, x, y, z].do({|iw| iw.time.postln})

t.play(3)
t.stop

t.pauseTime;

http://www.colourlovers.com/palette/2066017/False_Security_Bitc
g.groupColoursFromHex_(["E6FDE8", "EEF2FE", "FFFDED", "FFF7F2", "F4EFEF"]);

// future kitchen
g.groupColoursFromHex_(["#D94A0D", "#FC783F", "#FCF346", "#F4F7CB", "#63C0AA"])

// gia
g.groupColoursFromHex_(["#434A80", "#814E71", "#A59E65", "#797663", "#4E6D60"])

// arcoiris
g.groupColoursFromHex_(["#1D8DAF", "#0FCF1B", "#F51005", "#FDFF08", "#9919D9"])

// santal
g.groupColoursFromHex_(["#DADADA", "#F7C090", "#CDEDE8", "#D9C9E8", "#C9DCE8"])

// vintage chair
g.groupColoursFromHex_(["#1BF6DC", "#16DD72", "#A3DD16", "#FA8936", "#F3ED4F"])

// problem with gui opening for file event

//// Buffer
s.boot;
p = Platform.resourceDir +/+ "sounds/a11wlk01.wav";
b = Buffer.read(s, p);
f = SSTEnvelopedBufferWrapper(0.0, b);

(
t = SST.new;
t.addItem(w = SSTItemWrapper(0, {t.clock.beats.postln}));
t.addItem(x = SSTItemWrapper(1, {t.clock.beats.postln}));
t.addItem(y = SSTTextWrapper(2, "t.clock.beats.postln"));
t.addItem(z = SSTItemWrapper(12, {t.clock.beats.postln}));
t.addItem(f);
t.createGroup(\file, [f]);

g = SSTGUI(t)
)


t.asDocument

// read/write

(
t = SST.new;
t.addItem(w = SSTItemWrapper(0, {t.clock.beats.postln}));
t.addItem(x = SSTItemWrapper(1, {t.clock.beats.postln}));
t.addItem(y = SSTTextWrapper(2, "t.clock.beats.postln"));
t.addItem(z = SSTItemWrapper(12, {t.clock.beats.postln}));
t.createGroup(\foo, [y, z]);

g = SSTGUI(t)
)

g.writeSST("\tmp\ssttest.sst");

// now change something

g.readSST("\tmp\ssttest.sst");

SSTGUI.readSST("\tmp\ssttest.sst");


// sections

(
t = SST.new;
t.addItem(w = SSTItemWrapper(0, {t.clock.beats.postln}));
t.addItem(x = SSTItemWrapper(1, {t.clock.beats.postln}));
t.addItem(y = SSTTextWrapper(2, "t.clock.beats.postln"));
t.addItem(z = SSTItemWrapper(12, {t.clock.beats.postln}));
t.createGroup(\foo, [y, z]);

t.addSection(3);
g = SSTGUI(t)
)

t.addSection(10, "fooSect");

// need to fix dependency for add remove, etc. Especially empty SST.