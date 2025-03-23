ElectraDefBase {
    classvar <fixedColors=true, <colorNames, <>roundColors=false;
    
    *initClass {
        fixedColors = [
            "FFFFFF",
            "F45C51",
            "F49500",
            "529DEC",
            "03A598",
            "C44795",
        ];
        colorNames = [
            \white, \red, \orange, \blue, \green, \pink
        ];
        fixedColors = fixedColors.collect(Color.fromHexString(_));
    }
    
    *new {
        ^super.new.init
    }
    
    init {}
    
    ignoreFields {
        ^[]
    }
    
    fields {
        ^Array.newFrom(this.class.instVarNames).removeAll(this.ignoreFields)
    }
    
    toDict {
        var dict = ();
        this.fields.do {
            |varName|
            dict[varName] = this.perform(varName)
        };
        ^dict;
    }
    
    toJSON {
        ^this.toDict.toJSON;
    }
    
    colorToJSON {
        |color|
        case
            { color.isString } {
                ^color
            }
            { color.isSymbol } {
                ^this.colorToJSON(fixedColors[colorNames.indexOf(color) ? 0])
            }
            // default
            {
                if (roundColors) {
                    var nearest = fixedColors.minIndex({
                        |otherColor|
                        (otherColor.asArray - color.asArray).pow(2).sum.pow(0.5)
                    });
                    color = fixedColors[nearest];
                };
                // ^(
                // 	((color.red * (2.pow(5)-1)).asInteger << 11)
                // 	+ ((color.green * (2.pow(6)-1)).asInteger << 5)
                // 	+ ((color.blue * (2.pow(5)-1)).asInteger << 0)
                // ).asHexString[4..]
                ^color.asHtml[1..]
            }
    }
}

ElectraCCParamDef : ElectraDefBase {
    const channelsPerPort=8;
    var <>deviceId=0, <type="cc7", <>parameterNumber, <min=0, <max=127;
    
    *new {
        |id, device=0|
        ^super.new.init.parameterNumber_(id).deviceId_(device)
    }
    
    toDict {
        ^super.toDict().putAll((
            deviceId: 1 + deviceId
        ))
    }
    
    channel {
        ^(deviceId % channelsPerPort)
    }
    
    endpoint {
        |deviceName, source=false|
        var port;
        source = source.if(\sources, \destinations);
        port = 1 + (deviceId / channelsPerPort).asInteger;
        if (port > 2) {
            Error("deviceId=% is out of range".format(deviceId)).throw
        };
        ^MIDIClient.perform(source).detect({ 
            |d| 
            (d.device == deviceName) and: { d.name == "Electra Port %".format(port) }
        });
    }
}

ElectraRelParamDef : ElectraDefBase {
    const channelsPerPort=8;
    var <>deviceId=0, <type="cc7", <>parameterNumber, <min=0, <max=64,
        <relativeMode="binOffset", <accelerated=true, <relative=true;
    
    *new {
        |id, device=0|
        ^super.new.init.parameterNumber_(id).deviceId_(device)
    }
    
    toDict {
        ^super.toDict().putAll((
            deviceId: 1 + deviceId
        ))
    }
    
    channel {
        ^(deviceId % channelsPerPort)
    }
    
    endpoint {
        |deviceName, source=false|
        var port;
        source = source.if(\sources, \destinations);
        port = 1 + (deviceId / channelsPerPort).asInteger;
        if (port > 2) {
            Error("deviceId=% is out of range".format(deviceId)).throw
        };
        ^MIDIClient.perform(source).detect({ 
            |d| 
            (d.device == deviceName) and: { d.name == "Electra Port %".format(port) }
        });
    }
}

ElectraCC14ParamDef : ElectraDefBase {
    const channelsPerPort=8;
    var <>deviceId=0, <>type="cc14", <>parameterNumber, <>min=0, <>max;
    var <>lsbFirst=false;
    
    *new {
        |id, device=0|
        ^super.new.init.parameterNumber_(id).deviceId_(device)
    }
    
    init {
        max = MIDIControlValue.cc14Max;
        super.init;
    }
    
    toDict {
        ^super.toDict().putAll((
            deviceId: 1 + deviceId
        ))
    }
    
    channel {
        ^(deviceId % channelsPerPort)
    }
    
    endpoint {
        |deviceName, source=false|
        var port;
        source = source.if(\sources, \destinations);
        port = 1 + (deviceId / channelsPerPort).asInteger;
        if (port > 2) {
            Error("deviceId=% is out of range".format(deviceId)).throw
        };
        ^MIDIClient.perform(source).detect({ 
            |d| 
            (d.device == deviceName) and: { d.name == "Electra Port %".format(port) }
        });
    }
}

ElectraControlDef : ElectraDefBase {
    
}

ElectraFaderDef : ElectraControlDef {
    //classvar <>startX=4, <>startY=36, <>spanX=167, <>spanY=90, <>width=146, <>height=56;
    // classvar <>startX=20, <>startY=28, <>spanX=167, <>spanY=90, <>width=146, <>height=56;
    classvar <>startX=20, <>startY=28, <>spanX=166, <>spanY=90, <>width=150, <>height=56;
    classvar <>maxRange;
    // classvar <>maxRange=8191;
    
    var <>color, <parameter,
        <>max, <>min, <>defaultValue=0, <>spec,
        <>name="", <>type="fader", <>mode="",
        <>overlay, <>bank, <>index, <>variant="thin";
    
    *initClass {
        maxRange = MIDIControlValue.cc14Max;
    }
    
    *fromSpec {
        |controlSpec|
        var def = this.new();
        var range = this.calcMinMax(controlSpec);
        
        def.min = range[0];
        def.max = range[1];
        def.defaultValue = controlSpec.default;
        def.spec = controlSpec;
        
        ^def
    }
    
    *new {
        ^super.new.init
    }
    
    *calcMinMax {
        |controlSpec|
        var zeroVal;
        
        if (controlSpec.warp.isKindOf(DbFaderWarp)) {
            if ((controlSpec.minval < 1.0) && (controlSpec.maxval > 1.0)) {
                zeroVal = controlSpec.unmap(1.0);
                ^[
                    -1 * (maxRange * zeroVal).floor.asInteger,
                    (maxRange * (1 - zeroVal)).floor.asInteger
                ]
            } {
                ^[
                    0,
                    maxRange
                ]
            }
        };
        
        if ((controlSpec.minval * controlSpec.maxval).isNegative) {
            zeroVal = controlSpec.unmap(0);
            ^[
                -1 * (maxRange * zeroVal).floor.asInteger,
                (maxRange * (1 - zeroVal)).floor.asInteger
            ]
        } {
            ^[
                0,
                maxRange
            ]
        }
    }
    
    toLuaNumber {
        |value|
        ^if (value == inf) {
            "(1/0)"
        } {
            if (value == -inf) {
                "(-1/0)"
            } {
                value.asString
            }
        }
    }
    
    luaWarpDescription {
        |id, deviceId|
        var index = (deviceId + 1) + (32 * id);
        ^"\t[%] = {%, %, %, %, %, %}".format(
            index,
            spec.warp.class.name.asString,
            spec.minval,
            spec.maxval,
            spec.step ?? {0},
            spec.warp.respondsTo(\curve).if({ spec.warp.curve }, { 0 })
        )
    }
    
    ignoreFields { ^[\defaultValue, \name, \min, \max, \parameter, \bank, \index, \step, \warpFunc, \spec] }
    
    init {
        this.color = Color.green;
        this.parameter = ElectraCC14ParamDef(0);
        // this.parameter = ElectraRelParamDef(0);
        this.overlay = ElectraOverlayDef();
    }
    
    parameter_{
        |p|
        parameter = p;
    }
    
    toDict {
        ^super.toDict
            .putAll((
                name: name ++ (spec.units.notEmpty.if({ " [%]".format(spec.units) }, { "" })),
                color: this.colorToJSON(color),
                visible: true, // 3.0 firmware
                bounds: Rect(
                    (startX + ((index % 6) * spanX)).asInteger,
                    (startY + (bank * spanY * 2) + ((index / 6).floor * spanY)).asInteger,
                    width.asInteger,
                    height.asInteger
                ).asArray,
                values: [(
                    id: "value",
                    min: min,
                    max: max,
                    defaultValue: spec.unmap(defaultValue).linlin(0, 1, min, max).round.asInteger;,
                    formatter: "warp",
                    message: parameter.toDict(),
                )]
            ))
    }
}

ElectraListDef : ElectraControlDef {
    classvar <>startX=20, <>startY=28, <>spanX=167, <>spanY=90, <>width=146, <>height=56;
    
    var <>color, <parameter,
        <>name="", <>type="list",
        <>bank, <>index, <>overlay, <>variant;
    
    *fromSpec {
        |controlSpec|
        var def = this.new().values_(controlSpec.items);
        ^def
    }
    
    *new {
        ^super.new.init
    }
    
    ignoreFields { ^[\parameter] }
    
    init {
        this.color = Color.green;
        this.parameter = ElectraCCParamDef(0);
        this.overlay = ElectraOverlayDef();
    }
    
    values_{
        |values|
        overlay.items = values;
    }
    
    parameter_{
        |p|
        parameter = p;
    }
    
    toDict {
        ^super.toDict
            .putAll((
                color: this.colorToJSON(color),
                bounds: Rect(
                    (startX + ((index % 6) * spanX)).asInteger,
                    (startY + (bank * spanY * 2) + ((index / 6).floor * spanY)).asInteger,
                    width.asInteger,
                    height.asInteger
                ).asArray,
                values: [(
                    id: "value",
                    message: parameter.toDict().putAll((
                        onValue: 127,
                        offValue: 0
                    ))
                )]
            ))
    }
}


ElectraPadDef : ElectraControlDef {
    classvar <>startX=20, <>startY=28, <>spanX=167, <>spanY=90, <>width=146, <>height=56;
    
    var <>color, <parameter,
        <>name="", <>type="pad", <>momentary=false,
        <>onValue=127, <>offValue=0,
        <>bank, <>index;
    
    *fromSpec {
        |controlSpec|
        var def = this.new();
        ^def
    }
    
    *new {
        ^super.new.init
    }
    
    ignoreFields { ^[\momentary, \parameter, \onValue, \offValue, \bank, \index] }
    
    init {
        this.color = Color.green;
        this.parameter = ElectraCCParamDef(0);
    }
    
    parameter_{
        |p|
        parameter = p;
    }
    
    toDict {
        ^super.toDict
            .putAll((
                mode: if(momentary, {"momentary"}, {"toggle"}),
                color: this.colorToJSON(color),
                overlay: [],
                bounds: Rect(
                    (startX + ((index % 6) * spanX)).asInteger,
                    (startY + (bank * spanY * 2) + ((index / 6).floor * spanY)).asInteger,
                    width.asInteger,
                    height.asInteger
                ).asArray,
                values: [(
                    id: "value",
                    message: parameter.toDict().putAll((
                        onValue: 127,
                        offValue: 0
                    ))
                )]
            ))
    }
}

ElectraOverlayDef : ElectraDefBase {
    classvar <>bitmap;
    var <>items, <>bitmaps;
    
    init {
        items = ();
        bitmaps = ();
    }
    
    put {
        |index, value, image|
        var pixels, resizedImage;
        
        items[index] = value;
        
        if (image.notNil) {
            resizedImage = Image(48, 18);
            resizedImage.fill(Color.black);
            resizedImage.pixelRatio = 1;
            // resizedImage.draw({
            // 	Pen.fillColor = Color.white;
            // 	Pen.fillRect(Rect(6, 0, 8, 8))
            // });
            image.pixelRatio = 1;
            resizedImage.draw({
                image.drawInRect(
                    Rect(15, 0, 18, 18)
                )
            });
            
            pixels = resizedImage.asXBMString.postln;
            
            bitmaps[index] = pixels;
        }
    }
    
    at {
        |index|
        ^items[index]
    }
    
    toDict {
        var dict, sorted;
        sorted = items.asPairs.clump(2);
        sorted.sort({ |a, b| a[1] < b[1] });
        dict = (
            items: sorted.collect {
                |item, i|
                (
                    value: item[0].asInteger,
                    label: item[1].asString,
                    bitmap: bitmaps[item[0].asInteger]
                )
            }
        );
        ^dict
    }
    
    isEmpty {
        ^(items.size == 0)
    }
}

ElectraGroupDef : ElectraDefBase {
    const startX=14, startY=6, spacingX=2, spanY=176, width=164, height=171;
    var <>name="Group", <>position, <>span, <>color, <>variant="";
    var <>bank;
    
    *new {
        |name, position, span, bank, color|
        ^super.newCopyArgs(name, position, span, color ?? Color.red).init
    }
    
    ignoreFields { ^[
        \span,
        \bank, \position // 3.0 firmware
    ] }
    
    range {
        ^Range(position, span - 1)
    }
    
    overlaps {
        |other|
        ^((this !== other) and: {
            this.range.overlaps(other.range);
        })
    }
    
    toDict {
        var spanX = spacingX + width;
        ^super.toDict().putAll((
            color: this.colorToJSON(color),
            bounds: Rect(
                startX + (position * spanX),
                startY + (bank * spanY),
                (width * span) + ((span - 1).max(0) * spacingX),
                height
            ).asArray
        ))
    }
}

ElectraBank : ElectraDefBase {
    const <bankSize=12, <bankCount=3;
    var <>slots, <>groups, <>index;
    
    ignoreFields { ^[\index] }
    
    init {
        slots = nil ! 12;
        groups = [];
    }
    
    put {
        |slotIndex, control|
        control.bank = index;
        control.index = slotIndex;
        slots[slotIndex] = control;
    }
    
    at {
        |slotIndex|
        ^slots[slotIndex]
    }
    
    addGroup {
        |group|
        var newGroups, overlapGroup;
        
        if ((group.position < 6) and: {(group.position + group.span) > 6}) {
            this.addGroup(ElectraGroupDef(group.name, group.position, 6 - group.position), group.color);
            this.setGroup(ElectraGroupDef(group.name, 6, group.span - (6 - group.position), group.color));
        } {
            overlapGroup = groups.detect({
                |otherGroup|
                group.overlaps(otherGroup)
            });
            if (overlapGroup.notNil)
                {
                    "Group [%-%] overlaps with other group [%-%]".format(
                        group.position, group.position + group.span,
                        overlapGroup.position, overlapGroup.position + overlapGroup.span
                    ).error
                } {
                    group.bank = index;
                    groups = groups.add(group);
                };
        };
    }
}

ElectraPage : ElectraDefBase {
    const <pageCount=12, <pageSize=36;
    var <>banks, <>id, <>name="";
    
    *new {
        |id|
        ^super.new.id_(id)
    }
    
    ignoreFields { ^[\banks] }
    
    init {
        banks = [
            ElectraBank().index_(0),
            ElectraBank().index_(1),
            ElectraBank().index_(2),
        ]
    }
    
    controlDicts {
        var controls;
        controls = banks.collect {
            |bank, bankIndex|
            bank.slots.collect {
                |slot, slotIndex|
                slot !? {
                    slot.toDict.putAll((
                        id: 1 + (id * pageSize) + (bankIndex * bank.slots.size) + slotIndex,
                        inputs: [
                            (
                                potId: 1 + slotIndex,
                                valueId: "value"
                            )
                        ],
                        controlSetId: 1 + bankIndex,
                        pageId: 1 + id
                    ))
                };
            }
        };
        controls = controls.flatten.reject(_.isNil);
        ^controls
    }
    
    groupDicts {
        var groups;
        groups = banks.collect {
            |bank, i|
            bank.groups.collect {
                |group|
                group = group.toDict;
                group.putAll((
                    pageId: 1 + id
                ));
            }
        };
        ^groups.flatten;
    }
    
    toDict {
        ^super.toDict.putAll((
            id: 1 + id
        ))
    }
}

ElectraPreset : ElectraDefBase {
    var <>pages, <>name="", <>channel=0;
    
    init {
        pages = 12.collect(ElectraPage(_))
    }
    
    toDict {
        var dict = (
            version: 2,
            name: name,
            projectId: "tdz2SHUCM1tHfIsb95ZQ",
            pages: [],
            groups: [],
            devices: [],
            overlays: [],
            controls: [],
        );
        
        dict[\pages] = pages.collect(_.toDict());
        dict[\controls] = pages.collect(_.controlDicts).flatten;
        dict[\groups] = pages.collect(_.groupDicts).flatten;
        dict[\groups].do({ |group, index| group[\id] = index });
        dict[\devices] = 16.collect {
            |i|
            (
                id: 1 + i,
                name: "Generic MIDI",
                // instrumentId: "generic-midi", // 3.0 firmware
                port: 1 + (i / 8).asInteger,
                channel: 1 + (i % 8)
            )
        };
        dict[\controls].do {
            |control|
            var index;
            if (control[\overlay].isEmpty.not) {
                index = dict[\overlays].indexOf(control[\overlay]);
                if (index.isNil) {
                    index = dict[\overlays].size;
                    dict[\overlays] = dict[\overlays].add(control[\overlay]);
                };
                control[\values][0][\overlayId] = 1 + index;
            } {
                control[\overlayId] = nil
            };
            control[\overlay] = nil;
        };
        dict[\overlays] = dict[\overlays].collect {
            |overlay, index|
            overlay.toDict.putAll((
                id: 1 + index
            ))
        };
        
        ^dict
    }
}

ElectraDevice {
    var <name, midiOut, midiFunc;
    var waiting=false, queue;
    
    *isConnected {
        ^MIDIClient.sources.detect({ 
            |m| 
            m.device.contains("Electra Controller") and: { m.name == "Electra CTRL" } 
        }).notNil
    }
    
    *new {
        |name|
        ^super.newCopyArgs(name ?? { "Electra Controller" }).init
    }
    
    isElectraSource {
        |device|
        ^(device.device == name) and: {
            (device.name == "Electra CTRL")
            or: {
                device.name == "Port 3"
            }
        }
    }
    
    init {
        var uid = MIDIClient.sources.detect({ |m| this.isElectraSource(m) }).uid;
        
        midiOut = midiOut ?? {
            MIDIOut.newByName(name, "Electra CTRL")
        };
        
        midiFunc = MIDIdef.sysex(\electraIn, {
            |...args|
            this.prRecieveSysex(*args)
        }, srcID: uid);
        
        midiOut.latency = 0;
    }
    
    queueFunction {
        |func|
        queue = queue.add(func);
        this.prProcessQueue();
    }
    
    sendPreset {
        |json|
        this.sendSysex(
            this.class.makeMessage(\uploadData, \presetFile, json)
        );
    }
    
    sendScript {
        |script|
        this.sendSysex(
            this.class.makeMessage(\uploadData, \luaScript, script)
        );
    }
    
    sendConfiguration {
        |json|
        this.sendSysex(
            this.class.makeMessage(\uploadData, \configurationFile, json)
        );
    }
    
    sendSysex {
        |data|
        Log("Electra").info("Queueing sysex message % (size = %)".format(data.identityHash, data.size));
        queue = queue.add(data);
        this.prProcessQueue();
    }
    
    subscribe {
        |...events|
        var bits = 0;
        
        events.do {
            |e|
            bits = bits + switch(
                e,
                \page, 1 << 0,
                \control, 1 << 1,
                \usb, 1 << 2,
                \pots, 1 << 3,
                \touch, 1 << 4,
                \button, 1 << 5,
                \window, 1 << 6,
                0
            )  
        };
        
        this.sendSysex(
            this.class.makeMessage(\updateRuntime, \subscribe, bits);
        )
    }
    
    enableLogging {
        |enabled=true|
        this.sendSysex(
            this.class.makeMessage(\systemCall, \loggerStatus,
                enabled.if({[0x01, 0x00]}, {[0x00, 0x00]})
            )
        );
    }
    
    switchPreset {
        |bank=0, slot=0|
        this.sendSysex(
            this.class.makeMessage(\switch, \preset,
                [bank, slot],
            )
        )
    }
    
    switchPage {
        |page=0|
        
        this.sendSysex(
            this.class.makeMessage(\switch, \page,
                [page],
            )
        );
    }
    
    switchControlSet {
        |controlSet=0|
        
        this.sendSysex(
            this.class.makeMessage(\switch, \controlSet,
                [controlSet]
            )
        );
    }
    
    *api {
        |category, command|
        var api = (
            queryData: (
                _byte: 0x02,
                electraInformation: 0x7F,
                runtimeInformation: 0x7E,
                presetFile: 0x01,
                configurationFile: 0x02,
                snapshotList: 0x05,
                luaScript: 0x0C,
                applicationInformation: 0x7C,
            ),
            uploadData: (
                _byte: 0x01,
                presetFile: 0x01,
                configurationFile: 0x02,
                luaScript: 0x0C,
            ),
            systemCall: (
                _byte: 0x7F,
                logMessage: 0x00,
                loggerStatus: 0x7D,
            ),
            updateRuntime: (
                _byte: 0x14,
                control: 0x07,
                subscribe: 0x79,
            ),
            switch: (
                _byte: 0x09,
                preset: 0x08,
                page: 0x0A,
                controlSet: 0x0B
            )
        );
        category = api[category];
        
        ^Int8Array[category[\_byte], category[command]];
    }
    
    *makeMessage {
        |category, command, data|
        var header, footer, msg;
        
        Log("Electra").info("Making message: %, %, data.size = %", category, command, data.size);
        
        header = Int8Array[
            0xF0, 				// sysex header
            0x00, 0x21, 0x45, 	// electra manufacturer id
        ];
        footer = Int8Array[0xF7]; // sysex closing byte
        command = this.api(category, command);
        
        if (data.isString) {
            data = data.collectAs(_.ascii, Int8Array)
        };
        
        if (data.class != Int8Array) {
            data = Int8Array.newFrom(data.asArray)
        };
        
        msg = Int8Array(header.size + command.size + data.size + footer.size);
        msg = msg.addAll(header);
        msg = msg.addAll(command);
        msg = msg.addAll(data);
        msg = msg.addAll(footer);
        
        ^msg;
    }
    
    prProcessQueue {
        var item;
        if (waiting.not and: { queue.size > 0 }) {
            item = queue.pop();
            Log("Electra").info("Sending sysex message % (size = %)".format(item.identityHash, item.size));
            
            if (item.isKindOf(Function)) {
                item.();
                this.prProcessQueue();
            } {
                waiting = true;
                midiOut.sysex(item);
            }
        }
    }
    
    prRecieveACK {
        Log("Electra").info("Received ack for last sysex");
        waiting = false;
        this.prProcessQueue();
    }
    
    prRecieveNAK {
        "Received a NAK: something must have gone wrong".error;
        waiting = false;
        this.prProcessQueue();
    }
    
    prRecieveSysex {
        |sysex|
        var category, msg;
        var api = (
            0x7E: ( // controller
                0x01: {
                    this.prRecieveACK();
                },
                0x00: {
                    this.prRecieveNAK();
                },
                0x0A: {
                    this.update(\potTouched, sysex[7], sysex[9] == 1)
                }
            )
        );
        
        category = sysex[4];
        msg = sysex[5];
        
        api[category] !? _[msg] !? _.value;
    }
}

ElectraFactory {
    classvar <overlayCache, <>overlaySteps=64, <>reloadDelay=3;
    classvar <>logPath;
    
    *initClass {
        overlayCache = Dictionary();
    }
    
    *load {
        |...cves|
        ^this.loadDevice(ElectraDevice(), *cves);
    }
    
    *loadDevice {
        |device ...cves|
        var preset, json, lua;
        
        try {
            if (device.isKindOf(String)) {
                device = ElectraDevice(device);
            };
            device = device ?? { ElectraDevice() };
            
            // device.enableLogging(false);
            
            cves.do {
                |cve|
                if (cve.isKindOf(Association)) {
                    cve = cve.value;
                };
                // 
                cve.signal(\controls).connectToUnique(\ElectraFactory, {
                    this.loadDevice(device, *cves);
                }).collapse(reloadDelay);
            };
            
            preset = this.newFrom(device.name, *cves);
            json = preset.toDict.toJSON;
            device.sendPreset(json);
            device.subscribe(\page, \pots, \touch);
            
            if (logPath.notNil) {
                File.open((logPath +/+ "electra.json").standardizePath, "w").write(json).close();
            };
            
            {
                lua = this.makeLuaScript(preset);
                device.sendScript(lua);
                
                if (logPath.notNil) {
                    File.open((logPath +/+ "electra.lua").standardizePath, "w").write(lua).close();
                };
            }.defer(2);
            
            // workaround for defaultValue not working
            device.queueFunction({
                "*** ELECTRA PRESET LOADED ***".postln;
                // cves.do {
                //     |cve|
                //     if (cve.isKindOf(Association)) {
                //         cve = cve.value;
                //     };
                
                //     cve.do(_.emitChanged())
                // }
            });
        } {
            |e|
            e.reportError
        }
    }
    
    *loadFile {
        |file, deviceName="Electra Controller"|
        var midiOut = MIDIOut.newByName(deviceName, "Electra CTRL");
        var json = File.readAllString(file.standardizePath);
        midiOut.latency = 0;
        midiOut.sysex(ElectraPreset.toSysexBytes(json));
    }
    
    *newFrom {
        |deviceName ...cves|
        var preset = ElectraPreset();
        var index = 0;
        var page;
        
        cves.do {
            |cve|
            if (cve.isKindOf(Association)) {
                page = cve.key;
                cve = cve.value;
                
                if ((page * ElectraPage.pageSize) >= index) {
                    index = page * ElectraPage.pageSize;
                } {
                    // Just choose the next page up
                    page = (index / ElectraPage.pageSize).asInteger;
                    index = (page + 1) * ElectraPage.pageSize;
                }
            };
            
            index = this.addAllToPreset(deviceName, preset, cve, index);
        };
        
        ^preset;
    }
    
    *addAllToPreset {
        |deviceName, preset, cve, index = 0|
        var groups, groupItems, groupOrder, defaultGroup;
        var luaWarpDescriptions;
        
        defaultGroup = '';
        groups = ();
        groupItems = ();
        
        cve.keysValuesDo {
            |name, cv|
            var group = cv.md[\group] ?? { cve.name } ?? { defaultGroup };
            
            if (group.isKindOf(Event).not) { group = (name: group.asSymbol) };
            
            group[\color] = group[\color] ?? { cve.color };
            groups[group[\name]] = group;
            groupItems[group[\name]] = groupItems[group[\name]].add(cv);
        };
        
        groups = groups.asArray.sort({
            |a, b|
            var names, pages, banks, orders;
            
            names = [
                a[\name] ?? { "" },
                b[\name] ?? { "" },
            ];
            
            pages = [
                a[\page] ?? { 0 },
                b[\page] ?? { 0 },
            ];
            
            banks = [
                a[\bank] ?? { 0 },
                b[\bank] ?? { 0 },
            ];
            
            orders = [
                a[\order] ?? { 0 },
                b[\order] ?? { 0 },
            ];
            
            if (pages[0] != pages[1]) {
                pages[0] < pages[1]
            } {
                if (banks[0] != banks[1]) {
                    banks[0] < banks[1]
                } {
                    if (orders[0] != orders[1]) {
                        orders[0] < orders[1]
                    } {
                        names[0] < names[1]
                    }
                }
            }
        });
        
        groups.do {
            |group|
            var items = groupItems[group[\name]];
            
            items = items.sort({
                |a, b|
                if (a.md[\order].isNil) {
                    if (b.md[\order].isNil) {
                        a.md[\name] < b.md[\name]
                    } {
                        (99999) < b.md[\order]
                    }
                } {
                    if (b.md[\order].isNil) {
                        a.md[\order] < (99999)
                    } {
                        a.md[\order] < b.md[\order]
                    }
                }
            });
            
            // "Group % with items: %".format(group.name, items.collect(_.name)).postln;
            index = this.addToPreset(deviceName, index, preset, group, items, cve.name);
        };
        
        ^index
    }
    
    *makeLuaScript {
        |preset|
        var script = File.readAllString("~/Desktop/scztt-Core/sc_electra.lua".standardizePath);
        var warpLines = [];
        
        preset.pages.do {
            |page|
            page.banks.do {
                |bank|
                bank.slots.do {
                    |item|
                    if (item.respondsTo(\luaWarpDescription)) {
                        warpLines = warpLines.add(
                            item.luaWarpDescription(
                                item.parameter.parameterNumber, item.parameter.deviceId
                            )
                        )
                    }
                }
            }
        };
        
        script = script.replace(
            "%%%warps%%%",
            warpLines.join(",\n")
        );
        
        ^script
    }
    
    *addToPreset {
        |deviceName, index, preset, group, items, pageName=""|
        var groupPage, groupBank, page, pageIndex, bank, bankIndex, controlDef, leftoverItems;
        
        if (items.size > ElectraBank.bankSize) {
            leftoverItems = items[ElectraBank.bankSize..];
            items = items[0..(ElectraBank.bankSize-1)];
        };
        
        groupPage 	= group[\page];
        groupBank	= group[\bank];
        
        page = (index / ElectraPage.pageSize).asInteger;
        if (groupPage.notNil) {
            if (groupPage > page) {
                page = groupPage;
                index = groupPage * ElectraPage.pageSize;
            } {
                if (groupPage < page) {
                    "Group % page is %, but we're already on page %".format(group[\name], groupPage, page).warn;
                };
            }
        };
        pageIndex 	= index - (page * ElectraPage.pageSize);
        
        bank = (pageIndex / ElectraBank.bankSize).asInteger;
        
        if (groupBank.notNil) {
            if (groupBank > bank) {
                bank = groupBank;
                index = (page * ElectraPage.pageSize) + (groupBank * ElectraBank.bankSize);
            } {
                if (groupBank < bank) {
                    "Group % bank is %, but we're already on bank %".format(group[\name], groupBank, bank).warn;
                };
            }
        };
        
        pageIndex 	= index - (page * ElectraPage.pageSize);
        bankIndex 	= pageIndex - (bank * ElectraBank.bankSize);
        
        if (bankIndex.odd) {
            bankIndex = bankIndex + 1;
            index = index + 1;
        };
        
        if ((items.size + bankIndex) > ElectraBank.bankSize) {
            bank = bank + 1;
            bankIndex = 0;
            pageIndex = bank * ElectraBank.bankSize;
            
            if (bank >= ElectraBank.bankCount) {
                page = page + 1;
                pageIndex = 0;
                bank = 0;
                bankIndex = 0;
            };
            
            index = (page * ElectraPage.pageSize) + pageIndex;
        };
        
        preset.pages[page].name = pageName;
        
        items.do {
            |item, slotIndex|
            
            // We want top-to-bottom ordering, not left-to-right as our data scructure stores them.
            // "bankIndex: %, flotIndex: %".format(bankIndex, slotIndex).postln;
            slotIndex = this.convertBankIndex(bankIndex + slotIndex);
            controlDef = this.makeDef(deviceName, item, group, index);
            
            // "preset.pages[%].banks[%][%] = ElectraFaderDef(%);".format(page, bank, slotIndex, controlDef.name).postln;
            preset.pages[page].banks[bank][slotIndex] = controlDef;
            
            index = index + 1;
        };
        
        preset.pages[page].banks[bank].addGroup(
            ElectraGroupDef(
                name: 		group.name,
                position: 	this.convertBankIndex(bankIndex),
                span: 		(items.size / 2).ceil.asInteger,
                color: 		group.color ?? { Color.blue }
            )
        );
        
        if (leftoverItems.size > 0) {
            ^this.addToPreset(deviceName, index, preset, group, leftoverItems, pageName)
        } {
            ^index
        }
    }
    
    *fixName {
        |name|
        ^name.asString.split($_).last
    }
    
    *makeDef {
        |deviceName, cv, group, index|
        var minimumVisibleRange = 20;
        var smallRange, largeRange, nonLinear;
        var specRange, controlDef, overlayLabel, overlayUnits, midiCV;
        var paramIds14 = (0x00 + (0x00..0x1F)) ++ (0x40 + (0x00..0x1F));
        var luaWarpDescriptions;
        
        case(
            { cv.isKindOf(OnOffControlValue) or: { cv.metadata[\onOff].isKindOf(True) } }, {
                controlDef = ElectraPadDef();
                controlDef.name = this.fixName(cv.name);
                controlDef.color = cv.color ?? { group.color } ?? { Color.white };
                controlDef.parameter = ElectraCCParamDef(index % 32, (index / 32).asInteger);
                controlDef.momentary = cv.metadata[\momentary] ?? { false };
                
                cv.cc_(
                    controlDef.parameter.parameterNumber,
                    controlDef.parameter.channel,
                    controlDef.parameter.endpoint(deviceName, true).uid,
                    broadcast:true
                );
            },
            { cv.isKindOf(ArrayControlValue) }, {
                controlDef = ElectraListDef();
                controlDef.name = this.fixName(cv.name);
                controlDef.color = cv.color ?? { group.color } ?? { Color.white };
                controlDef.parameter = ElectraCCParamDef(index % 32, (index / 32).asInteger);
                controlDef.variant = cv.md[\variant] ?? { "thin" };
                
                // ~i = Image(48, 18).fill(Color.black);
                // ~i.pixelRatio = 1;
                // ~i.draw {
                // 	Pen.fillColor = Color.white;
                // 	8.do {
                // 		|x|
                // 		Pen.fillRect(Rect(x, x, 1, 1))
                // 	}
                // };
                
                cv.items.do({
                    |value, i|
                    var icon = cv.metadata[\icons];
                    
                    if (icon.isKindOf(Dictionary)) {
                        icon = icon[value.asSymbol];
                    };
                    if (icon.isKindOf(Array)) {
                        icon = icon[i];
                    };
                    
                    controlDef.overlay.put(
                        (i / (cv.items.size - 1).asFloat * 127.0).asInteger,
                        value,
                        icon
                    )
                });
                
                cv.cc_(
                    controlDef.parameter.parameterNumber,
                    controlDef.parameter.channel,
                    controlDef.parameter.endpoint(deviceName, true).uid,
                    broadcast:true
                );
            },
            {
                controlDef = ElectraFaderDef.fromSpec(cv.spec.asSpec);
                controlDef.name = this.fixName(cv.name);
                controlDef.type = cv.md[\controlType] ?? { "fader" };
                controlDef.variant = cv.md[\variant] ?? { "thin" };
                controlDef.defaultValue = cv.value;
                
                specRange = cv.spec.maxval.absdif(cv.spec.minval);
                
                smallRange = (specRange < minimumVisibleRange) && (cv.spec.step < 1.0);
                largeRange = specRange >= 512;
                nonLinear = cv.spec.warp.isKindOf(LinearWarp).not;
                
                // controlDef.defaultValue = cv.input.linlin(0, 1, controlDef.min, controlDef.max).round.asInteger.postln;
                controlDef.color = cv.color ?? { group.color } ?? { Color.white };
                
                if (cv.md[\cc8].asBoolean.not) {
                    controlDef.parameter = ElectraCC14ParamDef(
                        paramIds14[index % paramIds14.size], 
                        (index / paramIds14.size).asInteger
                    );
                    if (cv.respondsTo('cc14_')) {
                        cv.cc14_(
                            controlDef.parameter.parameterNumber,
                            controlDef.parameter.channel,
                            controlDef.parameter.endpoint(deviceName, true).uid,
                            broadcast:true
                        );
                        // cv.ccRel_(
                        //     controlDef.parameter.parameterNumber,
                        //     controlDef.parameter.channel,
                        //     controlDef.parameter.endpoint(true).uid,
                        //     broadcast:true
                        // );
                    } {
                        "Could not connect parameter '%' to midi".format(cv.name).warn;
                    };
                } {
                    controlDef.parameter = ElectraCCParamDef(index % 32, (index / paramIds14.size).asInteger);
                    if (cv.respondsTo('cc_')) {
                        cv.cc_(
                            controlDef.parameter.parameterNumber,
                            controlDef.parameter.channel,
                            controlDef.parameter.endpoint(deviceName, true).uid,
                            broadcast:true
                        );
                    } {
                        "Could not connect parameter '%' to midi".format(cv.name).warn;
                    };
                };                
            }
        );
        
        ^controlDef
    }
    
    *makeOverlay {
        |steps, spec|
        var overlayLabel, overlayUnits;
        var overlay = overlayCache[spec];
        var specRange = spec.maxval.absdif(spec.minval);
        
        if (overlay.isNil) {
            overlay = ElectraOverlayDef();
            
            if (spec.tryPerform(\units).size > 0) {
                overlayUnits = " " ++ spec.units.stripWhiteSpace;
            } {
                overlayUnits = ""
            };
            
            (0..steps).do {
                |i|
                overlayLabel = spec.map(i / steps.asFloat);
                if (specRange.log10 > 3) {
                    overlayLabel = overlayLabel.round(1).asStringPrec(1000);
                } {
                    overlayLabel = overlayLabel.round(10.pow(specRange.log10.floor.max(0) - 3)).asString
                };
                overlay[i] = overlayLabel ++ overlayUnits;
            };
            
            overlayCache[spec] = overlay;
        };
        
        ^overlay
    }
    
    *convertBankIndex {
        |index|
        ^(
            // should be [0, 6, 1, 7, 2, 8, 3, 9, ...]
            ((index % 2) * 6) + (index / 2).floor.asInteger;
        )
    }
}

+Image {
    asXBMString {
        |dither=false|
        var w, h, array, residual, value;
        var b64String = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        
        w = this.width;
        h = this.height;
        array = Int8Array(w * h);
        residual = 0.0;
        
        h.do {
            |y|
            w.do {
                |x|
                value = this.getColor(x, y);
                value = value.asHSV[2] + residual;
                
                if (dither) {
                    residual = value - value.round(1.0);
                };
                
                array.add(value.round(1.0).asInteger);
            }
        };
        array = array.clump(8).collect(_.reverse).flatten;
        
        array = array.clump(6).collect {
            |v|
            var value = 0;
            v.do {
                |v, i|
                value = value.setBit(5 - i, v > 0)
            };
            value;
        };
        
        array = array.collect {
            |v|
            b64String[v]
        };
        array = array.collect(_.asAscii).join("");
        
        ^array
    }
}

+Range {
    overlaps {
        |other|
        ^(
            this.includes(other.start)
                or: {
                    this.includes(other.end - 1)
                }
                or: {
                    other.includes(this.start)
                }
                or: {
                    other.includes(this.end - 1)
                }
        )
    }
}







