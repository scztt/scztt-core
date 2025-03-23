PComposeType : Pfunc {
    *new { 
        |typeFunc|
        
        if (typeFunc.isKindOf(Symbol)) {
            ^this.new({
                |server, inner|
                inner.();
                ~playEventType.(typeFunc);
            })
        };
        
        ^super.newCopyArgs({
            |event|
            var oldType = event[\type];            
            event[\parentType] = event[\parentType] ?? { oldType } ?? { \note };
            event[\type] = {
                |server ...args|
                typeFunc.value(server, { ~playEventType.(oldType) }, *args);
            };
            event;
        })
    }
}
