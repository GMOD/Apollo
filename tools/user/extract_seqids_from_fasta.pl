#!/usr/bin/perl

use strict;
use warnings;


use FindBin qw($RealBin);
use lib "$RealBin/../../src/perl5";
use JBlibs;


use Getopt::Long qw(:config no_ignore_case bundling);
use IO::File;
use File::Basename;

my $in = \*STDIN;
my $out = \*STDOUT;
my $prefix = "";

parse_options();
extract_seqids();

sub parse_options {
    my $in_file;
    my $out_file;
    my $help;
    GetOptions("in|i=s"     => \$in_file,
           "out|o=s"    => \$out_file,
           "prefix|p=s" => \$prefix,
           "help|h"     => \$help);
    print_usage() if $help;
    $in = new IO::File($in_file) or die "Error reading input FASTA: $!" if $in_file;
    $out = new IO::File($out_file, "w") or die "Error writing seqids: $!" if $out_file;
}

sub print_usage {
    my $progname = basename($0);
    die << "END";
usage: $progname
    [-i|--in <input_fasta>]
    [-o|--out <output_for_seqids>]
    [-p|--prefix <prefix_to_add_to_identifiers>]

    -i: input fasta [default: stdin]
    -o: output for seqids [default: stdout]
    -p: prefix to be added to each identifier [default: none]
END
}

sub extract_seqids {
    while (my $line = <$in>) {
        if ($line =~ /^>/) {
            $line =~ />(\S+)/;
            print $out "$prefix$1\n";
        }
    }
    $out->close();
}
