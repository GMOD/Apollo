Apollo stress test using Apache JMeter
======================================

### jmeter-dev-stress-test-1.jmx
The script has two user-defined variables:

* server (`localhost`)
* instance_name ( `apollo`)

The test is performed on data from *Apis mellifera*.

Datasets:

1. [*A. mellifera* reference genome](http://hymenopteragenome.org/beebase/sites/hymenopteragenome.org.beebase/files/data/Amel_4.5_scaffolds.fa.gz)
2. [*A. mellifera* Official Gene Set v3.2](http://hymenopteragenome.org/beebase/sites/hymenopteragenome.org.beebase/files/data/consortium_data/amel_OGSv3.2.gff3.gz)

The datasets can be processed as described [here](http://genomearchitect.readthedocs.io/en/latest/Data_loading/#data-generation-pipeline).

### jmeter-dev-stress-test-2.jmx

The script has two user-defined variables:
* server (`localhost`)
* instance_name ( `apollo`)

The test is performed on data from *Apis mellifera* as well as *Bos taurus*.

Datasets:

1. [*Bos taurus* reference genome](http://128.206.12.216/drupal/sites/bovinegenome.org/files/data/umd3.1/UMD3.1_chromosomes.fa.gz)
2. [*Bos taurus* RefSeq Annotations for protein coding genes](http://128.206.12.216/drupal/sites/bovinegenome.org/files/data/umd3.1/RefSeq_UMD3.1.1_protein_coding.gff3.gz)

The datasets can be processed as described [here](http://genomearchitect.readthedocs.io/en/latest/Data_loading/#data-generation-pipeline).

###Users
Each of the test script utilizes several user profiles. To add the same user profiles as described in the script, make use of ```add_users.groovy``` with ```example-users-for-stress-test.csv``` as input.
```
add_users.groovy -inputfile example-users-for-stress-test.csv --username <admin username> --password <admin password> --destinationurl <URL for apollo>
```

Note: After this step, make sure to grant organism permissions to each user. This can be done via the Users tab in Annotator Panel of Apollo.
