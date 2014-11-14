#!/usr/bin/perl

use strict;
use warnings;

use FindBin qw($RealBin);
use lib "$RealBin/../src/perl5";
use JBlibs;

use DBI;
use Getopt::Long qw(:config no_ignore_case bundling);
use File::Basename;
use IO::File;


my $TRACKS_TABLE = "tracks";

my $dbh;
my $tracks = \*STDIN;

parse_options();
add_tracks();
cleanup();

sub parse_options {
    my $host = "localhost";
    my $port = 5432;
    my $dbname = $ENV{WEB_APOLLO_DB};
    my $dbusername = $ENV{WEB_APOLLO_DB_USER};
    my $dbpassword = $ENV{WEB_APOLLO_DB_PASS};
    my $tracks_file;
    my $help;
    GetOptions("host|H=s"    	=> \$host,
           "port|X=s"		=> \$port,
           "dbname|D=s"		=> \$dbname,
           "dbusername|U=s"	=> \$dbusername,
           "dbpassword|P=s"	=> \$dbpassword,
           "tracks|t=s"		=> \$tracks_file,
           "help|h"		=> \$help);
    print_usage() if $help;
    die "Database name is required\n" if !$dbname;
    if ($tracks_file) {
        $tracks = new IO::File($tracks_file)
        	or die "Error reading $tracks_file: $!";
    }
    my $connect_string = "dbi:Pg:host=$host;port=$port;dbname=$dbname";
    $dbh = DBI->connect($connect_string, $dbusername, $dbpassword);
}

sub print_usage {
    my $progname = basename($0);
    die << "END";
usage: $progname
    [-H|--host <user_database_host>]
    [-X|--port <user_database_port>]
    -D|--dbname <user_database_name>
    [-U|--dbusername <user_database_username>]
    [-P|--dbpassword <user_database_password>]
    [-t|--track <file_containing_track_names>]
    [-h|--help]

    -t: file containing track names (one per line)
END
}

sub add_tracks {
    my $existing_tracks = get_existing_tracks();
    my $sql = "INSERT INTO $TRACKS_TABLE(track_name) VALUES(?)";
    my $sth = $dbh->prepare($sql);
    while (my $track = $tracks->getline()) {
        chomp $track;
        print "Processing $track\n";
        next if exists $existing_tracks->{$track};
        $sth->bind_param(1, $track);
        $sth->execute();
    }
}

sub get_existing_tracks {
    my %existing_tracks = ();
    my $sql = "SELECT track_name FROM $TRACKS_TABLE";
    my $sth = $dbh->prepare($sql);
    $sth->execute();
    while (my $row = $sth->fetchrow_arrayref()) {
        ++$existing_tracks{$row->[0]};
    }
    return \%existing_tracks;
}

sub cleanup {
    $dbh->disconnect();
}
