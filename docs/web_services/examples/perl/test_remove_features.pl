#!/usr/bin/perl
use strict;
use warnings;
use File::Basename;
use lib dirname($0);

use Getopt::Long qw(:config no_ignore_case bundling);
use LWP::UserAgent;
use JSON;


my $username;
my $password;
my $url;
my $session_id;
my $help;
my $trackname;


$| = 1;

parse_options();

sub parse_options {
    GetOptions(
           "username|u=s"       => \$username,
           "password|p=s"       => \$password,
           "url|U=s"            => \$url,
           "track|t=s"            => \$trackname,
           "help|h"             => \$help);
    print_usage() if $help;
    die "Missing required parameter: username\n" if !$username;
    die "Missing required parameter: password\n" if !$password;
    die "Missing required parameter: url\n" if !$url;
    die "Missing required parameter: track\n" if !$trackname;
    $url = "http://$url" if $url !~ m%http://%;
}



sub print_usage {
    my $progname = basename($0);
    die << "END";
usage: $progname
    --url|-U <URL to Apollo instance>
    --username|-u <username>
    --password|-p <password>
    --track|-t <trackname>
    [--prefix|-P <trackname>]

    U: URL to Apollo instance
    u: username to access Apollo
    p: password to access Apollo
    t: trackname to delete tracks on
END
}

print $trackname."\n";
my $track=$trackname;

print $track."\n";
my $login_result=`curl -b cookies.txt -c cookies.txt -H "Content-Type:application/json" -d "{'username': '$username', 'password': '$password'}" "$url/Login?operation=login" 2> /dev/null`;

my $features_to_delete=`curl -b cookies.txt -c cookies.txt --data '{ "track": "$track", "operation": "get_features" }' $url/AnnotationEditorService 2> /dev/null`;

my $result=decode_json $features_to_delete;

my @features;
if($result->{features}) {
    foreach my $feature (@{$result->{features}}) {
        my %uniquename=("uniquename"=>$feature->{uniquename});
        push (@features, \%uniquename);
    }
}
my $str=encode_json \@features;


my $output=`curl -b cookies.txt -c cookies.txt --data '{ "track": "$track", "operation": "delete_feature","features": $str }' $url/AnnotationEditorService 2> /dev/null`;
if($output eq '{"features":[]}') {
    print "Success\n";
}
else {
    print "Error: $output";
}
