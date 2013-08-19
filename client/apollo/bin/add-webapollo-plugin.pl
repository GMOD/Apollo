#!/usr/bin/env perl
use strict;
use warnings;

use FindBin qw($RealBin);
use lib "$RealBin/../src/perl5";
use Getopt::Long qw(:config no_ignore_case bundling);
use IO::File;
use Pod::Usage;
use JSON;
# use JSON 2;

my $in_file;
my $out_file;
my $plugin_url = "./plugins/WebApollo";
my $help;

parse_options();

sub parse_options {
  GetOptions("in|i=s"		=> \$in_file,
             "out|o=s"		=> \$out_file,
             "url|u=s"            => \$plugin_url, 
             "help|h"		=> \$help);
  die print_usage() if !$in_file;
  die print_usage() if $help;
  if (!$out_file) { $out_file = $in_file; }
}

sub print_usage {
	die << "END";
usage: add-webapollo-plugin.pl 
	-i|--in   <input_trackList.json>
	[-o|--out <output_trackList.json>]
	[-u|--url <root url for plugin>] 
	[-h|--help]

	i: input trackList.json file (required)
	o: output trackList.json file [default: input trackList.json]
	u: URL to WebApollo plugin (can be relative or absolute) [default: ./plugins/WebApollo]
END
}

my $j = JSON->new->pretty->relaxed;


my $user_track_json = '
      {
         "label" : "Annotations",
         "type" : "WebApollo/View/Track/AnnotTrack", 
	 "storeClass" : "WebApollo/Store/SeqFeature/ScratchPad", 
            "autocomplete" : "none",
            "style" : {
	       "renderClassName" : "annot-render", 
	       "uniqueIdField" : "id", 
               "className" : "annot",
	       "subfeatureClasses":{
			"CDS":"annot-CDS",
			"UTR":"annot-UTR", 
			"exon":"container-100pct", 
			"wholeCDS":null, 
			"non_canonical_five_prime_splice_site" : "noncanonical-splice-site", 
			"non_canonical_three_prime_splice_site" : "noncanonical-splice-site" 
		},
		"centerSubFeature": {
			"non_canonical_five_prime_splice_site" : false, 
			"non_canonical_three_prime_splice_site" : false
		}, 
               "arrowheadClass" : "annot-arrowhead"
            },
            "phase" : 0,
            "compress" : 0,
            "subfeatures" : 1, 
         "key" : "User-created Annotations"
      }';

my $user_track = $j->decode($user_track_json);

# check the track JSON structure
$user_track->{label} or die "invalid internal track JSON: missing a label property\n";

open my $in, '<', $in_file or die "error opening $in_file: $!";
my $tracklist_json = do { local $/; <$in> };
$in->close();
my $tracklist_data = $j->decode($tracklist_json);
# check the trackList JSON structure
$tracklist_data->{tracks} or die "invalid tracklist: missing a tracks property\n";
print "parsed tracklist from file: " . $in_file . "\n";

my $apollo_plugin =  {
    name => "WebApollo", 
    location => $plugin_url
};

my $plugin_is_new = 1;
for( my $i = 0; $i < @{$tracklist_data->{plugins}|| []}; $i++ ) {
  my $plugin = $tracklist_data->{plugins}[$i];
  if( $plugin->{name} eq $apollo_plugin->{name} ) {
    $tracklist_data->{plugins}[$i] = $apollo_plugin;
    $plugin_is_new = 0;
  }  
}

if ($plugin_is_new)  {
  push @{ $tracklist_data->{plugins} ||= [] }, $apollo_plugin;
}

$tracklist_data->{alwaysOnTracks} = "DNA,Annotations";

my $track_is_new = 1;
for( my $i = 0; $i < @{$tracklist_data->{tracks}|| []}; $i++ ) {
  my $track = $tracklist_data->{tracks}[$i];
  if( $track->{label} eq $user_track->{label} ) {
    $tracklist_data->{tracks}[$i] = $user_track;
    $track_is_new = 0;
  }
}

if ($track_is_new) {
  push @{ $tracklist_data->{tracks} ||= [] }, $user_track;
}

open my $out, '>', $out_file or die "$! writing $out_file";
print $out $j->encode($tracklist_data);
$out->close();
print "output modified trackList to file: " . $out_file . "\n";
