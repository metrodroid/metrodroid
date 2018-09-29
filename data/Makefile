# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
.PHONY:: all clean %.clean %.build hermetic web
MDST = ../extra/mdst/

# All targets that do not need an internet connection to return the same result.
HERMETIC_TARGETS := \
	compass \
	kmt \
	lax_tap \
	orca \
	szt \
	troika

# All targets that need to fetch some data from the internet (non-hermetic)
WEB_TARGETS := \
	clipper \
	ezlink \
	seq_gocard \
	tfi_leap

ALL_TARGETS := $(HERMETIC_TARGETS) $(WEB_TARGETS)

all: hermetic web

%.build: %
	make -C $<

hermetic: $(foreach d,$(HERMETIC_TARGETS),$(d).build)

web: $(foreach d,$(WEB_TARGETS),$(d).build)

%.clean: %
	make -C $< clean

clean: $(foreach d,$(ALL_TARGETS),$(d).clean) mdst_clean

include Makefile.common

