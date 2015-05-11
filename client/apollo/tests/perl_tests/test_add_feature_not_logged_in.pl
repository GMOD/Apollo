#!/usr/bin/perl
use strict;
use warnings;
use File::Basename;
use lib dirname($0);

use Getopt::Long qw(:config no_ignore_case bundling);
use LWP::UserAgent;
use JSON;


my $annotation_track_prefix = "Annotations-";
my $username;
my $password;
my $url;
my $session_id;
my $help;
my $trackname;
my $organism;
my $feature_data;

$| = 1;

parse_options();

sub parse_options {
    GetOptions(
           "username|u=s"       => \$username,
           "password|p=s"       => \$password,
           "url|U=s"            => \$url,
           "track|t=s"            => \$trackname,
           "organism|o=s"            => \$organism,
           "prefix|P=s"           => \$annotation_track_prefix,
           "featuredata|F=s"           => \$feature_data,
           "help|h"             => \$help);
    print_usage() if $help;
    die "Missing required parameter: username\n" if !$username;
#die "Missing required parameter: password\n" if !$password;
    die "Missing required parameter: url\n" if !$url;
    die "Missing required parameter: track\n" if !$trackname;
    die "Missing required parameter: featuredata\n" if !$feature_data;
    $url = "http://$url" if $url !~ m%http://%;
}

sub print_usage {
    my $progname = basename($0);
    die << "END";
usage: $progname
    --url|-U <URL to WebApollo instance>
    --username|-u <username>
    --password|-p <password>
    --track|-t <trackname>
    [--organism|-o <organism id, commonName, abbrev>]
    [--prefix|-P <trackname>]

    U: URL to WebApollo instance
    u: username to access WebApollo
    p: password to access WebApollo
    o: organism to over-ride (or we user default)
    t: trackname to delete tracks on
    P: annotation track prefix [default: Annotations-]
    F: feature data
END
}

my $track=$annotation_track_prefix.$trackname;


#my $login_result=`curl -b cookies.txt -c cookies.txt -H "Content-Type:application/json" -d "{'username': '$username', 'password': '$password'}" "$url/Login?operation=login" 2> /dev/null`;


#print $login_result."\n";
#[{"location":{"fmin":559153,"fmax":559540,"strand":1},"type":{"cv":{"name":"sequence"},"name":"mRNA"},"name":"GB42178-RA","children":[{"location":{"fmin":559153,"fmax":559540,"strand":1},"type":{"cv":{"name":"sequence"},"name":"exon"}}]}]

#my $feature_result=`curl -b cookies.txt -c cookies.txt --data '{ "username": '$username', "track": "$track", "features": $feature_data, "operation": "add_transcript" }' $url/AnnotationEditorService 2> /dev/null`;


#my $feature_result=`curl --data '{ "username": '$username', "password":'$password', "track": "$track", "features": $feature_data, "operation": "add_transcript" }' $url/AnnotationEditorService 2> /dev/null`;

#my $feature_result=`curl --data '{ "username": '$username', "track": "$track", "features": $feature_data, "operation": "add_transcript" }' $url/AnnotationEditorService 2> /dev/null`;

#my $feature_result=`curl --data '{ "username": '$username', "password":'$password', "track": "$track", "features": $feature_data, "operation": "add_feature" }' $url/AnnotationEditorService 2> /dev/null`;
#my $feature_result=`curl --data '{ "username": '$username', "password":'$password', "track": "$track", "features": $feature_data, "operation": "delete_feature" }' $url/AnnotationEditorService 2> /dev/null`;
my $feature_result=`curl --data '{ "username": '$username', "password":'$password',"track": "$track", "features": $feature_data, "operation": "add_transcript","organism": '$organism' }' $url/AnnotationEditorService 2> /dev/null`;

print $feature_result . "\n";



