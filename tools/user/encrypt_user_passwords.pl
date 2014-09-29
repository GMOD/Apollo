#!/usr/bin/perl

use strict;
use warnings;

use DBI;
use Getopt::Long qw(:config no_ignore_case bundling);
use File::Basename;
use Crypt::PBKDF2;

my $USER_TABLE = "users";

my $dbh;
my $username;
my $password;

parse_options();
encrypt_passwords();
cleanup();

sub parse_options {
    my $host = "localhost";
    my $port = 5432;
    my $dbname = $ENV{WEB_APOLLO_DB};
    my $dbusername = $ENV{WEB_APOLLO_DB_USER};
    my $dbpassword = $ENV{WEB_APOLLO_DB_PASS};
    my $help;
    GetOptions("host|H=s"       => \$host,
           "port|X=s"       => \$port,
           "dbname|D=s"     => \$dbname,
           "dbusername|U=s" => \$dbusername,
           "dbpassword|P=s" => \$dbpassword,
           "help|h"     => \$help);
    print_usage() if $help;
    die "Database name is required\n" if !$dbname;
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
    [-h|--help]
END
}

sub encrypt_passwords {
    my $pbkdf2 = Crypt::PBKDF2->new(
        hash_class => 'HMACSHA1', # this is the default
        iterations => 1000,      # so is this
        output_len => 20,        # and this
        salt_len => 4,           # and this.
        encoding => 'crypt',
    );  

    my $passwords = get_passwords();
    foreach my $row (@{$passwords}) {
        my $password = $row->[1];
        if((substr $password, 0, 10) eq '{X-PBKDF2}') {
            print "Database may already be encrypted. Are you sure you want to continue (y/n): ";
            my $input = <STDIN>;
            chomp($input);
            die unless($input eq "Y" or $input eq "y");
        }
    }

    foreach my $row (@{$passwords}) {
        my $user_id = $row->[0];
        my $password = $row->[1];

        print "Updating user id $user_id\n";
        my $hash = $pbkdf2->generate($password);
        my $sql = "UPDATE $USER_TABLE " .
              "SET password='$hash' " .
              "WHERE user_id=$user_id";
        $dbh->do($sql);
    }

}

sub get_passwords {
    my $sql = "SELECT user_id, password FROM $USER_TABLE";
    my $results = $dbh->selectall_arrayref($sql);
    return $results;
}

sub cleanup {
    $dbh->disconnect();
}
