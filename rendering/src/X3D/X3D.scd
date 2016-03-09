X3DSound.new(X3DWorld.new)

// specification
w = X3DWorld.new;
x = X3DTestNode.new(w);
y = X3DTestNode.new(w);
X3DRoute(x, \value, y, \value);
X3DRoute(y, \value, x, \value);

// implementation
z = X3DFieldListener(y, \value, { | time, value | [time, value].postln });
z = X3DFieldListener(x, \value, { | time, value | [time, value].postln });
x.field(\value).value_(rrand(0.1, 0.5));
