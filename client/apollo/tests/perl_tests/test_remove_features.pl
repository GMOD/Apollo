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
my $json = new JSON();
my $ua = new LWP::UserAgent();
$ua->timeout(3000); # 5 minutes



$| = 1;

parse_options();

sub parse_options {
    GetOptions(
           "username|u=s"       => \$username,
           "password|p=s"       => \$password,
           "url|U=s"            => \$url,
           "help|h"         => \$help);
    print_usage() if $help;
    die "Missing required parameter: username\n" if !$username;
    die "Missing required parameter: password\n" if !$password;
    die "Missing required parameter: url\n" if !$url;
    $url = "http://$url" if $url !~ m%http://%;
}
my $login_result=`curl -b cookies.txt -c cookies.txt -H "Content-Type:application/json" -d "{'username': '$username', 'password': '$password'}" "$url/Login?operation=login" 2> /dev/null`;

my $features_to_delete=`curl -b cookies.txt -c cookies.txt --data '{ "track": "Annotations-Group1.1", "operation": "get_features" }' $url/AnnotationEditorService 2> /dev/null`;

my $result=decode_json $features_to_delete;

my @features;
if($result->{features}) {
    foreach my $feature (@{$result->{features}}) {
        my %uniquename=("uniquename"=>$feature->{uniquename});
        push (@features, \%uniquename);
    }
}
my $str=encode_json \@features;


my $output=`curl -b cookies.txt -c cookies.txt --data '{ "track": "Annotations-Group1.1", "operation": "delete_feature","features": $str }' $url/AnnotationEditorService 2> /dev/null`;
if($output eq '{"features":[]}') {
    print "Success\n";
}
else {
    print "Error\n";
}
