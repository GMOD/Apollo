#!/usr/bin/perl

use strict;
use warnings;


use FindBin qw($RealBin);
use lib "$RealBin/../../src/perl5";
use JBlibs;

use DBI;
use Getopt::Long qw(:config no_ignore_case bundling);
use File::Basename;
use Crypt::PBKDF2;

my $USER_TABLE = "users";

my $dbh;
my $username;
my $password;
my $unencrypted=1;

parse_options();
add_user();
cleanup();

sub parse_options {
    my $host = "localhost";
    my $port = 5432;
    my $dbname = $ENV{WEB_APOLLO_DB};
    my $dbusername = $ENV{WEB_APOLLO_DB_USER};
    my $dbpassword = $ENV{WEB_APOLLO_DB_PASS};
    my $help;
    GetOptions("host|H=s"    	=> \$host,
           "port|X=s"		=> \$port,
           "dbname|D=s"		=> \$dbname,
           "dbusername|U=s"	=> \$dbusername,
           "dbpassword|P=s"	=> \$dbpassword,
           "username|u=s"	=> \$username,
           "password|p=s"	=> \$password,
           "help|h"		=> \$help,
           "unencrypted|x" => \$unencrypted);

    print_usage() if $help;
    die "Database name is required\n" if !$dbname;
    die "User name for new user required\n" if !$username;
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
    -u|--username <username_for_user_to_be_added>
    [-p|--password <password_for_user_to_be_added>]
    [-x|--unencrypted]
    [-h|--help]
END
}

sub add_user {
    if (user_exists()) {
        print "User $username already exists\n";
        return;
    }

    if(!$password) { 
        print "Enter a password for $username: ";
        $password =<STDIN>;
        chomp($password);
    }

    my $sql="";
    if($unencrypted) {
        print "Encrypted $encrypted";
        $sql = "INSERT INTO $USER_TABLE(username, password) " . 
                      "VALUES(?, ?)";
        $dbh->do($sql, undef, $username, $password);       
    }

    else {
        my $pbkdf2 = Crypt::PBKDF2->new(
            hash_class => 'HMACSHA1', # this is the default
            iterations => 1000,      # so is this
            output_len => 20,        # and this
            salt_len => 4,           # and this.
            encoding => 'crypt',
        );  

        my $hash = $pbkdf2->generate($password);

        $sql = "INSERT INTO $USER_TABLE(username, password) " .
              "VALUES(?, ?)";
        $dbh->do($sql, undef, $username, $hash);

    }
}

sub user_exists {
    my $sql = "SELECT * FROM $USER_TABLE WHERE username='$username'";
    my $results = $dbh->selectall_arrayref($sql);
    return scalar(@{$results});
}

sub cleanup {
    $dbh->disconnect();
}
