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


$| = 1;

parse_options();

sub parse_options {
    GetOptions(
           "username|u=s"       => \$username,
           "password|p=s"       => \$password,
           "url|U=s"            => \$url,
           "prefix|P=s"            => \$annotation_track_prefix,
           "help|h"         => \$help);
    print_usage() if $help;
    die "Missing required parameter: username\n" if !$username;
    die "Missing required parameter: password\n" if !$password;
    die "Missing required parameter: url\n" if !$url;
    $url = "http://$url" if $url !~ m%http://%;
}



my $login_result=`curl -b cookies.txt -c cookies.txt -H "Content-Type:application/json" -d "{'username': '$username', 'password': '$password'}" "$url/Login?operation=login" 2> /dev/null`;



my $feature_result=`curl -b cookies.txt -c cookies.txt --data '{ "track": "Annotations-Group1.1", "features": [{"location":{"fmin":559153,"fmax":559540,"strand":1},"type":{"cv":{"name":"sequence"},"name":"mRNA"},"name":"GB42178-RA","children":[{"location":{"fmin":559153,"fmax":559540,"strand":1},"type":{"cv":{"name":"sequence"},"name":"exon"}}]}], "operation": "add_transcript" }' $url/AnnotationEditorService 2> /dev/null`;


