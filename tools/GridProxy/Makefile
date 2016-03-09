TARGETS = bin/GridProxyApp.exe bin/GridProxy.dll
PROJECTFILE = GridProxy.build
BUILDFILES = $(PROJECTFILE) lib/GridProxy.dll.build src/GridProxyApp.exe.build

CONFIGURATION = release

all: $(TARGETS)

$(PROJECTFILE): prebuild.xml
	mono ./bin/Prebuild.exe prebuild.xml /target nant

$(TARGETS):
	nant -buildfile:GridProxy.build build-$(CONFIGURATION)

.PHONY: $(TARGETS)

README.html: README.txt
	pandoc -t html -s -o $@ $<

dist: $(TARGETS) README.html
	tar -C .. -czv -f GridProxy-`git describe --tags HEAD`.tar.gz \
		GridProxy/README.txt \
		GridProxy/README.html \
		GridProxy/bin

clean:
	rm -f \
		$(BUILDFILES) \
		.GridProxyApp-Release-timestamp \
		$(TARGETS) \
		$(TARGETS:=.mdb) \
		README.html

.PHONY: dist clean
