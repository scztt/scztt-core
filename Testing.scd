
(
//Setup GUI
a = Slider();
b = Slider();

w = Window(bounds:Rect(200,200,200,100)).layout_(
    HLayout( StaticText().string_("In:"), a, StaticText().string_("Out:"), b )
);

w.front;
)
TraceView().front;
//start event graph
(
ENdef(\graph1,{
    
    //in
    var aSig = a.enIn;
    
    //try changing the function in the collect method
    //and re-evaluate the ENdef, the gui behaviour will update.
    var sig = aSig.collect{ |x| 1.0 - x };
    
    //out
    b.enSinkValue( sig );
    
}).start;

Archive.at(\WindowPositions).inspect;

QtGUI.applicationName = "asdfasdfasdf";
)



(
~selector = EventSource();
//two sources
a = EventSource();
b = EventSource();
//the result
w = ~selector.switchTo{ |v|
    if(v==0){a}{b}
};
w.do(_.postln);
)
(
//will get value from y
~selector.fire(0);
a.fire(1);
b.fire(4);
)
(
//will get value from z
~selector.fire(1);
a.fire(1);
b.fire(4);



(
    (
        
        ~slider = Slider().value_(0.3).front;
        //network
        ~networkDescription = ENDef({
            //inputs
            var slSig = ~slider.postln.enIn;
            var r = slSig.collect(_*100);
            
            r.collect{ |v| putStrLn(v) }.enOut;
            
        });
        //compile network
        ~network = EventNetwork(~networkDescription, true);
        
        //start network
        ~network.start;
        
    )
)

(
    DoNotation.activate;
    //inputs
    
    x = EventSource();
    y = Var(1.0);
    //collected all the reactimates
    ~networkDescription = Do(
        x <- x.asENInput;
        y <- y.asENInput;
        //outputs
        x.collect{ |v| putStrLn(" es out : "++v) }.reactimate;
        y.collect{ |v| putStrLn(" var out : "++v) }.reactimate
    );
    //compile network
    ~network = EventNetwork(~networkDescription);
    //start network
    ~network.actuate;
)

(
    //Setup GUI
    var sl1 = Slider();
    var sl2 = Slider();
    var sl3 = Slider();
    w = Window(bounds:Rect(200,200,200,200)).layout_(
        HLayout( sl1, sl2 )
    );
    
    //Setup Event Network
    //network
    ~networkDescription =
        //inputs
        sl1.asENInput >>= { |x|
            var r = x.collect(_*100);
            r.collect{ |v| putStrLn(v) }.reactimate >>=|
                x.collect{ |v| 1-v }.connectEN(sl2) >>=|
                x.collect{ |v| sl3.setPropIO(\moveTo, 100+(v*200), 300) }.reactimate;
        };
    
    //compile network
    ~network = EventNetwork(~networkDescription);
    ~network.actuate;
    w.front.frontIO;
    sl3.frontIO;
)
