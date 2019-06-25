# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

include ../Makefile.github

GITHUB_PROJECT := N3evin/AmiiboAPI
GITHUB_COMMIT  := b5703ef93413941aa58c46ef42202af6c24c160d
DATA_FILES      = database/amiibo.json LICENSE

all: amiibo.mdst amiibo.csv

commit_info.json:
	$(call github-get-metadata,$(GITHUB_PROJECT),$(GITHUB_COMMIT))

$(DATA_FILES): commit_info.json
	$(call github-fetch-file-md,$(GITHUB_PROJECT),$@)

NOTICE: NOTICE-prefix LICENSE
	cat $^ > $@

sha512ok: sha512sums $(DATA_FILES)
	$(call sha512)

amiibo.mdst: mdst sha512ok commit_info.json database/amiibo.json NOTICE
	python3 $(MDST)/amiibo2pb.py \
		-V "$(call github-get-ts,commit_info.json)" \
		-o $@ -n NOTICE database/amiibo.json

copy: amiibo.copy

clean:
	rm -f amiibo.mdst amiibo.csv commit_info.json NOTICE sha512ok $(DATA_FILES)
	[[ -e database ]] && rmdir database || true

include ../Makefile.common
