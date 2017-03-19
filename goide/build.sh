#!/usr/bin/env bash

# TODO make this dynamic (maybe clone the repository first in the path then checkout the specified tag?
IDEA_PATH=/home/florin/IdeaProjects/intellij-community
IDEA_VERSION=144.3143.6

MY_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

ant -v -Didea.path=${IDEA_PATH} -Didea.build.number=${IDEA_VERSION} -Dbasedir=${MY_DIR}