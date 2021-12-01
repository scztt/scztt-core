TestPTChain : UnitTest {

	setup {
		// called before each test
	}

	tearDown {
		// called after each test
	}

	test_embedSimple {
		var a = Pbind(\degree, Pseq((1..4), 2), \dur, 0.5);
		var b = Pbind(\instrument, Pseq([\a, \b], inf), \dur, 1);
		var results = PTChain(a, b).asStream.nextN(9, ());
		var desired = Pbind(\degree, Pseq((1..4), 2), \dur, 0.5,
			\instrument, Pseq([\a, \a, \b, \b], inf)).asStream.nextN(9, ());
		this.assert(results == [
			(degree: 1, dur: 0.5, instrument: \a),
			(degree: 2, dur: 0.5, instrument: \a),
			(degree: 3, dur: 0.5, instrument: \b),
			(degree: 4, dur: 0.5, instrument: \b),
			(degree: 1, dur: 0.5, instrument: \a),
			(degree: 2, dur: 0.5, instrument: \a),
			(degree: 3, dur: 0.5, instrument: \b),
			(degree: 4, dur: 0.5, instrument: \b),
			nil
		], "literal array of Events");
		this.assert(results == desired, "equivalent Pbind");
	}

	test_embedDifferentDurs {
		var a = Pbind(\degree, Pseq((1..4), 2), \dur, 0.5);
		var b = Pbind(\instrument, Pseq([\a,\b,\default], inf), \dur, Pseq([2,0.5,1.5], inf));
		var results = PTChain(a, b).asStream.nextN(9, ());
		var expected = [
			(degree: 1, dur: 0.5, instrument: \a),
			(degree: 2, dur: 0.5, instrument: \a),
			(degree: 3, dur: 0.5, instrument: \a),
			(degree: 4, dur: 0.5, instrument: \a),
			(degree: 1, dur: 0.5, instrument: \b),
			(degree: 2, dur: 0.5, instrument: \default),
			(degree: 3, dur: 0.5, instrument: \default),
			(degree: 4, dur: 0.5, instrument: \default),
			nil
		];
		this.assert(results == expected, "literal array of Events");
		results = (a << b).asStream.nextN(9, ());
		this.assert(results == expected, "using << operator");
	}

	test_embedThreeWay {
		var degs = [0, 2, 4, 6, 8, 10];
		var noteDur = degs.size.reciprocal;
		// degree:     | 0  2  4  6  8  10 |
		// instrument: | a     b     c     |
		// pan:        |-1        1        |
		var a = Pbind(\degree, Pseq(degs, 1), \dur, noteDur);
		var b = Pbind(\instrument, Pseq([\a,\b,\c], inf), \dur, 3.reciprocal);
		var c = Pbind(\pan, Pseq([-1, 1], inf), \dur, 2.reciprocal);
		var results = PTChain(a, b, c).asStream.nextN(degs.size+1, ());
		this.assert(results == [
			(degree: 0, dur: noteDur, instrument: \a, pan: -1),
			(degree: 2, dur: noteDur, instrument: \a, pan: -1),
			(degree: 4, dur: noteDur, instrument: \b, pan: -1),
			(degree: 6, dur: noteDur, instrument: \b, pan: 1),
			(degree: 8, dur: noteDur, instrument: \c, pan: 1),
			(degree: 10, dur: noteDur, instrument: \c, pan: 1),
			nil
		], "a << b << c");

		results = (b << a << c).asStream.all(());
		noteDur = 3.reciprocal;
		this.assert(results == [
			(degree: 0, dur: noteDur, instrument: \a, pan: -1),
			(degree: 4, dur: noteDur, instrument: \b, pan: -1),
			(degree: 8, dur: noteDur, instrument: \c, pan: 1)
		], "b << a << c");
		this.assert(results == (b << c << a).asStream.all(()), "...same as b << c << a");

		results = (c << a << b).asStream.all(());
		noteDur = 2.reciprocal;
		this.assert(results == [
			(degree: 0, dur: noteDur, instrument: \a, pan: -1),
			(degree: 6, dur: noteDur, instrument: \b, pan: 1)
		], "c << a << b");
		this.assert(results == (c << b << a).asStream.all(()), "...same as c << b << a");
	}

	test_timelessStream {
		// Event streams without duration/delta
		var degs = [0, 2, 4, 6, 8, 10];
		var noteDur = degs.size.reciprocal;
		var a = Pbind(\degree, Pseq(degs, 1), \dur, noteDur);
		var b = Pbind(\pan, Pseq([-1, 0, 1], inf)); // no \dur keys
		var results = PTChain(a, b).asStream.nextN(degs.size+1, ());
		this.assert(results == [
			(degree: 0, dur: noteDur, pan: -1),
			(degree: 2, dur: noteDur, pan: 0),
			(degree: 4, dur: noteDur, pan: 1),
			(degree: 6, dur: noteDur, pan: -1),
			(degree: 8, dur: noteDur, pan: 0),
			(degree: 10, dur: noteDur, pan: 1),
			nil
		], "Pseq");

		b = Pbind(\db, Pseed(5, Pwhite(-24, 0)));
		results = PTChain(a, b).asStream.nextN(degs.size+1, ());
		this.assert(results == [
			(degree: 0, dur: noteDur, db: -21),
			(degree: 2, dur: noteDur, db: -20),
			(degree: 4, dur: noteDur, db: -3),
			(degree: 6, dur: noteDur, db: -20),
			(degree: 8, dur: noteDur, db: -10),
			(degree: 10, dur: noteDur, db: -23),
			nil
		], "Pwhite");
	}

}
