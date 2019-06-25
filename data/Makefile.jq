# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
## Check if jq works
JQ_ERROR = $(error Install jq 1.5 or later in your PATH)
ifneq ($(or $(shell echo '{"a": "b"}' | jq -e -r .a),$(JQ_ERROR)),b)
	die := $(JQ_ERROR)
endif
