+ControlValueEnvir {
    // gui {
    //     |decorateFunc, cvDecorateFunc|
    //     var buildFunc, parentView, view, slider, number, label, font, lastPrefix;
    
    //     if (guiFunc.notNil) {
    //         ^this.use({ guiFunc.() }).front;
    //     };
    
    //     view = View(bounds:400@300);
    
    //     font = Font("Mplus 2", 16);
    
    //     buildFunc = {
    //         view.removeAll();
    //         view.layout = VLayout().spacing_(0);
    
    //         envir.asAssociations.sort.do {
    //             |assoc|
    //             var prefix, name, cv, subview, decimals;
    
    //             prefix = ControlValueEnvir.splitPrefix(assoc.key);
    //             name = ControlValueEnvir.removePrefix(prefix, assoc.key);
    //             cv = assoc.value;
    
    //             decimals = max(
    //                 (cv.spec.map(0) - cv.spec.map(0.02)).abs.reciprocal.log10.asInteger,
    //                 (cv.spec.map(0.98) - cv.spec.map(1)).abs.reciprocal.log10.asInteger
    //             ) + 1;
    
    //             if (prefix != lastPrefix) {
    //                 if (lastPrefix.notNil) {
    //                     view.layout.add(16)
    //                 };
    
    //                 lastPrefix = prefix;
    //                 view.layout.add(
    //                     View().layout_(VLayout(
    //                         StaticText()
    //                             .string_(prefix)
    //                             .align_(\right)
    //                             .fixedWidth_(120)
    //                             .font_(Font(size:16, bold:true)),
    //                         4
    //                     ).margins_(4))
    //                 )
    //             };
    
    //             subview = View().layout_(HLayout(
    //                 label = StaticText().string_(name).fixedWidth_(120).font_(font).align_(\right),
    //                 number = if (
    //                     cv.spec.isKindOf(ItemSpec), 
    //                     {
    //                         TextField().fixedWidth_(80).font_(font).align_(\right)
    //                     }, 
    //                     {
    //                         NumberBox().fixedWidth_(80).font_(font).align_(\right).decimals_(decimals).minDecimals_(decimals)
    //                     }
    //                 ),
    //                 slider = Slider().orientation_(\horizontal),
    //             ).margins_(2));
    
    //             subview = cvDecorateFunc.(prefix, name, cv, subview) ?? { subview };
    
    //             [
    //                 cv.signal(\value).connectTo(number.valueSlot),
    //                 cv.signal(\input).connectTo(slider.valueSlot),
    //                 number.signal(\value).connectTo(cv.valueSlot),
    //                 slider.signal(\value).connectTo(cv.inputSlot),
    //             ].freeAfter(subview);
    
    //             view.layout.add(subview)
    //         };
    
    //         view.layout.add(nil);
    
    //         decorateFunc.value(view) ? view;
    //     };
    
    //     this.signal(\controls).connectToUnique({
    //         buildFunc.()
    //     }).freeAfter(view);
    
    //     ^buildFunc.().front;
    // }
    
    prGuiMakeSlider {
        |cv, style|
        var dragStartPos, dragStartValue;
        var color = cv.md[\color] ?? { cv.group !? _[\color] } ?? { Color.rand };
        var v = UserView() 
            setAll: (
                minWidth: 200,
                
                mouseDownAction: {
                    |v, x, y|
                    dragStartPos = x@y;
                    dragStartValue = cv.input;
                },
                mouseMoveAction: {
                    |v, x, y|   
                    var diff;
                    if (dragStartPos.notNil) {
                        diff = (x@y) - dragStartPos;
                        cv.input = dragStartValue + (diff.x / v.bounds.width)
                    }
                },
                mouseWheelAction: {
                    |v, x, y, scrollX, scrollY|
                    cv.input = cv.input + (scrollY * (cv.input / 100.0))
                }
            )
            .drawFunc_({
                |v|
                var bounds = v.bounds.moveTo(0, 0);
                var sliderBounds;
                var zero = cv.spec.unmap(0) ?? { 0 };
                var value = cv.input;
                
                sliderBounds = Rect.newSides(
                    bounds.width * min(zero, value),
                    0,
                    bounds.width * max(zero, value),
                    bounds.height
                ).insetBy(1, 1);
                
                Pen.fillColor = color.darken(0.3);
                Pen.fillRect(bounds);
                
                Pen.fillColor = color.darken(0.6);
                Pen.fillRect(sliderBounds);
                
                Pen.strokeColor = color.darken(0.7);
                Pen.strokeRect(bounds);        
            });
        
        cv.signal(\value).connectTo(v.methodSlot("refresh")).freeAfter(v);
        ^v;
    }
    
    prGuiMakeTextDisplay {
        |cv, style|
        var v = TextField()
            .fixedWidth_(80)
            .font_(style.font).
            align_(\right);
        
        cv.signal(\value).connectTo(v.valueSlot).freeAfter(v);
        
        ^v
    }
    
    prGuiMakeNumber {
        |cv, style|
        
        var decimals = max(
            (cv.spec.map(0) - cv.spec.map(0.02)).abs.reciprocal.log10.asInteger,
            (cv.spec.map(0.98) - cv.spec.map(1)).abs.reciprocal.log10.asInteger
        ) + 1;
        
        var v = NumberBox()
            setAll: (
                align:          \right,
                font:           style.font.copy.weight_(70),
                scroll_step:    cv.spec.range.abs / 100,
                clipHi:         cv.spec.maxval,
                clipLo:         cv.spec.minval,
                decimals:       decimals,
                maxDecimals:    decimals,
                background:     Color.clear,
                fixedWidth:     80
            );
        
        v.signal(\value).connectTo(cv.valueSlot).freeAfter(v);
        cv.signal(\value).connectTo(v.valueSlot).freeAfter(v);
        
        ^v
    }
    
    prGuiMakeLabel {
        |name, style, heading=false|
        var font = style.font.copy
            .weight_(heading.if(60, 10))
            // .capitalization_(\smallCaps)
            ;
        var color = Color.grey(0.9);
        var stringRect = QtGUI.stringBounds(name, font);
        var lineY = stringRect.height - (font.size / 2);
        var spacing = 4;
        var lineX1 = 40;
        var lineX2 = lineX1 + spacing + stringRect.width + spacing;
        
        if (heading) {
            ^UserView().fixedHeight_(30).drawFunc_({
                |v|
                var width = v.bounds.width;
                Pen.strokeColor = color;
                
                Pen.line(0@lineY, lineX1@lineY);
                Pen.stroke();
                
                Pen.stringAtPoint(name, stringRect.moveBy(lineX1 + spacing, 0).leftTop, font, color);
                Pen.stroke();
                
                Pen.line(lineX2@lineY, width@lineY);
                Pen.stroke();
            }) 
        } {
            ^StaticText()
                setAll: (
                    font:           style.font.copy
                        .weight_(heading.if(60, 10))
                        // .capitalization_(\smallCaps)
                        ,
                    align:          heading.if(\left, \right),
                    stringColor:    Color.grey(0.9),
                    string:         "%: ".format(name),
                )                
        }
    }
    
    gui {
        |decorateFunc, cvDecorateFunc|
        var buildFunc, parentView, view, slider, number, label, font, lastPrefix;
        var controlLayout;
        
        var style = (
            font:       { Font("M Plus 2", 17).weight_(10) },
            highlight:  { Color.hsv(0.5, 0.7, 1.0) },
            rowHeight:  26, 
        );
        
        if (guiFunc.notNil) {
            ^this.use({ guiFunc.(decorateFunc, cvDecorateFunc) }).front;
        };
        
        view = View(bounds:400@300);
        
        buildFunc = {
            view.children.do(_.remove);
            
            view.layout = VLayout(
                [controlLayout = VLayout(), stretch:1],
            ).spacing_(1);
            
            envir.asAssociations.sort.do {
                |assoc|
                var prefix, name, cv, subview;
                
                prefix = ControlValueEnvir.splitPrefix(assoc.key);
                name = ControlValueEnvir.removePrefix(prefix, assoc.key);
                cv = assoc.value;
                
                if (prefix != lastPrefix) {
                    if (lastPrefix.notNil) {
                        controlLayout.add(16)
                    };
                    
                    lastPrefix = prefix;
                    controlLayout.add(this.prGuiMakeLabel(prefix, style, heading:true));
                    controlLayout.add(4);                    
                };
                
                subview = View()
                    .fixedHeight_(style.rowHeight)
                    .layout_(HLayout(
                        label = this.prGuiMakeLabel(name, style)
                            .fixedWidth_(140),
                        number = if (
                            cv.spec.isKindOf(ItemSpec), 
                            {
                                this.prGuiMakeTextDisplay(cv, style);
                            }, 
                            {
                                this.prGuiMakeNumber(cv, style);
                            }
                        ),
                        slider = this.prGuiMakeSlider(cv, style),
                    )
                    .spacing_(0)
                    .margins_(2));
                
                subview = cvDecorateFunc.(prefix, name, cv, subview) ?? { subview };
                
                controlLayout.add(subview);
            };
            
            controlLayout.add(nil);
        };
        
        buildFunc.();
        
        this.signal(\controls).connectToUnique({
            buildFunc.();
        }).freeAfter(view);
        
        ^(decorateFunc.value(view) ? view).front;
    }
    
}






















