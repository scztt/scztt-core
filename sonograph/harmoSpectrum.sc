HarmoSpectrum {

	var <>spectrum ;

	// start with an array of amps
	*newFrom { arg spectrum ;
		^super.new.initHarmoSpectrum(spectrum)
	}

	initHarmoSpectrum { arg aSpectrum ;
		spectrum = aSpectrum ;
	}

	plotSpectrum  {|step = 10|
		var w = Window.new("spectrum",
			Rect(100, 100, step*88, 96*4)).front ;
		w.drawFunc = {
			Pen.font = Font( "DIN Condensed", step );
			Array.series(9, -10, -10).do{|i,j|
				Pen.strokeColor = Color.gray(0.5) ;
				Pen.line(
					Point(0, i.neg*4),
					Point(w.bounds.width, i.neg*4)
				) ;
				Pen.stroke ;
				Pen.fillColor = Color.red ;
				Pen.stringAtPoint(i.asString,Point(0, i.neg*4)) ;
				Pen.fill
			} ;
			Pen.stroke ;
			spectrum.do{|i,j|
				Pen.fillColor = Color.red ;
				if(((j+21)%12) == 0){
					Pen.stringAtPoint(
						((j+21).midinote.last).asString, Point(step*j+step, 10)) ;
					Pen.fillStroke ;
					Pen.strokeColor = Color.gray(0.5) ;
					Pen.line(
						Point(step*j, 0),
						Point(step*j, 96*4)
					) ;
					Pen.stroke ;
				} ;
				Pen.strokeColor = Color.white ;
				Pen.line(
					Point(step*j, i.neg*4),
					Point(step*j, 96*4)
				) ;
				Pen.stroke ;
				Pen.addOval(
					Rect(step*j-(step*0.25), i.neg*4, step*0.5, step*0.5)
				) ;
				Pen.fill ;
				Pen.fillColor = Color.black ;
				Pen.stringAtPoint((j+21).midinote.toUpper[..1],Point(step*j-(step*0.5), i.neg*4+step)) ;
				Pen.stringAtPoint((j+21).asString,Point(step*j-(step*0.5), i.neg*4-step)) ;
				Pen.fill
			} ;
		} ;
		w.view.mouseDownAction_{|view, x, y, mod|
			var pitch =
			x.linlin(0, view.bounds.width, 0, 88).round + 21;
			Synth(\mdaPiano, [\freq, pitch.midicps]) ;
		}
	}

	// set the maxima arr as a num of spectral maxima and db
	specMaxima { arg num = 4 ;
		var amps  ;
		var maxima = [] ;
		//if (spectrum.isNil){ this.calculateSpectrum } ;
		// we do a copy because of sort
		amps = spectrum.collect{|i| i};
		amps = amps.sort.reverse[..(num-1)] ;
		amps.do{|amp|
			maxima =
			maxima.add([spectrum.indexOf(amp)+21, amp]) ;
		} ;
		^maxima
	}

	// gives you back the chord of maxima, pitches and no dbs
	maximaChord { |num = 4| ^this.specMaxima(num).collect{|i| i[0]} }

	// plays back the maxima chord, db weighted
	playMaxima {|maxima, boost = 20| // lotta dbs coz typically low
		maxima.do{|i|
			Synth(\mdaPiano,
				[\freq, i[0].midicps, \mul, (i[1]+boost).dbamp])
		};
	}

	// spec to lily
	// PRIVATE
	createLilyNote {|midi|
		var post = "" ;
		var name = midi.midinote[..1] ;
		var oct = midi.midinote[2].asString.asInteger ;
		name = name.replace(" ", "").replace("#", "is") ;
		if (oct >= 5){
			(oct-4).do{|i|
				post = post++"'"
			}
		}{
			(4-oct).do{
				post = post++","
			}
		};
		^name++post++4;
	}

	createLilyChord  {|chord|
		var treble = [], tCh = "" ;
		var bass = [], bCh = "";
		chord.postln.do{|midi|
			if (midi >= 60){
				treble = treble.add(this.createLilyNote(midi))
			}{
				bass = bass.add(this.createLilyNote(midi))
			}
		};
		if (treble == []) {
			tCh = "	\\hideNotes c'4 \\unHideNotes  \\override Stem.transparent = ##t"
		}{
			treble.do{|n| tCh = tCh+n}};
		if (bass == []) {
			bCh = "	\\hideNotes c,4 \\unHideNotes  \\override Stem.transparent = ##t"
		}{
			bass.do{|n| bCh = bCh+n}};
		if (bass.size > 0) { bCh = "<<"+bCh+">>" };
		if (treble.size > 0) { tCh = "<<"+tCh+">>" };
		^[tCh, bCh]
	}



	writeLilyChord {|chord, path|
		var treble = "" ;
		var bass = "" ;
		var score ;
		var lilyChFile, ch ;
		path = if (path.isNil){"/tmp/sonoLily.ly"}{path} ;
		lilyChFile = File(path,"w") ;
		score = "

\\version \"2.18.2\"

\\header {
tagline = \"\"  % removed
}

#(set! paper-alist (cons '(\"my size\" . (cons (* 1.5 in) (* 2.5 in))) paper-alist))

\\paper {
#(set-paper-size \"my size\")
}


woodstaff = {
% This defines a staff with only one lines.
% It also defines its position
\\override Staff.StaffSymbol.line-positions = #'(0)
\\override Staff.TimeSignature #'stencil = ##f
\\override Stem.transparent = ##t
\\set fontSize = -3
%\tempo 4 = BPM
\\override Score.MetronomeMark.X-offset = #-3
\\override Score.MetronomeMark.Y-offset = #6
\\clef percussion
\\override Staff.Clef  #'stencil = ##f
\\time 1/4
\\hideNotes
% This is necessary; if not entered, the barline would be too short!
\\override Staff.BarLine.bar-extent = #'(-1 . 1)

}

\\score {
<<
\\new PianoStaff

<<
\\new Staff

{\\override Staff.TimeSignature #'stencil = ##f
\\override Stem.transparent = ##t
\\set fontSize = -1
%\tempo UN = BPM
\\time 1/4

TREBLE



}

\\new Staff

{\\clef bass
\\override Staff.TimeSignature #'stencil = ##f
\\override Stem.transparent = ##t


\\set fontSize = -1
%\tempo UN = BPM
\\time 1/4

BASS


\\bar \"|.\"

}
>>
>>
}
" ;
		ch = this.createLilyChord(chord) ;
		treble = treble + ch[0]++"\n" ;
	bass = bass +ch[1] ++"\n" ;
		score = score.replace("TREBLE", treble)
		.replace("BASS", bass)
		;
		lilyChFile.write(score);
		lilyChFile.close;
	}

	// PUBLIC
	specToLily {|maximaChord, path|
		this.writeLilyChord(maximaChord, path)
	}

	renderLily {|path|
		path = if (path.isNil){"/tmp/sonoLily.ly"}{path} ;
		(
			"Applications/LilyPond.app/Contents/Resources/bin/lilypond  -fpng --output="++path.splitext[0] + path
		).unixCmd
	}

	showSpectrumChord { |num = 6|
		var im, w ;
		var maxima = this.specMaxima(num) ;
		{
			this.specToLily(this.maximaChord(num)) ;
			1.wait ;
			this.renderLily ;
			1.wait ;

			im = Image.new("/tmp/sonoLily.png");

			w = Window.new("", Rect(400, 400, 100, 180));
			w.view.background_(Color.white);
			w.view.backgroundImage_(im);
			w.front;
			w.view.mouseDownAction_{this.playMaxima(maxima)}
		}.fork(AppClock)
	}

	// END OF SPECTRUM METHODS
}

/*
SonaGraph.prepare ;
// something to analyze, i.e a buffer
~path ="/Users/andrea/musica/regna/fossilia/compMine/erelerichnia/fragm/snd/vareseOctandreP18M5N[8,9,0,7,11,6].aif"; ~sample = Buffer.read(s, ~path).normalize ;

// an istance
a = SonaGraph.new;
// now analyzing in real-time
a.analyze(~sample,15) ; // high rate!
a.gui
h = HarmoSpectrum.newFrom(a.calculateAvSpectrum(79,95))
h.spectrum
h.plotSpectrum
h.specMaxima(6)
h.maximaChord(4)
h.showSpectrumChord(4)
*/