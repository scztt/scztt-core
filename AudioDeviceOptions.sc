AudioDeviceConfig : Singleton {
    classvar defaultConfigs, defaultTag, <initialized=false;
    
    var <options, conditionFunc=true, basedOn;
    
    *initClass {
        Class.initClassTree(Singleton);
        Class.initClassTree(Archive);
        
        initialized = true;
        
        defaultConfigs = Archive.at(\AudioDeviceConfig, \defaultConfigs) ?? { IdentitySet[\current, \default] };
        
        Archive.at(\AudioDeviceConfig, \instances, \default) ?? {
            Archive.put(\AudioDeviceConfig, \instances, \default, (
                options: (),
                basedOn: []
            ));
        };
        
        Archive.at(\AudioDeviceConfig, \instances, \current) ?? {
            Archive.put(\AudioDeviceConfig, \instances, \current, (
                options: (),
                basedOn: [\default]
            ));
        };
        
        ShutDown.add({
            Archive.put(\AudioDeviceConfig, \defaultConfigs, defaultConfigs);
            AudioDeviceConfig.all.keysValuesDo {
                |name|
                AudioDeviceConfig(name).prArchiveWrite();
            };
        })
    }
    
    *addDefault {
        |name|
        defaultConfigs.add(name)
    }
    
    *makeDefault {
        defaultConfigs.do {
            |name|
            "config '%' is %".format(name, AudioDeviceConfig(name).isUsable.if("usable", "not usable")).postln;
            if (AudioDeviceConfig(name).isUsable) {
                ^AudioDeviceConfig(name).prSynthesizeOptions()
            }
        };
        "No appropriate defailts found".warn;
        ^();
    }
    
    init {
        conditionFunc = {
            |devices, options|
            var inDevice = options[\inDevice];
            var outDevice = options[\outDevice];
            
            (
                inDevice !? { devices.includesEqual(inDevice) } ?? true 
            ) and: {
                outDevice !? { devices.includesEqual(outDevice) } ?? true
            }
        };
        
        this.prArchiveRead();
    }
    
    prArchiveWrite {
        "writing to archive for %".format(this.name).postln;
        Archive.put(\AudioDeviceConfig, \instances, this.name, \options, options);
        Archive.put(\AudioDeviceConfig, \instances, this.name, \basedOn, basedOn);
    }
    
    prArchiveRead {
        "reading from archive for %".format(this.name).postln;
        Archive.at(\AudioDeviceConfig, \instances, this.name).postln;
        options = Archive.at(\AudioDeviceConfig, \instances, this.name, \options) ?? { () };
        basedOn = Archive.at(\AudioDeviceConfig, \instances, this.name, \basedOn) ?? { [] };
    }
    
    set {
        |inOptions|
        if (inOptions.notNil) {
            options = inOptions ?? { () };
            "options for % are now %".format(name, options).postln;
        }
    }
    
    condition_{
        |function|
        conditionFunc = function;
    }
    
    isUsable {
        ^conditionFunc.value(ServerOptions.devices, this.prSynthesizeOptions)
    }
    
    basedOn {
        |name|
        if (name.isKindOf(String)) {
            name = name.asSymbol
        };
        
        basedOn = name !? _.asArray;
    }
    
    asServerOptions {
        var serverOptions, finalOptions = this.prSynthesizeOptions();
        
        if (not(conditionFunc.value(ServerOptions.devices, finalOptions))) {
            "ServerOptions from '%' may be missing audio devices".format(this.name).warn;
        };
        
        serverOptions = ServerOptions ();
        finalOptions.keysValuesDo { |key, val| serverOptions.instVarPut(key, val) };
        
        ^serverOptions
    }
    
    prSynthesizeOptions {
        |stack(IdentitySet())|
        var o = ();
        
        "synthesizing options for %".format(this.name).postln;
        
        if (basedOn.notNil) {
            if (stack.includes(this.name)) {
                Error("Recursion detected in AudioDeviceOptions:basedOn (%, %)".format(this.name, stack)).throw
            };
            
            stack = stack.add(this.name);
            
            basedOn.asArray.do {
                |based|
                o.putAll(
                    AudioDeviceConfig(based).prSynthesizeOptions(stack.copy);
                );
                
                "injected options from: %".format(based).postln;
            };
        };
        
        o.putAll(options);
        
        "Final options are: %".format(o).postln;
        ^o
    }
}

+ServerOptions {
    putAll {
        |dict|
        dict.keysValuesDo { 
            |key, val| 
            Server.internal.options.instVarPut(key, val);
            Server.local.options.instVarPut(key, val);
        }
    }
}
