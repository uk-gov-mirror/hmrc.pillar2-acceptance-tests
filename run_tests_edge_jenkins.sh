#!/usr/bin/env bash

ENV="local"
BROWSER="edge"

sbt clean -Dbrowser="${BROWSER:=chrome}" -Denvironment="${ENVIRONMENT:=local}" "testOnly uk.gov.hmrc.test.ui.cucumber.runner.Runner" testReport