Trace {
    classvar traceBuses=256, windowSize=0.05, traceMap;
    classvar traceStartFunc, buses, busAllocator;
    classvar <>showAfterFree=10;
    
    *initClass {
        var globalTraceView;
        
        ServerBoot.add(this);
        busAllocator    = StackNumberAllocator(0, traceBuses);
        traceMap        = nil ! traceBuses;
        
        CmdPeriod.add({
            try {
                this.clear();
            } { |e| e.reportError() }
        });
        
        ServerSelectorWidget.actionsCallback = ServerSelectorWidget.actionsCallback.addFunc {
            [
                MenuAction("Trace view", { 
                    if (globalTraceView !? { globalTraceView.isClosed } ?? { true }) {
                        globalTraceView = TraceView.gui();
                    };
                    
                    globalTraceView.front; 
                }).shortcut_('t')
            ]
        }
    }
    
    *doOnServerBoot {
        |server|
        buses = traceBuses.collect { Bus.control(server, 3) };
        
        traceStartFunc = OSCFunc({
            |msg|
            var synthDef, label, node;
            
            node = msg[1].asInteger;
            
            msg = msg[0].asString.split($/);
            synthDef = msg[3].asSymbol;
            node = NodeWatcher.all[server.name].nodes[node] ?? { Synth.basicNew(synthDef, server, node) };
            label = msg[4].asSymbol;
            
            this.onTraceStart(server, synthDef, node, label);
        }, "/trace/start/*", dispatcher:OSCMessageReversePatternDispatcher.instance).permanent_(true);
    }
    
    *doOnServerQuit {
        buses.do(_.free);
        buses = nil;
        traceStartFunc.free
    }
    
    *onTraceStart {
        |server, synthDef, node, label|
        var index = busAllocator.alloc;
        
        traceMap[index] = [synthDef, node, label];
        
        // "tracing %/%/% to bus index %".format(synthDef, node.nodeID, label, index).postln;
        server.sendMsg('/n_set', node.nodeID, this.fixControlName("traceOut_%".format(label)), buses[index].index);
        
        NodeWatcher.register(node, true);
        node.onFree {
            // "tracing ended for %/%".format(node.nodeID, label).postln;
            busAllocator.free(index);
            traceMap[index] = nil;
            
            this.changed(\trace_removed, synthDef, node, label);
        };
        
        this.changed(\trace_added, synthDef, node, label, {
            this.at(index);
        });
    }
    
    *fixControlName {
        |name|
        ^name
            .replace("]", "")
            .replace("[", "_");
    }
    
    *trace {
        |ugen, label, trigger=false|
        var name, traceVal, reset, rate;
        
        if (ugen.isArray) {
            ^ugen.collect {
                |u, i|
                this.trace(u, label, trigger)
            }
        };
        
        rate = ugen.rate.switch(\audio, \ar, \control, \kr);
        
        label = label 
            !? { "%[%]".format(label, ugen.synthIndex) } 
            ?? { "%[%]".format(ugen.class.name, ugen.synthIndex) };
        
        reset = this.fixControlName("traceReset_%".format(label)).asSymbol.tr(0);
        
        SendReply.kr(Impulse.kr(0), "/trace/start/%/%".format(ugen.synthDef.name, label));
        
        if (trigger) {
            ugen = switch(
                rate, 
                \ar, {
                    Latch.ar(ugen, ugen > 0)        
                },
                \kr, {
                    Latch.kr(ugen, ugen > 0)
                }
            )
        };
        
        Out.kr(
            this.fixControlName("traceOut_%".format(label)).asSymbol.kr(-3), 
            [
                RunningMin.perform(rate, ugen, reset),
                ugen,
                RunningMax.perform(rate, ugen, reset), 
            ]
        );
        
        ^ugen;
    }
    
    *clear {
        
        traceMap.fill(nil);
    }
    
    *at {
        |index|
        ^buses[index].getnSynchronous(3);
    }
    
    *do {
        |function|
        traceMap.do {
            |details, index|
            if (details.notNil) {
                function.(details[0], details[1], details[2], {
                    this.at(index)
                })
            }
        }
    }
}

PatternTrace {
    classvar traceValues,
        <>clearTime = 5;
    
    *onTraceStart {
        |label|
        traceValues = traceValues ?? { () };
        label = this.fixLabel(label);
        this.changed(\patterntrace_added, label, {
            this.at(label);
        })
    }
    
    *fixLabel {
        |label|
        ^label.asSymbol
    }
    
    *at {
        |label|
        label = this.fixLabel(label);
        ^traceValues[label];
    }
    
    *put {
        |label, value|
        label = this.fixLabel(label);
        traceValues[label] = value;
        this.changed(\patterntrace_changed, label, value);
    }
    
    *clear {
        traceValues.keys.do {
            |label|
            this.traceDone(label)
        };
        
        traceValues.clear;
    }
    
    *traceDone {
        |label|
        label = this.fixLabel(label);
        "Ending trace for %".format(label).postln;
        this.changed(\patterntrace_removed, label)
    }
    
    *tracePattern {
        |pattern, label, func|
        
        ^Prout({
            |inval|
            var stream = pattern.asStream;
            var displayVal, cleanup, cleanupFunc;
            
            PatternTrace.onTraceStart(label);
            
            cleanupFunc = Thunk {
                PatternTrace.traceDone(label)
            };
            
            if (inval.isKindOf(Event)) {
                cleanup = EventStreamCleanup();
                cleanup.addFunction(inval, cleanupFunc)
            };
            
            while { 
                if (inval.isKindOf(Event)) {
                    cleanup.update(inval);
                };
                inval = stream.next(inval);
                inval.notNil 
            } {
                displayVal = inval;
                func !? {
                    displayVal = func.value(displayVal)
                };
                PatternTrace.put(label, displayVal);
                inval = inval.yield;
            };
            
            ^cleanup.exit;
        });
    }
}

PatternTraceView : TreeView {
    classvar <hiddenMap;
    var <order, <itemsMap, <lastValues, <minValues, <maxValues, <graphs;
    
    *initClass {
        Class.initClassTree(Archive);
        
        Archive.read();
        hiddenMap = Archive.global.at(\TraceView, \hiddenMap) ?? { () };
        
        ShutDown.add({
            Archive.global.put(\TraceView, \hiddenMap, hiddenMap);
        })
    }
    
    *new { 
        |parent, bounds|
        var view;
        
        view = super.new(parent, bounds ?? { 300@80 });
        view.connect();
        
        ^view;
    }
    
    connect {
        lastValues = ();
        itemsMap = ();
        minValues = ();
        maxValues = ();
        graphs = ();
        
        this.columns_(["LABEL ", "  VALUE  ", "  MIN  ", "  MAX  ", ""]);
        this.headerItem.setFonts(Font("M Plus 2", 14).weight_(30));
        this.headerItem.setAlignments(\right);
        this.headerItem.setAlignment(0, \left);
        this.setColumnWidth(0, 240);
        
        PatternTrace.signal(\patterntrace_added).connectTo(this.methodSlot("traceAdded(value)")).freeAfter(this);
        PatternTrace.signal(\patterntrace_removed).connectTo(this.methodSlot("traceRemoved(value)")).freeAfter(this);
        PatternTrace.signal(\patterntrace_changed).connectTo(this.methodSlot("traceChanged(*args)")).freeAfter(this);        
    }
    
    traceAdded { 
        |label|
        var item; 
        defer {
            "Tracing: %".format(label).postln;
            
            itemsMap[label] = itemsMap[label] ?? {
                item = this.addChild([label, ""])
                    .setFont(0, Font("M Plus 2", 14).weight_(20))
                    .setAlignments(\right)
                    .setAlignment(0, \left)
                    .setFonts(Font("Hasklig", 14).weight_(10));
                
                graphs[label] = GraphCounter(label, "", Font(size:10), Color.hsv(1.0.rand, 0.7, 1.0));
                item.setView(4, graphs[label].view);
                
                item;
            };
        }
    }
    
    traceRemoved { 
        |label| 
        defer {
            "Removing trace: %".format(label).postln;
            lastValues[label] = nil;
            minValues[label] = nil;
            maxValues[label] = nil;
            itemsMap[label] !? this.removeItem(_);
            itemsMap[label] = nil;
        }
    }
    
    traceChanged {
        |label, value|
        var min, max;
        
        minValues[label] = min = try { min(value, minValues[label] ?? { value }) } { "" };
        maxValues[label] = max = try { max(value, maxValues[label] ?? { value }) } { "" };
        
        defer {
            if (value.isKindOf(SimpleNumber)) {
                graphs[label].value = value;
            };
            
            [value, min, max].do {
                |val, i|
                itemsMap[label] !? _.setString(i + 1, TraceView.formatNumber(val));
            }
        }
    }
}

TraceView : TreeView {
    classvar <hiddenMap;
    var updater, valueFuncs, valueSetters, graphs, playingView, stoppedView, viewUpdater, viewUpdates;
    var synthDefItems, synthDefInstances, nodeItems, labelItems;
    var <filter;
    var <>removeWait=5;
    
    *gui {
        ^ScrollView().layout_(VLayout(
            [StaticText()
                .string_("SERVER")
                .font_(Font("M Plus 2", 22).weight_(76))
                .stringColor_(Color.grey(0.8, 0.2)), 0],
            TraceView(),
            [StaticText()
                .string_("PATTERNS")
                .font_(Font("M Plus 2", 22).weight_(76))
                .stringColor_(Color.grey(0.8, 0.2)), 0],
            PatternTraceView()
        ))
        .autoRememberPosition(\TraceViewMeta)
        .front;
    }
    
    *initClass {
        Class.initClassTree(Archive);
        
        Archive.read();
        hiddenMap = Archive.global.at(\TraceView, \hiddenMap) ?? { () };
        
        ShutDown.add({
            Archive.global.put(\TraceView, \hiddenMap, hiddenMap);
        })
    }
    
    *new { 
        |parent, bounds|
        var view;
        
        view = super.new(parent, bounds ?? { 300@80 });
        view.connect();
        
        ^view;
    }
    
    filter_{
        |text|
        filter = text;
        
    }
    
    *formatNumber {
        |n|
        n = n.value;
        
        if (n.isKindOf(Collection)) {
            ^n.collect { |v| this.formatNumber(v) }
        };
        
        if (n.isKindOf(SimpleNumber).not) {
            ^n
        };
        
        if (n == inf or: { n == -inf } or: {n == (0/0)}) {
            ^n.asString.padRight(6);
        };
        
        if (n.isKindOf(Float)) {
            n = n.asStringPrec(3);
            n = n.split($.);
            n = [
                n[0].padLeft(6),
                (n[1] ?? "0").padRight(4)
            ];
            ^n.join(".")
        } {
            ^n.asString.padLeft(6) ++ ".    "
        }
    }
    
    connect {
        var children;
        
        synthDefItems = ();
        synthDefInstances = ();
        nodeItems = MultiLevelIdentityDictionary();
        labelItems = MultiLevelIdentityDictionary();
        graphs = MultiLevelIdentityDictionary();
        
        valueFuncs = MultiLevelIdentityDictionary();
        valueSetters = MultiLevelIdentityDictionary();
        
        viewUpdater = CollapsedUpdater(1);
        
        viewUpdater.connectToUnique({
            viewUpdates.do(_.value);
            viewUpdates.clear();
        });
        
        this.columns_(["TARGET ", "  VALUE  ", "  MIN  ", "  MAX  ", ""]);
        this.headerItem.setFonts(Font("M Plus 2", 14).weight_(30));
        this.headerItem.setAlignments(\right);
        this.headerItem.setAlignment(0, \left);
        this.setColumnWidth(0, 240);
        
        updater = SkipJack({
            valueFuncs.leafDo {
                |path, func, i|
                var vals = func.();
                valueSetters.at(*path).(*vals)
            }
        }, 1/4);
        
        Trace.signal(\trace_added).connectTo(this.methodSlot("traceAdded(*args)")).freeAfter(this);
        Trace.signal(\trace_removed).connectTo(this.methodSlot("traceRemoved(*args)")).freeAfter(this);
        
        Trace.do {
            |synthDef, node, label, valueFunc|
            this.traceAdded(synthDef, node, label, valueFunc);
        };
        
        this.onClose = this.onClose.addFunc({
            updater.stop;
            updater = nil;
        });  
        
        
        this.onItemExpanded = {
            |item, expanded|
            if (item.strings[0].contains("SynthDef")) {
                hiddenMap[
                    item.strings[0].findRegexp("SynthDef\\(\\\\(.*)\\)")[1][1].asSymbol
                ] = expanded
            }
        };
        
        CmdPeriod.add({
            this.clear();
            synthDefItems.clear();
            synthDefInstances.clear();
            nodeItems.clear();
            labelItems.clear();
            graphs.clear();
            
            valueFuncs.clear();
            valueSetters.clear();
        })
    }
    
    traceAdded {
        |synthDef, node, label, valueFunc|
        var defItem, nodeItem, labelItem;
        var fontHeading, fontValue, graph;
        
        fontHeading = Font("M Plus 2", 14).weight_(20);
        fontValue = Font("Hasklig", 14).weight_(10);
        
        node = node.nodeID;
        
        defItem = synthDefItems.atDefault(synthDef, { 
            this.insertChild(0, ["SynthDef(\\" ++ synthDef ++ ")"])
                .expanded_(hiddenMap[synthDef.asSymbol] ?? { true })
                .setFont(0, fontHeading.copy.weight_(80))
        });
        synthDefInstances[synthDef] = synthDefInstances.atDefault(synthDef, { IdentitySet() }).add(node);
        
        nodeItem = nodeItems.atDefault([synthDef, node], { 
            defItem.insertChild(0, [node])
                .expanded_(true)
                .setFont(0, fontHeading)
        });
        defItem.setString(1, "[% instances]".format(nodeItems.at(synthDef).size));
        
        labelItem = labelItems.atDefault([synthDef, node, label], { 
            nodeItem.insertChild(0, [label])
                .expanded_(true) 
                .setFonts(fontValue)
                .setAlignments(\right)
                .setAlignment(0, \left)
                .setFont(0, fontHeading)
        });
        
        graph = graphs.atDefault([synthDef, node, label], {
            graph = GraphCounter(label, "", Font(size:10), Color.hsv(1.0.rand, 0.7, 1.0));
            labelItem.setView(4, graph.view);
            graph;
        });
        
        this.sort(0, false);
        defItem.sortChildren(0, 0);
        nodeItem.sortChildren(0, 0);
        labelItem.sortChildren(0, 0);
        
        valueFuncs.put(node, label, valueFunc);
        valueSetters.put(node, label, {
            |min, val, max|
            graph.value = val;
            [val, min, max].do {
                |value, i|
                labelItem.setString(i + 1, this.class.formatNumber(value));
                if (value == inf or: { value == -inf } or: ( value == (0/0) )) {
                    labelItem.setTextColor(i + 1, Color.red)
                }
            }
        });
    }
    
    traceRemoved {
        |synthDef, node, label|
        node = node.nodeID;
        synthDefInstances[synthDef] = synthDefInstances.atDefault(synthDef, { IdentitySet() }).remove(node);
        
        valueFuncs.put(node, label, nil);
        valueSetters.put(node, label, nil);
        this.removeTraceView(synthDef, node, label)
    }
    
    updateView {
        |func, defer=false|
        viewUpdates = viewUpdates.add(func);
        
        if (defer) {
            viewUpdater.update();
        } {
            { viewUpdates.do(_.value) }.defer(0.05)
        }
    }
    
    removeTraceView {
        |synthDef, node, label|
        var labelItem, defItem, nodeItem;
        
        valueFuncs[[node, label]] = nil;
        valueSetters[[node, label]] = nil;
        
        labelItem = labelItems.at(synthDef, node, label);
        nodeItem = nodeItems.at(synthDef, node);
        defItem = synthDefItems.at(synthDef);
        
        nodeItem.setString(0, "[%]".format(node));
        defItem.sortChildren(0, 0);
        
        4.do { |i| labelItem.setTextColor(i, Color.grey(0.8, 0.4)); };
        4.do { |i| nodeItem.setTextColor(i, Color.grey(0.8, 0.4)); };
        
        {
            this.updateView({
                graphs.at(synthDef, node, label) !? { |v| v.view.remove() };
                graphs.removeAt(synthDef, node, label);
                
                labelItems.removeAt(synthDef, node, label);
                this.removeItem(labelItem);
                
                if (labelItems.at(synthDef, node).isEmpty) {
                    nodeItems.removeAt(synthDef, node);
                    this.removeItem(nodeItem);
                    defItem.setString(1, "[% instances]".format(
                        synthDefInstances[synthDef].size
                    ));
                };
                
                if (nodeItems.at(synthDef).isEmpty) {
                    synthDefItems.removeAt(synthDef);
                    this.removeItem(defItem);
                };
            }, true)
        }.defer(removeWait);
    }
    
    front {
        // If we are front, we are acting as a window, so remember positions
        if (this.parent != nil) {
            "Calling front on a TraceView but it has a parent, this is weird.".warn
        };
        
        this.autoRememberPosition(\TraceView);
        
        super.front;
    }
}

+UGen {
    trace {
        |label, trigger=false|
        ^Trace.trace(this, label, trigger);
    }
}

+Collection {
    trace {
        |label, trigger=false|
        ^Trace.trace(this, label, trigger);
    }
}

+Pattern {
    trace {
        |label, func|
        ^PatternTrace.tracePattern(
            this,
            "%:%".format(this.class.name, label).asSymbol,
            func
        )
    }
}
