var gobject = Packages.org.gnome.gir.gobject.GObjectGlobals.init()
var gtk = Packages.org.gnome.gir.dynamic.Gtk;

gtk.GtkGlobals.init(null, null)

var win = gtk.Window();

var button = gtk.Button.newWithLabel("hello!");
button.connect(gtk.Button.Clicked({
	    onClicked: function () {
		print("Hello");
	    }}));

win.add(button);
win.showAll();

win.connect(gtk.Widget.DeleteEvent({
	    onDeleteEvent: function (event) {
		gtk.GtkGlobals.mainQuit();
		return true;
	    }}));

gtk.GtkGlobals.main();
