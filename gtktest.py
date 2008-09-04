from org.gnome.gir.dynamic.WebKit import *
from org.gnome.gir.dynamic.Gtk import *
from org.gnome.gir.gobject import *

GtkGlobals.initCheck(None, None)
win = Window(WindowType.TOPLEVEL)
#class DeleteHandler(Widget.DeleteEvent):
#  def onDeleteEvent(self, w, e):
#    GtkGlobals.mainQuit()
#win.connect(DeleteHandler())
sw = ScrolledWindow(None, None)
win.add(sw)
wv = WebView()
wv.open("http://www.gnome.org")
sw.add(wv)
win.setSizeRequest(640, 480)
win.showAll();
GtkGlobals.main();
