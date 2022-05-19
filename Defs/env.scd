(
SynthDef(\env, {
	var env, curve, mod;

	env = \env.ir(Env([0, 1, 0] ++ (0 ! 15), [0.5, 0.5] ++ (0 ! 15)).asArray);
	env = EnvGen.ar(
		envelope:env,
		doneAction:2
	).poll(label:"env: ");

	mod = \mod.ar(0);

	Out.ar(\out.kr(0), env + mod);
}).add;
)