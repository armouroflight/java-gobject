from org.gnome.gir.dynamic.WebKit import *
from org.gnome.gir.dynamic.Gtk import *
from org.gnome.gir.gobject import *

GObjectGlobals.init()
GtkGlobals.initCheck(None, None)
win = Window(WindowType.TOPLEVEL);
sw = ScrolledWindow(None, None)
win.add(sw)
wv = WebView()
wv.open("http://www.gnome.org")
sw.add(wv)
win.setSizeRequest(640, 480)
win.showAll();
GtkGlobals.main();
