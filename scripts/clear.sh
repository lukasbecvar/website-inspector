#!/bin/bash

# clear project builded packages
mvn clean

# delete logs and target folderss
rm -rf logs
rm -rf target
