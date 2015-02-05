#!/bin/bash

psql apollo -c  "delete from feature_dbxref";
psql apollo -c  "delete from feature_property";
psql apollo -c  "delete from feature_relationship";
psql apollo -c  "delete from feature_location";
psql apollo -c  "delete from feature";

