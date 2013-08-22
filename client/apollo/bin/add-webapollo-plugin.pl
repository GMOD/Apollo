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
my $favicon_url;
my $help;

parse_options();

sub parse_options {
  GetOptions("in|i=s"		=> \$in_file,
             "out|o=s"		=> \$out_file,
             "url|u=s"            => \$plugin_url, 
             "favicon|f=s"        => \$favicon_url, 
             "help|h"		=> \$help);
  die print_usage() if !$in_file;
  die print_usage() if $help;
  if (!$out_file) { $out_file = $in_file; }
}

if (! $favicon_url)  { $favicon_url = $plugin_url . "/img/webapollo_favicon.ico"; }

sub print_usage {
	die << "END";
usage: add-webapollo-plugin.pl 
	-i|--in   <input_trackList.json>
	[-o|--out <output_trackList.json>]
	[-u|--url <root url for plugin>] 
        [-f|--fav <url for favicon>]
	[-h|--help]

	i: input trackList.json file (required)
	o: output trackList.json file [default: input trackList.json]
	u: URL to WebApollo plugin (can be relative or absolute) [default: ./plugins/WebApollo]
	f: URL for favicon [default: (WebApollo plugin URL)/img/webapollo_favicon.ico]
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
# legacy way of specifying WebApollo plugin just as "WebApollo" string, should eliminate;
if ($tracklist_data->{plugins} && ($tracklist_data->{plugins} eq "WebApollo")) {
    $tracklist_data->{plugins} = [];
}
for( my $i = 0; $i < @{$tracklist_data->{plugins}|| []}; $i++ ) {
  my $plugin = $tracklist_data->{plugins}[$i];
  if( $plugin->{name} && ( $plugin->{name} eq $apollo_plugin->{name} ) ) {
    $tracklist_data->{plugins}[$i] = $apollo_plugin;
    $plugin_is_new = 0;
  }  
}
if ($plugin_is_new)  {
  push @{ $tracklist_data->{plugins} ||= [] }, $apollo_plugin;
}

$tracklist_data->{alwaysOnTracks} = "DNA,Annotations";
$tracklist_data->{favicon} = $favicon_url;
$tracklist_data->{share_link} = 0;

my $track_is_new = 1;
for( my $i = 0; $i < @{$tracklist_data->{tracks}|| []}; $i++ ) {
  my $track = $tracklist_data->{tracks}[$i];
  if( $track->{label} && ( $track->{label} eq $user_track->{label} ) ) {
    $tracklist_data->{tracks}[$i] = $user_track;
    $track_is_new = 0;
  }
}

if ($track_is_new) {
  push @{ $tracklist_data->{tracks} ||= [] }, $user_track;
}

# trying to support backward compatibility, 
# if have previous include of "annotation_trackList.json" file, then remove 
#     since annotation track is now inserted directly into main trackList.json file
#     but, preserve any other includes
if (exists $tracklist_data->{include})  {
  my $new_include = [];
  for( my $i = 0; $i < @{$tracklist_data->{include}|| []}; $i++ ) {
    my $include_file = $tracklist_data->{include}[$i];
    if( $include_file ne "annotation_trackList.json") { 
      push(@$new_include, $include_file);
    }
  }
  if (@$new_include) {  
    $tracklist_data->{include} = $new_include;
  }
  else  { delete $tracklist_data->{include}; }
}

open my $out, '>', $out_file or die "$! writing $out_file";
print $out $j->encode($tracklist_data);
$out->close();
print "output modified trackList to file: " . $out_file . "\n";
