
(
var guiFunc;
var ampMin = -70;
var ampMax = 48;
var numberBoxHeight = 22;
var oscDef;

var compressFunc = {
    |isSynth, amp, aboveRatio, belowRatio, ratioScale, aboveThreshold, belowThreshold, knee, curve, expandMax, plot=false|
    var compressAmt, expandAmt;
    var kneeStart, kneeSlopeStart, kneeEnd, kneeSlopeEnd;
    var zero, curveMult, hermCurve;
    var compressCurve;
    
    zero = isSynth.if({ DC.ar(0) }, { 0 });
    curveMult = isSynth.if(
        {
            aboveRatio > 1
        },
        {
            (aboveRatio > 1).if(1, 0)
        }
    );
    
    aboveRatio = (1 / aboveRatio) * 2.pow(ratioScale.neg);
    belowRatio = (1 / belowRatio) * 2.pow(ratioScale.neg);
    
    knee = min(knee, (aboveThreshold - belowThreshold).abs / 2);
    
    compressAmt = (amp - aboveThreshold);
    compressAmt = compressAmt.linlin(knee.neg, knee, 0, knee) + (compressAmt - knee).clip(0, inf);
    aboveRatio = aboveRatio / (1 + (curve * compressAmt * curveMult));
    compressAmt = compressAmt * (1 - aboveRatio);
    
    expandAmt = (belowThreshold - amp);
    expandAmt = expandAmt.linlin(knee.neg, knee, 0, knee) + (expandAmt - knee).clip(0, inf);
    expandAmt = expandAmt * (1 - belowRatio);
    expandAmt = expandAmt.clip(expandMax.neg, expandMax);
    
    [compressAmt, expandAmt];
};


// Ndef(\eqcomp).clear
SynthDef.channelized(\compressor, 
    channelizations: [1, 2, 4, 6, 8], 
    ugenGraphFunc: {
        |numChannels=2|
        var in, bands, rms, peak, letters;
        var thresholdAdd, aboveRatioMul, belowRatioMul, postAmp;
        var attack, decay, solo, preGain, metadata, gain, knee, bypassAll, ratioScale, expandMax, limit;
        var lag;
        var fLow, fMid, fHigh;
        var db = true;
        
        in = \in.ar(0 ! numChannels);
        // in = In.ar(\in.kr, 2);
        
        letters 		= ["a", "b", "c", "d"];
        
        lag				= \lagTime.kr(spec:ControlSpec(0, 10, warp:12, default:0.1));
        preGain			= \preGain.kr(spec:ControlSpec(ampMin, ampMax, \lin, default:0)).lag(lag);
        attack 			= \attack.kr(spec:ControlSpec(0, 0.2, \lin, default: 0.01)).lag(lag);
        decay 			= \decay.kr(spec:ControlSpec(0, 1, \lin, default: 0.1)).lag(lag);
        solo			= \solo.kr(spec:ControlSpec(-1, 4, \lin, default: -1)).lag(lag);
        gain			= \gain.kr(spec:ControlSpec(ampMin, ampMax, \lin, default: 0)).lag(lag);
        bypassAll       = \bypassAll.kr(spec:ControlSpec(0, 1, \lin, default: 0)).lag(lag);
        knee			= \knee.kr(spec:ControlSpec(0, 12, default:3)).lag(lag);
        expandMax		= \expandMax.kr(spec:ControlSpec(0, 36, default:24)).lag(lag);
        ratioScale	 	= \ratioScale.kr(spec:ControlSpec(-4, 4, default:0));
        limit           = \limit.kr(spec:ControlSpec(0, 1, default:0));
        
        fLow			= \fLow.kr(spec:\freq.asSpec.copy.default_(100));
        fMid			= \fMid.kr(spec:\freq.asSpec.copy.default_(700));
        fHigh			= \fHigh.kr(spec:\freq.asSpec.copy.default_(4000));
        
        rms				= \rms.kr(spec:ControlSpec(0, 1, default:1));
        rms 			= rms > 0;
        
        knee			= max(knee, 0.00001); // avoid divide-by-zero
        
        metadata 		= Array.newClear((4*4));
        
        in = in * preGain.dbamp;
        
        bands = BandSplitter4.ar(in, fLow, fMid, fHigh);
        bands = bands.collect {
            |band, i|
            var amp, ampDiff, preGain, belowThreshold, aboveThreshold, belowRatio, aboveRatio, bypass;
            var targetAmp, gain, compressAmt, expandAmt, curve;
            var mute, zero = DC.ar(0);
            
            preGain			= "preGain_%".format(letters[i]).asSymbol.kr(spec:ControlSpec(ampMin, ampMax, \lin, default: 0));
            aboveThreshold 	= "aboveThreshold_%".format(letters[i]).asSymbol.kr(spec:ControlSpec(ampMin, ampMax, \lin, default: -10));
            belowThreshold 	= "belowThreshold_%".format(letters[i]).asSymbol.kr(spec:ControlSpec(ampMin, ampMax, \lin, default: -40));
            curve			= "curve_%".format(letters[i]).asSymbol.kr(spec:ControlSpec(0, 64, default:0));
            aboveRatio 		= "aboveRatio_%".format(letters[i]).asSymbol.kr(spec:ControlSpec(1, 1000, 10, default: 3));
            belowRatio 		= "belowRatio_%".format(letters[i]).asSymbol.kr(spec:ControlSpec(1/3, 20, \exp, default: 1));
            gain 			= "gain_%".format(letters[i]).asSymbol.kr(spec:ControlSpec(ampMin, ampMax, \lin, default: 0));
            bypass 			= "bypass_%".format(letters[i]).asSymbol.kr(spec:ControlSpec(0, 1, \lin, default: 0));
            bypass      	= max(bypassAll, bypass) > 0;
            
            #preGain, gain = K2A.ar([preGain, gain]).dbamp.lag(lag);
            
            mute = (solo >= 0) * ((solo - i).abs > 0.001);
            band = band * (mute < 1);
            band = band * blend(preGain, DC.ar(1), bypass);
            
            amp = blend(
                band.abs,
                RMS.ar(band),
                rms
            );
            amp = ArrayMax.ar(amp)[0].ampdb.clip(*\compressRange.kr([-90, 24]));
            
            #compressAmt, expandAmt = compressFunc.(
                true,
                amp, aboveRatio, belowRatio, 
                ratioScale, aboveThreshold, belowThreshold, 
                knee, curve, expandMax
            );
            compressAmt = blend(compressAmt, zero, bypass);
            expandAmt = blend(expandAmt, zero, bypass);
            
            metadata[i*4] = amp;
            metadata[i*4+1] = (expandAmt - compressAmt).lagud(attack, decay);
            metadata[i*4+2] = (amp + (expandAmt - compressAmt).lagud(attack, decay));
            metadata[i*4+3] = (amp + gain.ampdb + (expandAmt - compressAmt).lagud(attack, decay));
            
            (mute <= 0) * (
                band * gain * (expandAmt - compressAmt).lagud(attack, decay).dbamp
            );
        };
        
        gain = K2A.ar(gain).dbamp.lag(lag);
        bands = gain * bands.sum;
        bands = blend(bands, Limiter.ar(bands, -3.dbamp), limit);
        
        postAmp = blend(
            bands.abs,
            RMS.ar(bands),
            rms
        );
        postAmp = ArrayMax.ar(postAmp)[0].ampdb.clip(-120, 24);
        
        metadata = metadata.add(postAmp);
        
        SendReply.ar(Impulse.ar(30), '/compressor', metadata, \replyId.kr(0));
        
        bands.assertChannels(numChannels);
        
        Out.ar(\out.kr(0), bands);
});

guiFunc = {
    |name, replyId|
    var plotMin = -50, plotMax = 6;
    var bands, makeVLabel, makeNumberView, makeBandView, makeBalanceView, view, makeSlider, makeMultiSlider, makeMeter;
    var bandView, thresholdAddSlider, aboveRatioSlider, below;
    var divider, bandMeters, balanceMeter, outputAmp=0, makeSolo, soloValue, bypassAll, limit;
    var makePlot = {
        |data|    
        var view, plotter;
        var changed;
        
        view = View().layout_(HLayout());
        plotter = Plotter("plotter", Rect(0, 0, 100, 100), view);
        view.layout.add(plotter.interactionView);
        
        data.signal(\value).connectToUnique({
            plotter.setValue(
                data,
                findSpecs: true,
                refresh: true,
                separately: true,
                minval: plotMin,
                maxval: plotMax
            ); 
        }).freeAfter(view);
        
        view.fixedSize_(100@100);
    };
    
    bands = ["a", "b", "c", "d"];
    bandMeters = [0, 0, 0] ! 4;
    balanceMeter = [1, 1, 1, 1] / 4.0;
    
    makeVLabel = {
        |string|
        var size, font;
        
        string = string.asString;
        
        font = Font("Mplus 2", 16, false);
        size = string.bounds(font).insetBy(-3, -3).size;
        size = Size(size.height, size.width);
        
        UserView()
            .minSize_(size)
            .drawFunc_({
                |v|
                Pen.fillColor = Color.grey(0.7);
                Pen.translate(0, v.bounds.height);
                Pen.rotate(-pi/2);
                Pen.stringInRect(
                    string,
                    Rect(0, 0, v.bounds.height, v.bounds.width)
                        .insetBy(18, 6),
                    font,
                    alignment: QAlignment(\left)
                );
            })
            .canFocus_(false)
            .acceptsMouse_(false)
            .update()
    };
    
    makeSolo = {
        |index, soloCV, bypassCV|
        var view, tb, soloAction, bypassAction;
        
        view = HLayout(
            tb = ToolBar(soloAction = MenuAction("solo").checkable_(true)),
            ToolBar(bypassAction = MenuAction("bypass").checkable_(true)),
            nil
        );
        
        soloCV.signal(\value).connectTo({
            |who, what, value|
            soloAction.checked = (value - index).abs < 0.001
        }).freeAfter(tb);
        
        bypassCV.signal(\value).connectTo(bypassAction.methodSlot("checked_(value > 0)")).freeAfter(tb);
        
        bypassAction.signal(\toggled).connectTo(bypassCV.methodSlot("value_(value.if(1, 0))")).freeAfter(tb);
        soloAction.signal(\toggled).connectTo({
            |who, what, value|
            if (value) {
                soloCV.value = index;
            } {
                soloCV.value = -1;
            }
        }).freeAfter(tb);
        
        view;
    };
    
    makeNumberView = {
        |string, cv|
        var view;
        string = string.asString;
        
        view = NumberBox()
            .background_(Color.grey(1, 0.2))
            .maxWidth_(60)
            .fixedHeight_(numberBoxHeight)
            .align_(\right)
            .font_(Font("Mplus 2", 14, false));
        
        view.signal(\value).connectTo(cv.valueSlot).freeAfter(view);
        cv.signal(\value).connectTo(view.valueSlot).freeAfter(view);
        
        view;
    };
    
    makeBalanceView = {
        var view = UserView()
            .animate_(true)
            .drawFunc_({
                |v|
                var lastX = 0.0;
                var width = v.bounds.width;
                var height = v.bounds.height;
                
                Pen.fillColor = Color.hsv(0.1, 0.7, 1, 0.5);
                Pen.strokeColor = Color.hsv(0.1, 0.7, 1, 1);
                Pen.addRect(Rect(
                    lastX, 0.0,
                    floor(balanceMeter[0] * width), height
                ));
                Pen.fillStroke();
                lastX = lastX + ceil(balanceMeter[0] * width);
                
                Pen.fillColor = Color.hsv(0.3, 0.7, 1, 0.5);
                Pen.strokeColor = Color.hsv(0.3, 0.7, 1, 1);
                Pen.addRect(Rect(
                    lastX, 0.0,
                    floor(balanceMeter[1] + width), height
                ));
                Pen.fillStroke();
                lastX = lastX + ceil(balanceMeter[1] * width);
                
                Pen.fillColor = Color.hsv(0.5, 0.7, 1, 0.5);
                Pen.strokeColor = Color.hsv(0.5, 0.7, 1, 1);
                Pen.addRect(Rect(
                    lastX, 0.0,
                    floor(balanceMeter[2] * width), height
                ));
                Pen.fillStroke();
                lastX = lastX + ceil(balanceMeter[2] * width); 
                
                Pen.fillColor = Color.hsv(0.7, 0.7, 1, 0.5);
                Pen.strokeColor = Color.hsv(0.7, 0.7, 1, 1);
                Pen.addRect(Rect(
                    lastX, 0.0,
                    floor(balanceMeter[3] * width), height
                ));
                Pen.fillStroke();
            });
        
        view.fixedHeight_(16).minWidth_(100);
    };
    
    makeBandView = {
        |name, values|
        var preGain, aboveThreshold, belowThreshold, aboveRatio, belowRatio, gain, amp, compression, expansion, bypass, limiter;
        var ampValue=0.5, compressionValue=0.2, expansionValue=0.1, postAmpValue=0;
        var plotData = FloatArray.newFrom(0!128), plotDataCalculate;
        
        var view = View().layout_(
            VLayout(
                HLayout(
                    preGain = makeSlider.(
                        "in gain",
                        currentEnvironment["preGain_%".format(name).asSymbol]
                    ),
                    aboveThreshold = makeMultiSlider.(
                        "threshold",
                        currentEnvironment["aboveThreshold_%".format(name).asSymbol],
                        currentEnvironment["belowThreshold_%".format(name).asSymbol]
                    ),
                    VLayout(
                        UserView().fixedWidth_(40).drawFunc_({
                            |v|
                            var bounds = v.bounds.moveTo(0, 0);
                            var width;
                            Pen.scale(1/2, 1);
                            
                            Pen.capStyle = 0;
                            
                            Pen.width = width = (bounds.width * 0.5) - 2;
                            Pen.lineDash = FloatArray[2 / width, 1 / width];
                            Pen.strokeColor = Color.hsv(0.4, 0.5, 0.8, 0.85);
                            Pen.line(
                                (bounds.width*0.25)@bounds.height,
                                (bounds.width*0.25)@(bounds.height * (1.0 - ampValue)),
                            );
                            Pen.stroke();
                            
                            Pen.width = width = (bounds.width * 1) - 2;
                            Pen.lineDash = FloatArray[2 / width, 1 / width];
                            Pen.strokeColor = Color.hsv(0.4, 0.5, 0.8);
                            Pen.line(
                                (bounds.width*1.5)@bounds.height,
                                (bounds.width*1.5)@(bounds.height * (1.0 - postAmpValue)),
                            );                            
                            Pen.stroke();
                            
                            // compress / expand rectangles
                            Pen.fillColor = Color.hsv(0.55, 1, 0.8);
                            Pen.fillRect(Rect(
                                bounds.width * 0.5,
                                bounds.height * (1.0 - ampValue),
                                bounds.width * 0.5,
                                bounds.height * (ampValue - postAmpValue).max(0)
                            ).insetBy(1, 1));
                            
                            Pen.fillColor = Color.hsv(0.75, 1, 0.8);
                            Pen.fillRect(Rect(
                                bounds.width * 0.5,
                                bounds.height * (1.0 - postAmpValue),
                                bounds.width * 0.5,
                                bounds.height * (postAmpValue - ampValue).max(0)
                            ).insetBy(1, 1));
                            
                        }).animate_(true),
                        
                        StaticText()
                            .string_(name)
                            .align_(\center)
                            .font_(Font("Mplus 2", 16))
                            .fixedHeight_(20),
                    ),
                    
                    gain = makeSlider.(
                        "out gain",
                        currentEnvironment["gain_%".format(name).asSymbol]
                    ),
                    
                    VLayout(
                        nil,
                        
                        CVGridCell(
                            "above", 
                            currentEnvironment["aboveRatio_%".format(name).asSymbol]
                        )
                        .step_(0.1)
                        .modStep_(0.02)
                        .view,
                        
                        CVGridCell(
                            "below", 
                            currentEnvironment["belowRatio_%".format(name).asSymbol]
                        )
                        .step_(0.1)
                        .modStep_(0.02)
                        .view,
                        
                        CVGridCell(
                            "curve",
                            currentEnvironment["curve_%".format(name).asSymbol]
                        ).view,
                        
                        makeSolo.(
                            bands.indexOfEqual(name),
                            ~solo,
                            currentEnvironment["bypass_%".format(name).asSymbol],
                        ),
                        
                        makePlot.(plotData),
                    )
                )                                 
            ).spacing_(0).margins_(0)
        );
        
        values.connectTo({
            ampValue 			= values[0].linlin(ampMin, ampMax, 0, 1);
            compressionValue 	= values[1].linlin(0, ampMin.neg, 0, 1);
            expansionValue 		= values[1].neg.linlin(0, ampMax, 0, 1);
            postAmpValue 		= values[2].linlin(ampMin, ampMax, 0, 1);
        }).freeAfter(view);
        
        plotDataCalculate = {
            |i|
            var amps = FloatArray.newFrom([plotMin, plotMax].resamp1(128));
            var compress = compressFunc.(
                false,
                amps,
                currentEnvironment["aboveRatio_%".format(name).asSymbol].value,
                currentEnvironment["belowRatio_%".format(name).asSymbol].value,
                ~ratioScale.value,
                currentEnvironment["aboveThreshold_%".format(name).asSymbol].value,
                currentEnvironment["belowThreshold_%".format(name).asSymbol].value,
                ~knee.value,
                currentEnvironment["curve_%".format(name).asSymbol].value,
                ~expandMax.value
            );
            compress = amps - compress[0] + compress[1];
            compress.do { |v, i| plotData[i] = v };
            plotData.changed(\value);
        }.e(currentEnvironment);
        
        [
            currentEnvironment["aboveRatio_%".format(name).asSymbol],
            currentEnvironment["belowRatio_%".format(name).asSymbol],
            ~ratioScale,
            currentEnvironment["aboveThreshold_%".format(name).asSymbol],
            currentEnvironment["belowThreshold_%".format(name).asSymbol],
            ~knee,
            currentEnvironment["curve_%".format(name).asSymbol],
            ~expandMax
        ].do {
            |cv|
            cv.signal(\value).connectTo(plotDataCalculate).collapse(0.1).freeAfter(view);
        }; 
        
        view;
    };
    
    makeMultiSlider = {
        |name, cvHi, cvLo|
        var slider, view;
        
        slider = RangeSlider()
            .maxWidth_(60)
            .canFocus_(false);            
        
        cvLo.signal(\input).connectTo(slider.valueSlot(\lo));
        cvHi.signal(\input).connectTo(slider.valueSlot(\hi));
        slider.signal(\lo).connectTo(cvLo.inputSlot);
        slider.signal(\hi).connectTo(cvHi.inputSlot);
        
        View().layout_(VLayout(
            makeNumberView.(name, cvHi),
            StackLayout(
                makeVLabel.(name),
                slider,
            ).mode_(\stackAll),
            makeNumberView.(name, cvLo),
        ).spacing_(0).margins_(0))
        
    };
    
    makeSlider = {
        |name, cv|
        var slider, view;
        slider = Slider()
            .maxWidth_(60)
            .canFocus_(false);            
        
        cv.signal(\input).connectTo(slider.valueSlot);
        slider.signal(\value).connectTo(cv.inputSlot);
        
        View().layout_(VLayout(
            numberBoxHeight,
            StackLayout(
                makeVLabel.(name),
                slider,
            ).mode_(\stackAll).spacing_(0).margins_(0),
            HLayout(
                nil,
                makeNumberView.(name, cv)
            )
        ).spacing_(0).margins_(0))
    };
    makeMeter = {};
    
    divider = {
        View().fixedWidth_(2).background_(Color.grey);
    };
    
    view = View(bounds:400@200).layout_(HLayout(
        makeSlider.("pre gain", ~preGain), divider.(),
        
        makeBandView.("a", bandMeters[0]), divider.(),
        makeBandView.("b", bandMeters[1]), divider.(),
        makeBandView.("c", bandMeters[2]), divider.(),
        makeBandView.("d", bandMeters[3]), divider.(),
        
        VLayout(
            HLayout(
                UserView().fixedWidth_(40).animate_(true).drawFunc_({
                    |v|
                    var bounds = v.bounds.moveTo(0, 0);
                    Pen.scale(1/3, 1);
                    
                    Pen.fillColor = Color.hsv(0.4, 0.5, 0.8);
                    Pen.fillRect(Rect(
                        0,
                        bounds.height * (1.0 - outputAmp),
                        bounds.width,
                        bounds.height * outputAmp
                    ).insetBy(1, 1));
                }),
                makeSlider.("post gain", ~gain),
            ),
            CVGridCell(
                "knee",
                ~knee,
            ).view,
            CVGridCell(
                "ratio *",
                ~ratioScale,
            ).view,
            CVGridCell(
                "attack",
                ~attack,
            ).view,
            CVGridCell(
                "decay",
                ~decay,
            ).view,
            
            makeBalanceView.(),
            
            ToolBar(
                limit = MenuAction("limiter").checkable_(true),
            ),    
            
            ToolBar(
                bypassAll = MenuAction("bypass all").checkable_(true),
            ),    
        ),
        
        nil
    ));
    
    ~bypassAll.postln.signal(\value).connectTo(bypassAll.methodSlot("checked_(value > 0)")).freeAfter(view);
    bypassAll.signal(\toggled).connectTo(~bypassAll.methodSlot("value_(value.if(1, 0))")).freeAfter(view);
    
    ~limit.signal(\value).connectTo(limit.methodSlot("checked_(value > 0)")).freeAfter(view);
    limit.signal(\toggled).connectTo(~limit.methodSlot("value_(value.if(1, 0))")).freeAfter(view);
    
    oscDef = OSCdef("compressor_%".format(name).asSymbol, Routine({
        |msg|
        var newBalanceMeter, newPeak, lastPeak = 0, lastPeakTime = 0;
        newBalanceMeter = balanceMeter.copy;
        
        inf.do {
            msg = msg[3..];
            
            outputAmp = msg.pop.linlin(ampMin, ampMax, 0, 1);
            
            msg.clump(4).do {
                |v, i|
                bandMeters[i][0] = v[0];
                bandMeters[i][1] = v[1];
                bandMeters[i][2] = v[2];
                newBalanceMeter[i] = v[3].dbamp.max(-60.dbamp);
            };
            
            newPeak = newBalanceMeter.sum;
            if (newPeak > lastPeak or: { (thisThread.seconds - lastPeakTime) > 1 }) {
                newBalanceMeter.do({ |v, i| balanceMeter[i] = v });
                lastPeak = newPeak;
                lastPeakTime = thisThread.seconds;
                balanceMeter = balanceMeter.normalizeSum();            
            };
            
            bandMeters.do(_.changed);
            msg = 0.yield;
        }
    }), "/compressor").permanent_(true);
    
    view.onClose = view.onClose.addFunc({
        oscDef.clear;
    });
    
    view.name = name;
    view.front;
};

e = currentEnvironment;
Pdef(\compressor, {
    var path = thisProcess.nowExecutingPath;
    var name = (~name !? ("%_eqcomp".format(_)) ?? { "eqcomp" });
    var replyId = UniqueID.next;
    var channels = ~channels ?? { 2 };
    
    "CREATING COMPRESSOR: %".format(replyId).postln;
    "  CHANNELS: %".format(channels).postln;
    
    if (~c.envir.size == 0) {
        "INITIALIZING COMPRESSOR".postln;
        
        ~c.guiFunc = {
            guiFunc.(name, replyId).autoRememberPosition(name.asSymbol);
        };
        
        ~c.setSpecs(\compressor.asSynthDesc.specs);
    };
    
    ~c.storeCurrent(name);
    
    Pbind.mono(
        \instrument, "compressor_%ch".format(channels).asSymbol,
        \dur, 16,
        \replyId, replyId,
        *(~c.asSynthMapArgs)
    )
});

)

