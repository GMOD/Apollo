#!/usr/bin/perl

use strict;
use warnings;

use DBI;
use Getopt::Long qw(:config no_ignore_case bundling);
use File::Basename;
use Digest::MD5;

my $USER_TABLE = "users";

my $dbh;
my $username;
my $password;

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
	GetOptions("host|H=s"		=> \$host,
		   "port|X=s"		=> \$port,
		   "dbname|D=s"		=> \$dbname,
		   "dbusername|U=s"	=> \$dbusername,
		   "dbpassword|P=s"	=> \$dbpassword,
		   "username|u=s"	=> \$username,
		   "password|p=s"	=> \$password,
		   "help|h"		=> \$help);
	print_usage() if $help;
	die "Database name is required\n" if !$dbname;
	die "User name for new user required\n" if !$username;
	die "Password for new user required\n" if !$password;
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
	-p|--password <password_for_user_to_be_added>
	[-h|--help]
END
}

sub add_user {
	if (user_exists()) {
		print "User $username already exists\n";
		return;
	}
	my $md5 = new Digest::MD5();
	$md5->add($password);
	my $digest = $md5->hexdigest();
	my $sql = "INSERT INTO $USER_TABLE(username, password) " .
		  "VALUES('$username', '$password')";
	$dbh->do($sql);
}

sub user_exists {
	my $sql = "SELECT * FROM $USER_TABLE WHERE username='$username'";
	my $results = $dbh->selectall_arrayref($sql);
	return scalar(@{$results});
}

sub cleanup {
	$dbh->disconnect();
}
