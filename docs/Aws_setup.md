

# Setup Apollo with Amazon Web Services (AWS)

## Common instructions.

1. Find launchable preconfigured public Amazon Web Services (AWS) EC2 images may be [launched from Community AMIs in the N. Virginia region under 'Apollo'](docs/images/EC2Image.png).   
1. Launch with 2 vCPU, 8 GB recommended, 80 GB storage depending on the size of your data, and ports 22 and 8080 open at a minimum.
1. Server will be available at <http://aws.public.ip:8080/apollo/>.   

Binaries with be in the `apollo/bin` directory though it should be in the path.

## Working with image: Apollo 2.X Template Configured with Data

The `Apollo 2.5.0 Template Configured with Data` instance contains an existing admin user `admin@sampleorg.org` / `samplepassword`.

It also contains data from [three sample genomes](Apollo2Build.md#adding-sample-data).  The Honeybee (Apis Melifera) example has a working blat search and is sourced from the [Hymenoptera Genome Database](http://hymenopteragenome.org/).  

To use this instance:

1. Launch from AWS as above with the ports 
1. Change the password of the user by clicking on the user icon.
1. [Add data](Data_loading.md) either manually or automatically.
1. ***Don't use secure passwords unless https is configured!!!***

## Working with image: Apollo 2.X Empty Template 

The `Apollo 2.5.0 Empty Template` instance contains a deployed tomcat with database only.

1. Launch from AWS as above.
1. Register a new user at the dialog.
1. SSH into the machine and create a common_data_directory and update the path. e.g.:
   - `sudo mkdir -p /var/lib/tomcat8/apollo_data`
   - `sudo chown -R tomcat8:tomcat8 /var/lib/tomcat8/apollo_data`
   - `sudo sudo chmod -R 775 /var/lib/tomcat8/apollo_data`
   - set common folder to `/var/lib/tomcat8/apollo_data` or `apollo_data`
  
1. [Add data](Data_loading.md) either manually or automatically.
1. ***Don't use secure passwords unless https is configured!!!***

