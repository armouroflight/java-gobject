all:
	python ./waf build

install: all
	python ./waf install --destdir=$(DESTDIR)
	
clean:
	python ./waf clean