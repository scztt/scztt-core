// {
// 	var a, b, c, d, e, f, g, h, n, sig;
// 	var pos, width = 4, chans = 4, paddedChans = chans + 1;
// 	pos = ((MouseX.kr(-1, 1).poll * ((paddedChans - width)/paddedChans)) - (1 / paddedChans));
// 	sig  = PanAz.ar(paddedChans, WhiteNoise.ar(), pos, width: width, orientation: 0);
// 	sig.wrapAt( ((0..(chans - 1)) + (chans / 2) + 1) )
// }.play

PanArray : UGen {
	*ar { arg numChans, in, pos = 0.0, level = 1.0, width = 2.0;
		var sig, actualPos, paddedNumChans;
		paddedNumChans = numChans + 1;
		actualPos = ((pos.linlin(0.0, 1.0, -1.0, 1.0) * ((paddedNumChans - width)/paddedNumChans)) - (1 / paddedNumChans));
		sig = PanAz.ar(paddedNumChans, in, actualPos, level, width, orientation: 0.0);
		^sig.wrapAt( ((0..(numChans - 1)) + (numChans / 2) + 1) );
	}
}
