#!/bin/bash
SRC=/home/cdiesh/src/Apollo/grails-app/controllers/org/bbop/apollo
DST=/home/cdiesh/src/Apollo/apollo7/grails-app/controllers/org/bbop/apollo

mkdir -p "$DST"

for f in "$SRC"/*.groovy; do
  filename=$(basename "$f")
  sed \
    -e 's|org\.codehaus\.groovy\.grails\.web\.json\.JSONObject|org.grails.web.json.JSONObject|g' \
    -e 's|org\.codehaus\.groovy\.grails\.web\.json\.JSONArray|org.grails.web.json.JSONArray|g' \
    -e 's|org\.codehaus\.groovy\.grails\.web\.json\.JSONException|org.grails.web.json.JSONException|g' \
    -e 's|org\.codehaus\.groovy\.grails\.web\.servlet\.mvc\.GrailsParameterMap|org.grails.web.servlet.mvc.GrailsParameterMap|g' \
    -e 's|org\.codehaus\.groovy\.grails\.web\.converters\.exceptions\.ConverterException|org.grails.web.converters.exceptions.ConverterException|g' \
    -e 's|javax\.servlet\.|jakarta.servlet.|g' \
    -e 's|grails\.transaction\.Transactional|grails.gorm.transactions.Transactional|g' \
    -e '/^import grails\.transaction\.NotTransactional$/d' \
    -e 's|@NotTransactional||g' \
    "$f" > "$DST/$filename"
  echo "Ported: $filename"
done
echo "All controllers ported."
