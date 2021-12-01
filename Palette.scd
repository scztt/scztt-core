
(
~palette = QPalette.dark;

// ~roles = QColorRole.class.methods.collect(_.name);
// ~roles.remove(\new);
//
// ~e = ~roles.collectAs({
// 	|role|
// 	role -> "Color.hsv(%, %, %)".format(
// 		~palette.color(role).hue.round(0.01),
// 		~palette.color(role).sat.round(0.01),
// 		~palette.color(role).val.round(0.01)
// 	);
// }, Event);
// ~e.keysValuesDo {
// 	|role, color|
// 	"\%:\t\t\t%".format(role, color).postln
// };

~hue1 = 0.87;
~new = (
	window:			Color.hsv(0.0, 0.0, 0.08),
	windowText:		Color.hsv(0.0, 0.0, 0.85),

	base:			Color.hsv(0.0, 0.0, 0.06),
	baseText:		Color.hsv(0, 0.0, 0.65),
	alternateBase:	Color.hsv(0.4, 0.0, 0.11),

	brightText:		Color.hsv(0.0, 0.0, 1.0),

	highlight:		Color.hsv(~hue1, 0.7, 0.5),
	highlightText:	Color.hsv(~hue1, 0, 0.95),

	button:			Color.hsv(0.0, 0.0, 0.18),
	buttonText:		Color.hsv(~hue1, 0, 0.85),

	toolTipBase:	Color.hsv(0.17, 0.14, 1.0),
	toolTipText:	Color.hsv(0.0, 0.0, 0.0),

	link:			Color.hsv(0.67, 1.0, 1.0),
	linkVisited:	Color.hsv(0.83, 1.0, 1.0),

	light:			Color.hsv(0.0, 0.0, 0.64),
	shadow:			Color.hsv(0.0, 0.0, 0.08),
	dark:			Color.hsv(0.0, 0.0, 0.21),
	midlight:		Color.hsv(0.0, 0.0, 0.53),
	middark:		Color.hsv(0.0, 0.0, 0.29),

);

~new.keysValuesDo {
	|role, color|
	~palette.setColor(color, role);
};

~palette;
)
