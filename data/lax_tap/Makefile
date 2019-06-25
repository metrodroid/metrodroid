# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

include ../Makefile.gitlab

GITLAB_PROJECT := LACMTA/gtfs_rail
GITLAB_COMMIT := 2e838bc22e0e7c084dbab70a1c97dc54975493c3

all: lax_tap.mdst lax_tap.csv

commit_info.json:
	$(call gitlab-get-metadata,$(GITLAB_PROJECT),$(GITLAB_COMMIT))

gtfs_rail.zip: commit_info.json
	$(call gitlab-fetch-file-md,$(GITLAB_PROJECT),$@)

sha512ok: sha512sums gtfs_rail.zip
	$(call sha512)

lax_tap.mdst: mdst sha512ok gtfs_rail.zip NOTICE mapping.csv operators.csv
	python3 $(MDST)/compile_stops_from_gtfs.py \
		gtfs_rail.zip -m mapping.csv -p operators.csv \
		-l en-US -n NOTICE -o $@

copy: lax_tap.copy

clean:
	rm -f lax_tap.mdst lax_tap.csv sha512ok gtfs_rail.zip

include ../Makefile.common
