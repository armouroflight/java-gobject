all:
	python ./waf build

install: all
	python ./waf install --destdir=$(DESTDIR)
	
check:

clean:
	python ./waf clean