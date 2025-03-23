PointListener : UGen {
	classvar <>headphoneModel = 10; // AudioTechnica-ATH-M50
	classvar <>hoaRefRadius;

	*initClass {
		ServerBoot.add(this)
	}

	*doOnServerBoot {
		|server|
		server.waitForBoot({
			HOABinaural.loadbinauralIRs(server);
			HOABinaural.loadHeadphoneCorrections(server);
			HOABinaural.binauralIRs;
			HOABinaural.headPhoneIRs;
		})
	}

	*ar {
		|sig|
		^HOABinaural.ar(
			AtkHoa.defaultOrder,
			HoaNFCtrl.ar(
				sig,
				AtkHoa.refRadius,
				hoaRefRadius,
				AtkHoa.defaultOrder
			),
			headphoneCorrection: headphoneModel
		)
	}
}

PointSource : UGen {
	classvar <>speedOfSound = 340.29;

	*ar {
		|sig, pos, listenerPos, encoding=\bformat|
		^switch (
			encoding,
			\bformat, {
				this.arBformat(sig, pos, listenerPos)
			}
		);
	}

	*arBformat {
		|sig, sourcePos, listenerPos=([0,0,0])|
		var relativePos, azimuth, elevation, distance;

		relativePos = (listenerPos - sourcePos);

		// angle of the speaker relative to the listener
		#azimuth, elevation = this.calcRelativeAngles(relativePos);
		distance = (relativePos[0].squared + relativePos[1].squared + relativePos[0].squared).sqrt;

		sig = DistAttenuate.ar(sig, distance, minDist:0.2);

		sig = HoaEncodeDirection.ar(
			sig,
			azimuth,
			elevation,
			distance,
			AtkHoa.defaultOrder
		);

		^sig;
	}

	*calcRelativeAngles {
		|relativePos|
		var relX, relY, relZ, azimuth, elevation;

		// #relX, relY, relZ = ZPointrelativePos;
		// azimuth = atan(relX / relY) - (pi/2);
		// elevation = atan(relZ / relY);
		relativePos = ZPoint(*relativePos);
		azimuth = relativePos.theta;
		elevation = relativePos.phi;

		^[azimuth, elevation];
	}

	*calc {
		| rot |
		rot = 1 - (((rot*2)-1).abs).pow(2.2);
		^(1 - (rot*0.78));
	}

	*distDelay {
		|sig, distance|
		^DelayC.ar(sig, 1, distance / speedOfSound);
	}
}