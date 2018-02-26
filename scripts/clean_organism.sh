#!/bin/sh

if [ "$#" -ne 2 ]; then
    echo "DO NOT USE ON PRODUCTION!!!"
    echo "Use 'delete_annotations_from_organism.groovy' instead"
    echo "Usage ./clean_organism.sh <common name> <database>"
	exit ;
fi
echo "Processing organism '$1' on database '$2'" 


echo "delete from feature_relationship where id in (select fr.id from feature_relationship fr join feature f on f.id = fr.parent_feature_id join feature_location fl on fl.feature_id = f.id join sequence s on fl.sequence_id=s.id join organism o on s.organism_id=o.id where o.common_name = '$1' ) ; " | psql $2

echo "delete from feature_location where id in (select fl.id from feature_location fl join sequence s on fl.sequence_id=s.id join organism o on s.organism_id=o.id where o.common_name = '$1' ) ; " | psql $2

echo "delete from feature_grails_user where feature_owners_id in ( select u.feature_owners_id from feature_grails_user u join feature f on f.id = u.feature_owners_id join feature_location fl on fl.feature_id = f.id join sequence s on fl.sequence_id=s.id join organism o on s.organism_id=o.id where o.common_name = '$1' ) ; " | psql $2

echo "delete from feature_property where id in ( select u.id from feature_property u join feature f on f.id = u.feature_id join feature_location fl on fl.feature_id = f.id join sequence s on fl.sequence_id=s.id join organism o on s.organism_id=o.id where o.common_name = '$1' ) ; " | psql $2

## select with features without feature locations 
echo "select count(*) from feature f left join feature_location fl on fl.feature_id = f.id join sequence s on fl.sequence_id=s.id join organism o on s.organism_id=o.id where o.common_name = '$1' and fl is null  ; " | psql $2

echo "delete from feature_event fe where not exists (select 'x' from feature f where f.unique_name = f.unique_name )" | psql $2 

