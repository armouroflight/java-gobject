print("hello");
var gtkPkg=Packages.org.gnome.gir.dynamic.Gtk;
importPackage(gtkPkg);
print(gtkPkg.Window);
var win = Window(WindowType.TOPLEVEL);
var button = Button("hello!");
win.add(button);
win.showAll();
GtkGlobals.main();
