# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
.PHONY:: all clean %.clean %.build hermetic web %.copy
MDROOT=..

# All targets that do not need an internet connection, and should always return
# the same results.
#
# If GTFS or other data is fetched only from git, then it can go here (eg:
# lax_tap).
HERMETIC_TARGETS := \
	compass \
	gironde \
	kmt \
	lax_tap \
	lisboa_viva \
	navigo \
	orca \
	oura \
	ravkav \
	szt \
	tisseo \
	troika

# All targets that need to fetch some data from the internet, so may not always
# return the same result.
WEB_TARGETS := \
	clipper \
	ezlink \
	mobib \
	ricaricami \
	seq_go \
	tfi_leap

ALL_TARGETS := $(HERMETIC_TARGETS) $(WEB_TARGETS)

all: hermetic web

%.build: %
	make -C $<

hermetic: $(foreach d,$(HERMETIC_TARGETS),$(d).build)

web: $(foreach d,$(WEB_TARGETS),$(d).build)

%.copy: %
	make -C $< copy

copy: $(foreach d,$(ALL_TARGETS),$(d).copy)

%.clean: %
	make -C $< clean

clean: $(foreach d,$(ALL_TARGETS),$(d).clean) mdst_clean

include Makefile.common
