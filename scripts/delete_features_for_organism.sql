DELETE FROM feature_property
WHERE feature_property.id IN (
  SELECT feature_property.id AS fp_id
  FROM feature_property fp
    JOIN feature f ON fp.feature_id = f.id
    JOIN feature_location fl ON f.id = fl.feature_id
    JOIN sequence s ON fl.sequence_id = s.id
    JOIN organism o ON s.organism_id = o.id
  WHERE o.common_name = 'volvox'
);

DELETE FROM feature_grails_user
WHERE feature_grails_user.feature_owners_id IN (
  SELECT feature_grails_user.feature_owners_id
  FROM feature_grails_user fp
    JOIN feature f ON fp.feature_owners_id = f.id
    JOIN feature_location fl ON f.id = fl.feature_id
    JOIN sequence s ON fl.sequence_id = s.id
    JOIN organism o ON s.organism_id = o.id
  WHERE o.common_name = 'volvox'
);

DELETE FROM feature_relationship
WHERE feature_relationship.id IN (
  SELECT feature_relationship.id AS fp_id
  FROM feature_relationship fp
    JOIN feature f ON fp.parent_feature_id = f.id
    JOIN feature_location fl ON f.id = fl.feature_id
    JOIN sequence s ON fl.sequence_id = s.id
    JOIN organism o ON s.organism_id = o.id
  WHERE o.common_name = 'volvox'
);

DELETE FROM feature_relationship
WHERE feature_relationship.id IN (
  SELECT feature_relationship.id AS fp_id
  FROM feature_relationship fp
    JOIN feature f ON fp.child_feature_id = f.id
    JOIN feature_location fl ON f.id = fl.feature_id
    JOIN sequence s ON fl.sequence_id = s.id
    JOIN organism o ON s.organism_id = o.id
  WHERE o.common_name = 'volvox'
);

DELETE FROM feature_event
WHERE feature_event.unique_name IN (
  SELECT feature_event.unique_name
   from feature_event fp
    JOIN feature f ON fp.unique_name = f.unique_name
    JOIN feature_location fl ON f.id = fl.feature_id
    JOIN sequence s ON fl.sequence_id = s.id
    JOIN organism o ON s.organism_id = o.id
  WHERE o.common_name = 'volvox'
);

DELETE FROM feature
WHERE feature.id IN (
  SELECT feature.id from feature AS f
    JOIN feature_location fl ON f.id = fl.feature_id
    JOIN sequence s ON fl.sequence_id = s.id
    JOIN organism o ON s.organism_id = o.id
  WHERE o.common_name = 'volvox'
);



DELETE FROM feature_location
WHERE feature_location.id IN (
  SELECT feature_location.id
  FROM feature_location fl
    JOIN sequence s ON fl.sequence_id = s.id
    JOIN organism o ON s.organism_id = o.id
  WHERE o.common_name = 'volvox'
);
