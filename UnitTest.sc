UnitTest2 {
	classvar inTestContext = false;

	var <name, <>before, <>after, <>test, <>defaults;

	*new {
		|...args|
		var name, func, unittest;

		if (args.isEmpty) { Exception("First argument must be a name or a function.").throw };
		if (args[0].isKindOf(Function)) {
			name = "";
			func = args[0];
		} {
			name = args[0];
			func = args[1];
		};

		^super.newCopyArgs(name, func);
	}

	unpackParams {
		|params|
		var paramNames, paramValues, results;

		params = (defaults ?? ()).copy.putAll(params ?? ());

		paramNames = params.keys.asList;
		paramValues = paramNames.collect(params.at(_));

		results = paramValues.allTuples;
		results = results.collect {
			|result|
			paramNames.collectAs({
				|name, i|
				name -> result[i]
			}, Event)
		};

		^results;
	}

	runOrAdd {
		if (inTestContext.not) {
			^UnitTestDriver.runOneTest(this).report();
		} {
		}
	}
}

Test {
	classvar <currentTest;
	var <unitTest;
	var <description, <creator,
	<timeout, <results, expectedTests, asyncConditions;
	var <>params;

	*currentTest_{
		|test|

		if (test.notNil && currentTest.notNil) {
			currentTest = nil;
			Exception("Cannot execute a test inside of another test.").throw();
		};

		"currentTest set to: %".format(test).postln;
		currentTest = test;
	}

	*new {
		|unitTest, params|
		^super.newCopyArgs(unitTest).init().params_(params ?? ());
	}

	init {
		results = List();
		asyncConditions = List();
	}

	*withCurrent {
		|action ...args|

		if (currentTest.isNil) {
			"No current test using a temporary one.".warn;
			currentTest = Test(nil);
			protect {
				^currentTest.perform(action, *args).report;
			} {
				currentTest = nil;
			}
		} {
			^currentTest.perform(action, *args)
		}
	}

	*params { ^this.withCurrent(\params) }
	*assert { | test, msg="" | ^this.withCurrent(\assert, test, msg) }
	*equals { | a, b, msg="" | ^this.withCurrent(\equals, a, b, msg) }
	*identical { | a, b, msg="" | ^this.withCurrent(\identical, a, b, msg) }
	*approxEquals { | a, b, threshold = 0.001, msg="" | ^this.withCurrent(\approxEquals, a, b, threshold, msg) }
	*isTrue { |test, msg| ^this.withCurrent(\isTrue, test, msg) }
	*isFalse{ |test, msg| ^this.withCurrent(\isFalse, test, msg) }
	*throws { | func, error=(Exception), msg=nil | ^this.withCurrent(\throws, func, error, msg) }
	*noError { |func, msg| ^this.withCurrent(\noError, func, msg) }
	*isNil { |...args| ^this.withCurrent(\isNil, *args) }
	*isNotNil { |...args| ^this.withCurrent(\isNotNil, *args) }
	*canSet { |...args| ^this.withCurrent(\canSet, *args) }
	*canGetAndSet { |...args| ^this.withCurrent(\canGetAndSet, *args) }
	*checkIsKindOf { |...args| ^this.withCurrent(\checkIsKindOf, *args) }
	*finishes { |...args| ^this.withCurrent(\finishes, *args) }
	*finishesIn1 { |...args| ^this.withCurrent(\finishesIn1, *args) }
	*finishesIn10 { |...args| ^this.withCurrent(\finishesIn10, *args) }
	*finishesIn30 { |...args| ^this.withCurrent(\finishesIn30, *args) }

	*skip { |...args| ^this.withCurrent(\skip, *args) }

	logTest {
		| kind, pass, msg, skipped = false|
		var result = TestResult(kind, msg, pass.asBoolean, skipped);
		results.add(result);
		^result;
	}

	timeout_{}
	withServer {}

	assert {
		| test, msg="" |
		var result;

		if (test.isKindOf(Function)) {
			if (msg.isEmpty) { msg = test.def.sourceCode };
			test = test.value;
		};

		^this.logTest(\assert, test, msg);
	}

	identical {
		| a, b, msg="" |
		msg = msg + "% === %".format(a, b);
		^this.logTest(\identical, (a === b), msg);
	}


	equals {
		| a, b, msg="" |
		msg = msg + "% == %".format(a, b);
		^this.logTest(\equals, (a == b), msg);
	}

	approxEquals {
		| a, b, within = 0.001, msg="" |
		msg = msg + "% ~== %".format(a, b);
		^this.logTest(\approxEquals, (a - b).abs <= within, msg);
	}

	arrayApproxEquals {
		| a, b, within = 0.001, msg="" |
		var result = (a.class == b.class) and: {a.size == b.size};
		msg = msg + "% ~== %";
		if (result) {
			result = a.detectIndex({
				|aValue, i|
				(aValue - b[i]).abs > within
			}).notNil;
		};
		^this.logTest(\arrayApproxEquals, result, msg);
	}

	isTrue {
		|test, msg|
		^this.logTest(\isTrue, test, msg);
	}

	isFalse {
		|test, msg|
		^this.logTest(\isTrue, test.not, msg);
	}

	noError {
		|func, msg|
		var error;
		if (msg.isNil) { msg = "Function completes without error '%'.".format(error.asString()) };
		func.protect {
			|e|
			error = e;
			msg = msg + (error ?? "").asString;
		};
		this.logTest(\noError, error.isNil, msg);
	}

	throws {
		| func, error=(Exception), msg=nil |
		var actualError;
		if (msg.isNil) { msg = "Function throws '%'.".format(error.asString()) };

		try {
			func.value();
		} {
			|e|
			"error: %".format(e).postln;
			actualError = e;
		};

		^this.logTest(\throws, actualError.isKindOf(error), msg)
	}

	notNil {
		|obj, msg|
		if (msg.isNil) { msg = "Object '%' is not nil" };
		^this.logTest(\notNil, obj.notNil, msg);
	}

	isNil {
		|obj, msg|
		if (msg.isNil) { msg = "Object '%' is nil" };
		^this.logTest(\isNil, obj.isNil, msg);
	}

	canSet {
		|obj, property, value, test, msg|

		if (msg.isNil) { msg = "Object '%' can set property '%' to value '%'.".format(obj, property, value) };
		if (test.isNil) { test = { true } };

		obj.perform(property.asSymbol.asSetter, value);

		^this.logTest(\canSet, test.value(obj), msg);
	}

	canGetAndSet {
		|obj, property, value, test, msg|
		var getter, setter, oldValue, newValue;
		if (msg.isNil) { msg = "Object '%' can get and set property '%' to value '%'.".format(obj, property, value) };
		if (test.isNil) { test = {|a, b| a == b} };

		getter = property.asSymbol;
		setter = getter.asSetter;

		oldValue = obj.perform(getter);
		obj.perform(setter, value);
		newValue = obj.perform(getter);
		obj.perform(setter, oldValue);

		^this.logTest(\canGetAndSet, test.value(value, newValue), msg + "(result: %)".format(newValue));
	}

	checkIsKindOf {
		|obj, kind, msg|
		if (msg.isNil) { msg = "Object '%' is a kind of %.".format(obj, kind) };

		^this.logTest(\checkIsKindOf, obj.isKindOf(kind), msg);
	}


	finishes {
		| func, timeout = 60, msg |
		var result;
		var doneFunc, timeoutFunc, condition = Condition(), finished = false, error;

		if (msg.isNil) { msg = "Async call finished correctly." };

		doneFunc = {
			if (finished.not) {
				finished = true;
				result = this.logTest(\finishes, true, msg);
			};
			condition.test = true;
			condition.unhang();
		};

		timeoutFunc = {
			if (finished.not) {
				finished = true;
				result = this.logTest(\finishes, false, msg);
			};
			condition.test = true;
			condition.unhang();
		};

		timeoutFunc.defer(timeout);
		func.value(doneFunc);
		condition.wait();

		^result;
	}

	finishesIn1 {
		|func, msg|
		^this.finishes(func, 1, msg);
	}

	finishesIn10 {
		|func, msg|
		^this.finishes(func, 10, msg);
	}

	finishesIn30 {
		|func, msg|
		^this.finishes(func, 30, msg);
	}

	synthOutput {
		|func, time|
	}

	fail {
		|msg|
		this.logTest(\fail, false, msg);
	}

	success {
		|msg|
		this.logTest(\succcess, true, msg);
	}

	expect {
		| num |
		expectedTests = num;
	}

	dontrun {
		|func, why|
		var msg = "Not running test: %".format(func.asCompileString);
		if (why.notNil) { msg = msg + " (%)".format(why) };
		this.logTest(\dontrun, true, msg, true);
	}

	skip {
		|result, why|
		result.skip(why);
	}
}

UnitTestLegacyWrapper {
	var unitTest;

	*new {
		|unitTest|
		^super.newCopyArgs(unitTest);
	}
}

TestResult {
	var <>kind='', <>description="", <>pass=false, <>skipped=false, skipReason;

	*new { |...args| ^super.newCopyArgs(*args) }

	skip {
		|why|
		skipped = true;
		skipReason = why;
	}

	report {
		"\n\tkind: %\n\tdescription: %\n\tpass: %\n\tskipped: %".format(
			this.kind,
			this.description,
			this.pass,
			this.skipped
		).postln;
	}
}

UnitTestDriver {
	classvar <lastRun;
	var tests, running;

	*runOneTest {
		|unitTest, params|
		var driver, unpackedParams;

		driver = UnitTestDriver();

		unpackedParams = unitTest.unpackParams(params);
		unpackedParams.do {
			|paramSet|
			driver.add(unitTest, paramSet);
		};

		"running one test (% permutations)".format(unpackedParams.size).postln;
		driver.run();
		^driver;
	}

	*new { ^super.new.init() }

	init {
		tests = List();
		running = false;
	}

	add {
		|test, paramSet|
		tests.add([test, paramSet]);
	}

	run {
		var testEnvironment, finished = Condition();

		running = true;

		fork({
			lastRun = tests.collect {
				|item|
				var testDefinition, paramSet;
				var test;
				var testFinished = Condition();

				#testDefinition, paramSet = item;

				Test.currentTest = test = Test(testDefinition, paramSet);

				testEnvironment = Environment();
				testEnvironment.push();

				fork({
					try {
						protect {
							testDefinition.before.value(test);
							testDefinition.test.value(test);
						} {
							testDefinition.after.value(test);
						}
					} {
						|e|
						e.reportError;
						test.results.add(TestResult(\error, e.errorString));
					};
					testFinished.unhang();
				}, AppClock);

				testFinished.wait();
				testEnvironment.pop();

				this.report(test.results);

				Test.currentTest = nil;
				test;
			};

			finished.unhang();
		}, AppClock);

		running = false;

		^finished
	}

	report {
		|results|
		results.do {
			|r|
			r.report();
		};
	}
}

/*UnitTest {

	var currentMethod;
	classvar <failures, <passes, routine, <>reportPasses = true;
	classvar <allTestClasses;

	*findTestClasses {
		allTestClasses = UnitTest.allSubclasses.collectAs({ |c|
			var classkey = c.name; // drop Meta_
			var methtests = c.findTestMethods.collectAs({|m|
				m.name.asString -> { c.new.runTestMethod(m) }
			}, Dictionary);
			methtests.add(" run all in this class" -> { c.run });
			classkey -> methtests;
		}, Dictionary);
		// err there may be some empty classes hanging around
		allTestClasses = allTestClasses.reject {|d| d.size == 1 };
		allTestClasses.add("...All..." -> Dictionary["Run all" -> { UnitTest.runAll }]);

	}

	// called before each test
	setUp {}

	// called after each test
	tearDown {}


	// use YourClass.test of TestYourClass.run
	*run { | reset = true, report = true|
		this.new.run(reset, report);
	}

	// run all UnitTest subclasses
	*runAll {
		this.forkIfNeeded {
			this.reset;
			this.allSubclasses.do ({ |testClass|
				testClass.run(false,false);
				0.1.wait;
			});
			this.report;
		}
	}

	// run a single test in the name format "TestPolyPlayerPool:test_prepareChildrenToBundle"
	*runTest { | methodName |
		var class, method, unitTest;
		# class, method = methodName.split($:);
		class = class.asSymbol.asClass;
		method.asSymbol;
		method = class.findMethod(method.asSymbol);
		if(method.isNil) { Error("Test method not found "+methodName).throw };
		class.new.runTestMethod(method);
	}

	// run a single test method of this class
	runTestMethod { | method |
		var function;
		("RUNNING UNIT TEST" + this.class.name ++ ":" ++ method.name).inform;
		this.class.forkIfNeeded {
			this.setUp;
			currentMethod = method;
			this.perform(method.name);
			this.tearDown;
			this.class.report;
			nil
		}
	}

	*gui {

		// UnitTest GUI written by Dan Stowell 2009.
		var w, classlist, methodlist;

		this.findTestClasses;

		w = Window.new("[UnitTest GUI]", Rect(100, 100, 415, 615), resizable: false);
		w.addFlowLayout;

		StaticText(w, Rect(0,0, 400, 40))
		.string_("Select a category, then a test method, and press Enter")
		.align_(\center);

		classlist = ListView(w, Rect(0,0, 200, 600-40))
		.items_(allTestClasses.asSortedArray.collect(_[0]))
		.action_{|widg|
			methodlist.items_(
				allTestClasses.asSortedArray[widg.value][1].asSortedArray.collect(_[0])
			)
		};

		// nowork: classlist.enterKeyAction_{|widg| methodlist.valueAction_(0)};

		methodlist = ListView(w, Rect(200,40, 200, 600-40));
		methodlist.enterKeyAction_ {|widg|
			allTestClasses.asSortedArray[classlist.value][1].asSortedArray[widg.value][1].value
		};

		classlist.value_(0);
		classlist.doAction; // fills in the right-hand column
		^w.front;

	}



	///////////////////////////////////////////////////////////////////////
	// call these in your test_ methods to check conditions and pass or fail

	assert { | boolean, message, report = true, onFailure |
		if(boolean.not) {
			this.failed(currentMethod, message, report);
			if(onFailure.notNil) {
				{ onFailure.value }.defer;
				Error("UnitTest halted with onFailure handler.").throw;
			};
		} {
			this.passed(currentMethod, message, report)
		};
		^boolean
	}

	assertEquals { |a, b, message = "", report = true, onFailure |
		this.assert( a == b, message + "\nIs:\n\t" + a + "\nShould be:\n\t" + b + "\n", report, onFailure)
	}

	assertFloatEquals { |a, b, message = "", within = 0.0001, report = true, onFailure|
		this.assert( (a - b).abs <= within,
			message + "\nIs:\n\t" + a + "\nShould equal (within range" + within ++ "):\n\t" + b + "\n", report, onFailure);
	}

	assertArrayFloatEquals { |a, b, message = "", within = 0.0001, report = true, onFailure|
		// Check whether all in array meet the condition.
		var results, startFrom;
		a = a.asArray;
		results = if(b.isArray) {
			a.collect {|item, index| (item - b[index]).abs <= within }
		}{
			a.collect {|item, index| (item - b).abs <= within }
		};

		if(results.any(_ == false)) {
			startFrom = results.indexOf(false);
			// Add failure details:
			message = message ++
			"\n% of % items in array failed to match."
			" Displaying arrays from index of first failure"
			" (%) onwards:\n%\n! = \n%\n"
			.format(
				results.count(_ == false),
				results.size,
				startFrom,
				a[startFrom..],
				if(b.isArray) { b[startFrom..] } { b }
			);
			this.failed(currentMethod,message, report);
			if(onFailure.notNil) {
				{ onFailure.value }.defer;
				Error("UnitTest halted with onFailure handler.").throw;
			};
			^false
		}{
			this.passed(currentMethod,message, report)
			^true
		}
	}

	// make a further assertion only if it passed, or only if it failed
	ifAsserts { | boolean, message, ifPassedFunc, ifFailedFunc, report = true|
		if(boolean.not,{
			this.failed(currentMethod,message, report);
			ifFailedFunc.value;
		},{
			this.passed(currentMethod,message, report);
			ifPassedFunc.value;
		});
		^boolean
	}

	// waits for condition with a maxTime limit
	// if time expires, the test is a failure
	wait { |condition, failureMessage, maxTime = 10.0|
		var limit;
		limit = maxTime / 0.05;
		while({
			condition.value.not and:
			{(limit = limit - 1) > 0}
		},{
			0.05.wait;
		});
		if(limit == 0 and: failureMessage.notNil,{
			this.failed(currentMethod,failureMessage)
		})
	}

	// wait is better
	asynchAssert { |waitConditionBlock, testBlock, timeoutMessage = "", timeout = 10|
		var limit;
		limit = timeout / 0.1;
		while {
			waitConditionBlock.value.not and:
			{ (limit = limit - 1) > 0 }
		} {
			0.1.wait;
		};
		if(limit == 0) {
			this.failed(currentMethod,"Timeout:" + timeoutMessage)
		} {
			testBlock.value
		};
	}

	// if already booted, then freeAll and create new allocators
	// if this is called inside a routine, the routine waits until server is booted

	bootServer { | server |
		server = server ? Server.default;
		if(server.serverRunning.not) {
			server.bootSync
		} {
			server.freeAll;
		};
		server.newAllocators; // new nodes, busses regardless
	}

	// call failure directly
	failed { | method, message, report = true |
		var r = UnitTestResult(this, method, message);
		failures = failures.add(r);
		if(report){
			Post << Char.nl << "FAIL:";
			r.report;
			Post << Char.nl;
		};
	}

	// call pass directly
	passed { | method, message, report = true |
		var r = UnitTestResult(this, method, message);
		passes = passes.add(r);
		if(report and: reportPasses) {
			Post << "PASS:";
			r.report;
		};
	}


	// PRIVATE IMPLEMENTATION
	// these are mostly private
	// don't use this directly,
	// use Class.test or TestClass.run

	*runTestClassForClass { | class, reset = true, report = true |
		var testClass;
		if(class.isNil) {
			"No class supplied for testing".die;
		};
		testClass = ("Test" ++ class.name.asString).asSymbol.asClass;
		if(testClass.isNil) {
			("No test class found for " + class).inform;
			^this
		};
		if(testClass.respondsTo(\run).not) {
			("Attempting to run UnitTests on class that is not a subclass of UnitTest"
				+ testClass).error;
			^this
		};
		testClass.run(reset,report)
	}

	*findTestClass { | forClass |
		^("Test" ++ forClass.name.asString).asSymbol.asClass
	}

	*report {
		Post.nl;
		"UNIT TEST.............".inform;
		if(failures.size > 0) {
			"There were failures:".inform;
			failures.do { arg results;

				results.report
			};
		} {
			"There were no failures".inform;
		}
	}

	// private - use YourClass.test or TestYourClass.run

	run { | reset = true, report = true|
		var function;
		if(reset) { this.class.reset };
		if(report) { ("RUNNING UNIT TEST" + this).inform };
		this.class.forkIfNeeded {
			this.findTestMethods.do { |method|
				this.setUp;
				currentMethod = method;
				//{
				this.perform(method.name);
				// unfortunately this removes the interesting part of the call stack
				//}.try({ |err|
				//	("ERROR during test"+method).postln;
				//	err.throw;
				//});

				this.tearDown;
			};
			if(report) { this.class.report };
			nil
		};

	}

	*forkIfNeeded { |function|
		if(thisThread.isKindOf(Routine)) { // we are inside the Routine already
			function.value
		} {
			Routine(function).play(AppClock)
		}
	}

	// returns the methods named test_
	findTestMethods {
		^this.class.findTestMethods
	}
	*findTestMethods {
		^methods.select({ arg m;
			m.name.asString.copyRange(0,4) == "test_"
		})
	}
	*classesWithTests { | package = 'Common'|
		^Quarks.classesInPackage(package).select({ |c| UnitTest.findTestClass(c).notNil })
	}
	*classesWithoutTests { |package = 'Common'|
		^Quarks.classesInPackage(package).difference( UnitTest.classesWithTests(package) );
	}

	// whom I am testing
	*findTestedClass {
		^this.name.asString.copyToEnd(4).asSymbol.asClass
	}
	// methods in the tested class that do not have test_ methods written
	*untestedMethods {
		var testedClass,testMethods,testedMethods,untestedMethods;
		testedClass = this.findTestedClass;
		// what methods in the target class do not have tests written for them ?
		testMethods = this.findTestMethods;
		testedMethods = testMethods.collect({ |meth|
			testedClass.findMethod(meth.name.asString.copyToEnd(5).asSymbol)
		}).reject(_.isNil);
		if(testedMethods.isNil or: {testedMethods.isEmpty},{
			untestedMethods = testedClass.methods;
		},{
			untestedMethods =
			testedClass.methods.select({ |meth| testedMethods.includes(meth).not });
		});
		// reject getters,setters, empty methods
		untestedMethods = untestedMethods.reject({ |meth| meth.code.isNil });
		^untestedMethods
	}
	*listUntestedMethods { arg forClass;
		this.findTestClass(forClass).untestedMethods.do({|m| m.name.postln })
	}
	// private
	*reset {
		failures = [];
		routine.stop;
	}
	s {
		^Server.default; // for convenient translation to/from example code
	}
}*/

// UnitTestResult {
//
// 	var <testClass, <testMethod, <message;
//
// 	*new { |testClass, testMethod, message = ""|
// 		^super.newCopyArgs(testClass ? this, testMethod ? thisMethod, message)
// 	}
//
// 	capture {
// 		|func|
// 		try {
//
// 		}
// 	}
//
// 	report {
// 		var name = if(testMethod.notNil) { testMethod.name } { "unit test result" };
// 		Post << testClass.asString << ":" << name << " - " << message << Char.nl;
// 	}
//
// 	junitReport {
//
// 	}
// }


// scripts may be located next to the class or one folder below
// they should have a unique name and end with "_unittest.scd"
// the scripts are listed under this test class: UnitTestScript

// UnitTestScript mimics the behavior of Method,
// in order to sneak into the anthill without getting eaten

// UnitTestScript : UnitTest {
//
// 	var <>name, <>path;
//
// 	classvar <allScripts;
// 	classvar filePattern = "test_";
// 	classvar scriptDict;
//
//
// 	*new { |name, path|
// 		^super.new.init(name, path)
// 	}
//
// 	init { |argName, argPath|
// 		name = argName;
// 		path = argPath;
// 	}
//
// 	*initClass {
// 		scriptDict = ();
// 		allScripts = List();
// 	}
//
// 	*runTest { | scriptName |
// 		var script;
// 		allScripts ?? { this.findTestScripts };
// 		script = allScripts[scriptName.asSymbol];
// 		if(script.isNil) { ("UnitTestScript: script not found: "+ scriptName ).warn } {
// 			script.runScript
// 		}
// 	}
//
// 	*findTestScripts {
// 		var classPaths;
// 		var func = { |path|
// 			var scriptPaths,fileNames, scriptNames;
// 			scriptPaths = pathMatch(path ++"/" ++ filePattern ++ "*.scd");
// 			scriptPaths = scriptPaths ++ pathMatch(path ++"/*/" ++ filePattern ++ "*.scd");
// 			scriptPaths = scriptPaths.as(Set).as(Array); // remove duplicates
// 			fileNames = scriptPaths.collect(_.basename);
// 			scriptNames = fileNames.collect { |x| x.replace(filePattern, "").asSymbol };
// 			scriptNames.do { |name, i|
// 				var oldPath = scriptDict.at(name);
// 				if(oldPath.notNil and: { oldPath != scriptPaths[i] }) {
// 					Error(
// 						"duplicate script name:\n%\n%\n\npath:%\n\n"
// 						.format(scriptPaths[i], scriptDict[name], path)
// 					);
// 				};
// 				scriptDict.put(name, scriptPaths[i]);
// 				if(oldPath.isNil) { allScripts.add(this.new(name, scriptPaths[i])) };
// 			};
// 		};
//
// 		classPaths = Class.allClasses
// 		.collectAs({ |class| class.filenameSymbol.asString.dirname }, Set);
// 		classPaths.do(func);
// 	}
//
// 	*findTestMethods {
// 		this.findTestScripts;
// 		^allScripts
// 	}
//
// 	runTestMethod { |testScript|
//
// 		testScript.runScript;
//
// 	}
//
// 	runScript {
// 		("RUNNING UNIT TEST SCRIPT" + name ++ " path:" ++ path ++ "\n\n").inform;
// 		this.class.forkIfNeeded {
// 			currentMethod = this;
// 			path.load.value(this);
// 			this.class.report;
// 			nil
// 		}
// 	}
//
// 	run {
// 		allScripts ?? { this.class.findTestScripts };
// 		Routine {
// 			allScripts.do { |testScript|
// 				this.runTestMethod(testScript)
// 			}
// 		}.play(AppClock);
// 	}
//
// }

+Class {
	< { |other|
		^(this.identityHash < other.identityHash)
	}
}

+String {
	*unicodeTest {
		^"Testing «ταБЬℓσ»: 1<2 & 4+1>3, now 20% off!"
	}
}
