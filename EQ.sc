EQ {
	*ar {
		|sig,
		lowShelfFreq, lowShelfRS, lowShelfDB,
		mid1Freq, mid1RQ, mid1DB,
		mid2Freq, mid2RQ, mid2DB,
		mid3Freq, mid3RQ, mid3DB,
		hiShelfFreq, hiShelfRS, hiShelfDB,
		gainDB
		|

		var defaults,
		freqSpec, rqSpec, rsSpec, dbSpec;

		defaults			= EQdef.fromKey(\default).presetManager[\flat];

		freqSpec 			= EQdef.specDict[\freq];
		rqSpec 				= EQdef.specDict[\rq];
		rsSpec				= EQdef.specDict[\rs];
		dbSpec				= EQdef.specDict[\db];

		lowShelfFreq 		= \lowShelf_freq.kr(lowShelfFreq ? defaults[0][0], spec:freqSpec);
		lowShelfRS 			= \lowShelf_rs.kr(lowShelfRS ? defaults[0][1], spec:rsSpec);
		lowShelfDB 			= \lowShelf_db.kr(lowShelfDB ? defaults[0][2], spec:dbSpec);

		mid1Freq 			= \mid1_freq.kr(mid1Freq ? defaults[1][0], spec:freqSpec);
		mid1RQ 				= \mid1_rq.kr(mid1RQ ? defaults[1][1], spec:rqSpec);
		mid1DB 				= \mid1_db.kr(mid1DB ? defaults[1][2], spec:dbSpec);

		mid2Freq 			= \mid2_freq.kr(mid2Freq ? defaults[2][0], spec:freqSpec);
		mid2RQ 				= \mid2_rq.kr(mid2RQ ? defaults[2][1], spec:rqSpec);
		mid2DB 				= \mid2_db.kr(mid2DB ? defaults[2][2], spec:dbSpec);

		mid3Freq 			= \mid3_freq.kr(mid3Freq ? defaults[3][0], spec:freqSpec);
		mid3RQ 				= \mid3_rq.kr(mid3RQ ? defaults[3][1], spec:rqSpec);
		mid3DB 				= \mid3_db.kr(mid3DB ? defaults[3][2], spec:dbSpec);

		hiShelfFreq 		= \hiShelf_freq.kr(hiShelfFreq ? defaults[4][0], spec:freqSpec);
		hiShelfRS 			= \hiShelf_rs.kr(hiShelfRS ? defaults[4][1], spec:rsSpec);
		hiShelfDB 			= \hiShelf_db.kr(hiShelfDB ? defaults[4][2], spec:dbSpec);

		gainDB 				= \gainDB.kr(gainDB ? defaults[5][0], spec:dbSpec);

		sig = BLowShelf.ar(sig, lowShelfFreq, lowShelfRS, lowShelfDB);
		sig = BPeakEQ.ar(sig, mid1Freq, mid1RQ, mid1DB);
		sig = BPeakEQ.ar(sig, mid2Freq, mid2RQ, mid2DB);
		sig = BPeakEQ.ar(sig, mid3Freq, mid3RQ, mid3DB);
		sig = BHiShelf.ar(sig, hiShelfFreq, hiShelfRS, hiShelfDB);
		sig = Gain.ar(sig, gainDB);

		^sig
	}
}

