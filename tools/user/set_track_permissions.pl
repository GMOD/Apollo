#!/usr/bin/perl

use strict;
use warnings;

use FindBin qw($RealBin);
use lib "$RealBin/../../src/perl5";
use JBlibs;

use DBI;
use Getopt::Long qw(:config no_ignore_case bundling);
use File::Basename;
use IO::File;

my $TRACKS_TABLE = "tracks";
my $USERS_TABLE = "users";
my $PERMISSIONS_TABLE = "permissions";
my $PERMISSION_NONE = 0x0;
my $PERMISSION_READ = 0x1;
my $PERMISSION_WRITE = 0x2;
my $PERMISSION_PUBLISH = 0x4;
my $PERMISSION_USER_MANAGER = 0x8;

my $dbh;
my $tracks = \*STDIN;
my $username;
my $permission = $PERMISSION_NONE;

parse_options();
set_permissions();
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
           "username|u=s"	=> \$username,
           "tracks|t=s"		=> \$tracks_file,
           "read|r"		=> sub { $permission |= $PERMISSION_READ },
           "write|w"		=> sub { $permission |= $PERMISSION_WRITE },

           "publish|c"		=> sub { $permission |= $PERMISSION_PUBLISH },

           "user_manager|m"	=> sub { $permission |= $PERMISSION_USER_MANAGER },
           "all|a"		=> sub { $permission |= $PERMISSION_READ | $PERMISSION_WRITE | $PERMISSION_PUBLISH | $PERMISSION_USER_MANAGER },
           "help|h"		=> \$help);
    print_usage() if $help;
    die "Permission is required\n" if !$permission;
    die "Database name is required\n" if !$dbname;
    die "User name to give permissions to is required\n" if !$username;
    die "Permission value to set is required\n" if !$permission;
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
    -u|--username <username_for_user_to_set_permission_to>
    -r|--read
    -w|--write
    -c|--publish
    -m|--user_manager
    -a|--all
    [-h|--help]

    -t: file containing track names (one per line)
    -x: permission for track (integer)
    -r: grant read permissions
    -w: grant write permission
    -c: grant publish (writing to Chado) permission
    -m: grant user manager permission (can use the web interface to add
        new users and change permissions)
    -a: grant all permissions (read/write/publish/user manager)

    NOTE: You can combine the -r -w -c -m options to give an user any
          any combination of those options (e.g., -rw for read/write)
END
}

sub set_permissions {
    my $user_id = get_user_id();
    my $existing_permissions = get_existing_permissions($user_id);
    my $track_hash = get_track_ids();
    my $insert_sql = "INSERT INTO $PERMISSIONS_TABLE VALUES(?, ?, ?)";
    my $insert_sth = $dbh->prepare($insert_sql);
    my $update_sql = "UPDATE $PERMISSIONS_TABLE SET permission=? " .
             "WHERE track_id=? AND user_id=?";
    my $update_sth = $dbh->prepare($update_sql);
    while (my $track = $tracks->getline()) {
        chomp $track;
        my $track_id = $track_hash->{$track} || die "Track does not exist in database yet. Please recheck seqids";
        print "Processing $track\n";
        if (exists $existing_permissions->{$track}) {
            $update_sth->bind_param(1, $permission);
            $update_sth->bind_param(2, $track_id);
            $update_sth->bind_param(3, $user_id);
            $update_sth->execute();
        }
        else {
            $insert_sth->bind_param(1, $track_id);
            $insert_sth->bind_param(2, $user_id);
            $insert_sth->bind_param(3, $permission);
            $insert_sth->execute();
        }
    }
}

sub get_existing_permissions {
    my $user_id = shift;
    my %existing_permissions = ();
    my $sql = "SELECT track_name, permission " .
          "FROM $PERMISSIONS_TABLE " .
          "INNER JOIN $TRACKS_TABLE USING(track_id) " .
          "INNER JOIN $USERS_TABLE USING(user_id) " .
          "WHERE user_id = $user_id";
    my $sth = $dbh->prepare($sql);
    $sth->execute();
    while (my $row = $sth->fetchrow_arrayref()) {
        $existing_permissions{$row->[0]} = $row->[1];
    }
    return \%existing_permissions;
}

sub get_user_id {
    my $sql = "SELECT user_id FROM $USERS_TABLE WHERE username='$username'";
    my $rows = $dbh->selectall_arrayref($sql);
    die "User '$username' does not exist in database\n"
        if !scalar(@{$rows});
    return $rows->[0]->[0];
}

sub get_track_ids {
    my %track_hash = ();
    my $sql = "SELECT track_id,track_name FROM $TRACKS_TABLE";
    my $sth = $dbh->prepare($sql);
    $sth->execute();
    while (my $row = $sth->fetchrow_arrayref()) {
        $track_hash{$row->[1]} = $row->[0];
    }
    return \%track_hash;
}

sub cleanup {
    $dbh->disconnect();
}
