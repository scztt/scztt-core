MIDIClient.init;
m = MIDIOut.newByName("Midi Fighter Twister", "Midi Fighter Twister");
m.latency = 0;
m.connect;
MIDIIn.connectAll;

~controls = (0..15).collect {
	|cc|
	var t, c, b;
	var buttonState = false;

	t = TwisterDeviceKnob(MIDIClient.externalSources[0].uid, m, cc);
	t.ledColor = Color.blue;
	t.ledBrightness = 0.5;

	c = Collapse({
		fork {
			(1,0.9..0.5).do {
				|b|
				t.ledBrightness = b;
				0.05.wait;
			}
		}
	}, 0.2);

	b = {
		fork {
			(1,0.9..0.5).do {
				|b, i|
				t.ledBrightness = b;
				t.ledHue = i.linlin(0, 5, Color.green.asHSV[0], Color.blue.asHSV[0]);
				0.03.wait;
			}
		}
	};

	t.knobFunc = {
		|val|
		val = (val / 127.0) ** 4;
		t.value = val.postln;
		t.ledBrightness = 1;
		c.value();
	};

	t.buttonFunc = {
		|value|
		if (value == 127) {
			t.ledColor = Color.green;
		} {
			b.value();
		}
	}
};


~controls.do(_.off)


MIDIFunc.trace(false);