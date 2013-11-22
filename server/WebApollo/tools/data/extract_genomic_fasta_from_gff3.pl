#!/usr/bin/perl

use strict;
use warnings;

use Getopt::Long qw(:config no_ignore_case bundling);
use IO::File;
use File::Basename;

my $in = \*STDIN;
my $out = \*STDOUT;

parse_options();
extract_seqs();

sub parse_options {
	my $in_file;
	my $out_file;
	my $help;
	GetOptions("in|i=s"     => \$in_file,
		   "out|o=s"    => \$out_file,
		   "help|h"     => \$help);
	print_usage() if $help;
	$in = new IO::File($in_file) or die "Error reading input GFF3: $!"
		if $in_file;
	$out = new IO::File($out_file, "w") or die "Error writing FASTA: $!"
		if $out_file;
}

sub print_usage {
	my $progname = basename($0);
	die << "END";
usage: $progname
	[-i|--in <input_gff3>]
	[-o|--out <output_for_fasta>]

	-i: input fasta [default: stdin]
	-o: output for seqids [default: stdout]
END
}

sub extract_seqs {
	my %ids;
	my $fasta = 0;
	my $valid = 0;
	while (my $line = <$in>) {
		$fasta = 1 if ($line =~ /(^##FASTA|^>)/);
		if (!$fasta) {
			my @tokens = split("\t", $line);
			next if scalar(@tokens) != 9;
			++$ids{$tokens[0]};
		}
		else {
			if ($line =~ /^>(\w+)/) {
				if ($ids{$1}) {
					$valid = 1;
					print $out $line;
				}
				else {
					$valid = 0;
				}
			}
			elsif ($valid) {
				print $out $line;
			}
		}
	}
}
