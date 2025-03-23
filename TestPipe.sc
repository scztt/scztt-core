// TestPipe : UnitTest {

// 	test_basic_pipe {
// 		if (thisProcess.platform.isKindOf(UnixPlatform)) {
// 			var pipe = Pipe.argv(["date", "+%H"], "r");
// 			var result = pipe.getLine();
// 			pipe.close();

// 			if (result.isKindOf(String)) {
// 				this.assert("^[0-9]+$".matchRegexp(result), "Result was a number");
// 			} {
// 				this.assert(false, "Failed to get result")
// 			}
// 		}
// 	}

// 	test_bidirectional_pipe {
// 		if (thisProcess.platform.isKindOf(UnixPlatform)) {
// 			var in, out, result;
// 			var testString = "I will find 'findme' in this string.";

// 			//////////////////////////////////////////////////////////////////////////////////////////////////////
// 			#in, out = Pipe.argvReadWrite(["grep", "findme"]);

// 			if (out.isOpen) {
// 				out.putString(testString);
// 				out.close();
// 			} {
// 				this.assert(false, "output pipe was not opened")
// 			};

// 			if (in.isOpen) {
// 				result = in.getLine();
// 			} {
// 				this.assert(false, "input pipe was not opened")
// 			};

// 			this.assertEquals(result, testString);

// 			//////////////////////////////////////////////////////////////////////////////////////////////////////
// 			#in, out = Pipe.argvReadWrite(["date", "+%H"]);
// 			result = in.getLine();
// 			out.close();

// 			if (result.isKindOf(String)) {
// 				this.assert("^[0-9]+$".matchRegexp(result), "Result was a number");
// 			} {
// 				this.assert(false, "Failed to get result")
// 			}
// 		}
// 	}
// }

+True {
    iff {
        |trueFunc|
        ^trueFunc.value
    }
}

+False {
    iff {
        |trueFunc, falseFunc|
        ^falseFunc.value
    }
}

+UGen {
    iff {
        |trueFunc, falseFunc|
        ^this.if(trueFunc, falseFunc)
    }
}