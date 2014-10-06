#!/usr/bin/perl
use strict;
use warnings;
use Data::Dumper;
use File::Basename;
use lib dirname($0);

use Getopt::Long qw(:config no_ignore_case bundling);
use IO::File;
use LWP::UserAgent;
use JSON;

my $annotation_track_prefix = "Annotations-";
my $username;
my $password;
my $url;
my $session_id;
my $help;
my $output;
my $tracks_file;
my @tracks;
my $json = new JSON();
my $ua = new LWP::UserAgent();
$ua->timeout(3000); # 5 minutes



$| = 1;

parse_options();
parse_tracks_file();
login();
send_gff3_request(\@tracks);
logout();

sub parse_options {
    GetOptions("output|o=s"     => \$output,
           "username|u=s"       => \$username,
           "password|p=s"       => \$password,
           "url|U=s"            => \$url,
           "track_prefix|P=s"   => \$annotation_track_prefix,
           "tracks_file|T=s"    => \$tracks_file);
    print_usage() if $help;
    die "Missing required parameter: username\n" if !$username;
    die "Missing required parameter: password\n" if !$password;
    die "Missing required parameter: url\n" if !$url;
    die "Missing required parameter: tracks_file\n" if !$tracks_file;
    $url = "http://$url" if $url !~ m%http://%;
}



sub print_usage {
    my $progname = basename($0);
    die << "END";
usage: $progname
    --url|-U <URL to WebApollo instance>
    --output|-o <output file>
    --username|-u <username>
    --password|-p <password>
    [--track_prefix|-P <annotation track prefix>]
    --tracks_file|-T <tracks file>

    U: URL to WebApollo instance
    u: username to access WebApollo
    p: password to access WebApollo
    P: annotation track prefix
       [default: "$annotation_track_prefix"]
    T: tracks file
    o: output file
END
}



sub parse_tracks_file {
   open my $fh, '<', $tracks_file or die $!;
   while (my $line=<$fh>) {
       chomp($line);
       push(@tracks,$line);
   }
}


sub login {
    my $login = {
        username => $username,
        password => $password
    };
    my $req = new HTTP::Request();
    $req->method("POST");
    $req->uri("$url/Login?operation=login");
    $req->content($json->encode($login));
    my $res = $ua->request($req);
    if ($res->is_success()) {
        my $content = $json->decode($res->content());
        $session_id = $content->{sessionId};
    }
    else {
        my $content;
        my $message;
        eval {
            $content = $json->decode($res->content());
        };
        if ($@) {
            $message = $res->status_line;
        }
        else {
            $message = $content->{error} ? $content->{error} : $res->status_line;
        }
        die "Error logging in to server: $message\n";
    }
}

sub logout {
    my $req = new HTTP::Request();
    $req->method("POST");
    $req->uri("$url/Login?operation=logout");
    $ua->request($req);
}

sub send_gff3_request {
    my @arr;
    foreach my $i (@tracks) {
        push(@arr,$annotation_track_prefix.$i);
    }
    my $request = {
            tracks => \@arr,
            adapter => "GFF3",
            operation => "write",
            options => "output=display&format=text"
    };
    my $req = new HTTP::Request();
    $req->method("POST");
    $req->uri("$url/IOService;jsessionid=$session_id");
    $req->content($json->encode($request));
    my $res = $ua->request($req);
    if ($res->is_success()) {
        my $fh;
        if($output) {
            open($fh, '>', $output);
        }
        my $outh = $output ? $fh : *STDOUT;
        print $outh $res->content();
        if($output) {
            close($fh);
        }
    }
    else {
        my $content;
        my $message;
        eval {
            $content = $json->decode($res->content());
        };
        if ($@) {
            $message = $res->status_line;
        }
        else {
            $message = $content->{error} ? $content->{error} : $res->status_line;
        }
        return { error => $message };
    }
}

