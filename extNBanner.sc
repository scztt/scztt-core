+String {
    banner {
        |character="—", width=300, height=1, left=16, charWidth=116|
        var target = (height - 2).max(0);
        var characterFunc, uniSize, line;
        
        uniSize = this.unicodeSize;
        
        if (character.isKindOf(Collection)) {
            characterFunc = { |i| character.unicodeWrapAt(i) } 
        } {
            characterFunc = character
        };
        
        if (height == 2) {
            target = 1;
        };
        
        height.do {
            |line|
            if (line == target) {
                width.collect {
                    |char|
                    if ((char < left) or: { char > (left + uniSize - 1) }) {
                        character.(char, line).asString
                    } {
                        this.unicodeAt(char - left).asString;
                    }
                }.join("").postln
            } {
                width.collect {
                    |char|
                    character.(char, line).asString
                }.join("").postln
            }
        }
    }
    
    unicodeSize {
        var size = this.size;
        size = size - (this.select({ |c| c.ascii < 0 }).size / 3 * 2).asInteger;
        ^size;
    }
    
    unicodeIndex {
        |index|
        var i = 0;
        var char = 0;
        while { char < index } {
            if (this[i].ascii < 0) {
                i = i + 3;
            } {
                i = i + 1;
            };
            char = char + 1;
        };
        ^i;
    }
    
    unicodeAt {
        |at|
        var i = this.unicodeIndex(at);
        if (this[i].ascii < 0) {
            ^this[i..(i+2)]
        } {
            ^this[i]
        }
    }
    
    unicodeWrapAt {
        |i|
        ^this.unicodeAt(i % this.unicodeSize)
    }
    
    unicodeCollect {
        |func|
        var i = 0, j = 0, size = this.size;
        var replacement, result;
        result = CollStream("");
        
        while { i < size } {
            if (this[i].ascii < 0) {
                result.putString(func.value(this[i], j).asString);
                i = i + 1;
            } {
                result.putString(func.value(this[i..(i+2)], j).asString);
                i = i + 3;
            };
            j = j + 1;
        }
        
        ^result.collection
    }
}
