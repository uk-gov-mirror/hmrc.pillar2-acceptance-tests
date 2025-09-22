#!/usr/bin/env bash

ENV="local"


sbt clean -Dbrowser="edge" -Denvironment="${ENVIRONMENT:=local}" "testOnly uk.gov.hmrc.test.ui.cucumber.runner.Runner" testReport