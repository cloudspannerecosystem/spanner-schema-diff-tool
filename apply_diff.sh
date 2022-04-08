#!/bin/bash

SPANNER_INSTANCE="test-spanner-consumer"
SPANNER_DATABASE="zhiwei-test"

FILE=$1
gcloud spanner databases ddl update "${SPANNER_DATABASE}" --instance="${SPANNER_INSTANCE}" \
    --ddl-file=$1
