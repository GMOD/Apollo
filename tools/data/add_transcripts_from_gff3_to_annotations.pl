#!/usr/bin/perl

use strict;
use warnings;

use FindBin qw($RealBin);
use lib "$RealBin/../../src/perl5";
use JBlibs;

use File::Basename;
use lib dirname($0);

use Bio::GFF3::LowLevel::Parser;
use Getopt::Long qw(:config no_ignore_case bundling);
use IO::File;
use LWP::UserAgent;
use JSON;

my $gene_types_in = "gene";
my $transcript_types_in = "transcript|mRNA";
my $exon_types_in = "exon";
my $cds_types_in = "CDS";
my $ontology = "sequence";
my $gene_type_out = "gene";
my $transcript_type_out = "mRNA";
my $exon_type_out = "exon";
my $cds_type_out = "CDS";
my $annotation_track_prefix = "Annotations-";
my $property_ontology = "feature_property";
my $comment_type_out = "comment";
my $property_type_out = "feature_property";
my $in = \*STDIN;
my $username;
my $password;
my $url;
my $session_id;
my $name_attributes

my $success_log;
my $error_log;

my $chunk_size = 100;

my $json = new JSON();
my $ua = new LWP::UserAgent();
$ua->timeout(3000); # 5 minutes

my %source_features = ();
my %skip_ids = ();

my %reserved_properties = (     symbol      => 1,
                                description => 1,
                                status      => 1
                          );
my %ignored_properties = (  owner               => 1,
                            date_creation       => 1,
                            date_last_modified  => 1
                         );

$| = 1;

parse_options();
login();
process_gff();
logout();

sub parse_options {
    my $help;
    my $input_file;
    my $success_log_file;
    my $error_log_file;
    my $skip_file;
    GetOptions("input|i=s"          => \$input_file,
           "username|u=s"       => \$username,
           "password|p=s"       => \$password,
           "url|U=s"            => \$url,
           "gene_types_in|g=s"      => \$gene_types_in,
           "transcript_types_in|t=s"    => \$transcript_types_in,
           "exon_types_in|e=s"      => \$exon_types_in,
           "cds_types_in|d=s"       => \$cds_types_in,
           "ontology|O=s"       => \$ontology,
           "gene_type_out|G=s"      => \$gene_type_out,
           "transcript_type_out|T=s"    => \$transcript_type_out,
           "exon_type_out|E=s"      => \$exon_type_out,
           "cds_type_out|D=s"       => \$cds_type_out,
           "property_ontolgy|R=s"   => \$property_ontology,
           "comment_type_out|C=s"   => \$comment_type_out,
           "property_type_out|S=s"  => \$property_type_out,
           "track_prefix|P=s"       => \$annotation_track_prefix,
           "success_log|l=s"        => \$success_log_file,
           "error_log|L=s"      => \$error_log_file,
           "skip|s=s"           => \$skip_file,
           "help|h"         => \$help,
           "name_attributes=s"   => \$name_attributes);
           
    print_usage() if $help;
    $in = new IO::File($input_file) or die "Error reading $input_file: $!\n"
        if $input_file;
    $success_log = new IO::File($success_log_file, "w") or die "Error writing success log $success_log_file: $!\n" if $success_log_file;
    $error_log = new IO::File($error_log_file, "w") or die "Error writing error log $error_log_file: $!\n" if $error_log_file;
    if ($skip_file) {
        my $fh = new IO::File($skip_file) or die "Error reading $skip_file: $!\n";
        while (my $id = <$fh>) {
            chomp $id;
            ++$skip_ids{$id};
        }
    }
    die "Missing required parameter: username\n" if !$username;
    die "Missing required parameter: password\n" if !$password;
    die "Missing required parameter: url\n" if !$url;
    $url = "http://$url" if $url !~ m%https?://%;
}

sub print_usage {
    my $progname = basename($0);
    die << "END";
usage: $progname
    --url|-U <URL to WebApollo instance>
    --username|-u <username>
    --password|-p <password>
    [--genes_types_in|-g <gene types for input>]
    [--transcript_types_in|-t <transcript types for input>]
    [--exon_types_in|-e <exon types for input>]
    [--cds_types_in|-d <CDS types for input>]
    [--ontology|-O <ontology name used in server>]
    [--gene_type_out|-G <gene type used in server>]
    [--transcript_type_out|-T <transcript type used in server>]
    [--exon_type_out|-E <exon type used in server>]
    [--cds_type_out|-D <CDS type used in server>]
    [--property_ontology|-R <property ontology name used in server>]
    [--comment_type_out|-C <comment type used in server>]
    [--property_type_out|-S <feature property type used in server>]
    [--track_prefix|-P <annotation track prefix>]
    [--input|-i <GFF3 file>]
    [--success_log|-l <success log file>]
    [--error_log|-L <error log file>]
    [--skip|-s <skip id file>]
    [--help|-h]
    [--name_attributes <feature attribute to be used as name, first found used>]

    U: URL to WebApollo instance
    u: username to access WebApollo
    p: password to access WebApollo
    g: string/regex to define the GFF3 types to treat as genes 
       [default: "$gene_types_in"]
    t: string/regex to define the GFF3 types to treat as transcripts
       [default: "$transcript_types_in"]
    e: string/regex to define the GFF3 types to treat as exons 
       [default: "$exon_types_in"]
    O: ontology name used in WebApollo instance
       [default: "$ontology"]
    G: gene type used in WebApollo instance
       [default: "$gene_type_out"]
    T: transcript type used in WebApollo instance
       [default: "$transcript_type_out"]
    E: exon type used in WebApollo instance
       [default: "$exon_type_out"]
    R: property ontology name used in WebApollo instance
       [default: "$property_ontology"]
    C: comment type used in WebApollo instance
       [default: "$comment_type_out"]
    S: feature property type used in WebApollo instance
       [default: "$property_type_out"]
    P: annotation track prefix
       [default: "$annotation_track_prefix"]
    i: input GFF3 file
       [default: STDIN]
    l: log file for ids that were successfully processed
    L: log file for ids that were erroneously processed
    s: file with ids to skip
    h: this help screen
END
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

sub process_gff {
    my $gffio = Bio::GFF3::LowLevel::Parser->open($in);
    my $seq_ids_to_transcripts = {};
    my $seq_ids_to_genes = {};
    while (my $features = $gffio->next_item()) {
        next if ref $features ne "ARRAY";
        process_gff_entry($features, $seq_ids_to_genes, $seq_ids_to_transcripts);
    }
    write_features("add_feature", $seq_ids_to_genes);
    write_features("add_transcript", $seq_ids_to_transcripts);
    if (!scalar(keys(%{$seq_ids_to_genes})) && !scalar(keys(%{$seq_ids_to_transcripts}))) {
        print "No genes of type \"$gene_types_in\" or transcripts of type \"$transcript_types_in\" found\n";
    }
}

sub write_features {
    my $operation = shift;
    my $data = shift;
    while (my ($seq_id, $features) = each(%{$data})) {
        print "Processing $seq_id\n";
        my @to_be_written = ();
        my @ids = ();
        my @error = ();
        my $chunk = 0;
        for (my $i = 0; $i < scalar(@{$features}); ++$i) {
            my $feature = $features->[$i];
            push @to_be_written, $feature->[0];
            push @ids, $feature->[1];
            if (($i + 1) % $chunk_size == 0 || $i == scalar(@{$features}) - 1) {
                print "Processing chunk " . ++$chunk . ": ";
                my $res = send_request($seq_id, $operation, \@to_be_written);
                if ($res->{error}) {
                    print "error: $res->{error}\n";
                    for (my $j = 0; $j < scalar(@to_be_written); ++$j) {
                        push @error, [$to_be_written[$j], $ids[$j]];
                    }
                }
                else {
                    print "success\n";
                    foreach my $id (@ids) {
                        print $success_log "$id\n" if $success_log;
                    }
                }
                @to_be_written = ();
                @ids = ();
            }
        }
        if (scalar(@error)) {
            print "Processing individual features for failed chunks\n";
            foreach my $feature (@error) {
                print "Processing $feature->[1]: ";
                my $res = send_request($seq_id, $operation, [$feature->[0]]);
                if ($res->{error}) {
                    print "error: $res->{error}\n";
                    print $error_log "$feature->[1]\n" if $error_log;
                }
                else {
                    print "success\n";
                    print $success_log "$feature->[1]\n" if $success_log;
                }
            }
        }
    release_resources($seq_id);
    }
}

sub process_feature {
    my $features = shift;
    my $type = get_type($features);

    if ($type =~ /$gene_types_in/) {
        return process_gene($features);
    }
    elsif ($type =~ /$transcript_types_in/) {
        return process_transcript($features);
    }
    return undef;
}

sub convert_feature {
    my $features = shift;
    my $type = shift;
    my $name_attributes = shift;
    my $start = get_start($features);
    my $end = get_end($features);
    my $strand = get_strand($features);
    my $name = get_name($features,$name_attributes)
    my $comments = get_comments($features);
    my $dbxrefs = get_dbxrefs($features);
    my $ontology_terms = get_ontology_terms($features);
    my $json_feature = {
        location => {
            fmin => $start - 1,
            fmax => $end,
            strand => $strand
        },
        type => {
            cv => {
                name => $ontology
            },
            name => $type
        }
    };
    if ($name) {
        $json_feature->{name} = $name;
    }
    my $json_properties = [];
    foreach my $comment (@{$comments}) {
        my $json_comment = {
            value => $comment,
            type => {
                cv => {
                    name => $property_ontology
                },
                name => $comment_type_out
            }
        };
        push @{$json_properties}, $json_comment;
    }
    foreach my $feature (@{$features}) {
        while (my ($tag, $values) = each(%{$feature->{attributes}})) {
            next if $tag =~ /^[A-Z]/;
            next if $ignored_properties{$tag};  
            my $type = $reserved_properties{$tag} ? $tag : $property_type_out;
            foreach my $value (@{$values}) {
                $value = $reserved_properties{$tag} ? $value : "$tag=$value";
                my $json_property = {
                    value => $value,
                    type => {
                        cv => {
                            name => $property_ontology
                        },
                        name => $type
                    }
                };
                push @{$json_properties}, $json_property;
            }
        }
        if (scalar(@{$json_properties})) {
            $json_feature->{properties} = $json_properties;
        }
    }
    my $json_dbxrefs = [];
    foreach my $dbxref (@{$dbxrefs}) {
        my ($db, $accession) = split ":", $dbxref;
        my $json_dbxref = {
            accession => $accession,
            db => {
                name => $db 
            }
        };
        push @{$json_dbxrefs}, $json_dbxref;
    }
    foreach my $ontology_term (@{$ontology_terms}) {
        my @ontology_term_parts = split /:/, $ontology_term;
        my $json_ontology_term = {
            accession => $ontology_term_parts[1],
            db => {
                name => $ontology_term_parts[0]
            }
        };
        push @{$json_dbxrefs}, $json_ontology_term;
    }
    if (scalar(@{$dbxrefs})) {
        $json_feature->{dbxrefs} = $json_dbxrefs;
    }
    return $json_feature;
}

sub get_editor_url {
    return "$url/AnnotationEditorService;jsessionid=$session_id";
}

sub get_source_feature {
    my $seq_id = shift;
    my $source_feature = $source_features{$seq_id};
    if ($source_feature) {
        return $source_feature;
    }
    my $operation = "get_source_feature";
    my $features = send_request($seq_id, $operation);
    $source_feature = $features->{features}->[0];
    $source_features{$seq_id} = $source_feature;
    release_resources($seq_id) if $source_feature;
    return $source_feature;
}

sub release_resources {
    my $seq_id = shift;
    my $operation = "release_resources";
    send_request($seq_id, $operation);
}

sub send_request {
    my $seq_id = shift;
    my $operation = shift;
    my $features = shift;
    my $request = {
            track => "$annotation_track_prefix$seq_id",
            operation => $operation
    };
    if ($features) {
        foreach my $feature (@{$features}) {
            push(@{$request->{features}}, $feature);
        }
    }
    my $req = new HTTP::Request();
    $req->method("POST");
    $req->uri(get_editor_url());
    $req->content($json->encode($request));
    my $res = $ua->request($req);
    if ($res->is_success()) {
        return $json->decode($res->content());
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

sub get_name {
    my $features = shift;
    my $name_attributes = shift;
    my @attrs = split /\|/, $name_attributes;
    my $name;
    ATTR:
    foreach my $attr (@attrs) {
        $name = $features->[0]->{attributes}->{$attr}->[0];
        last ATTR unless !$name;
    }
    return $name;
}

sub get_comments {
    my $features = shift;
    my @comments;
    foreach my $feature (@{$features}) {
        my $values = $feature->{attributes}->{Note};
        push @comments, @{$values} if $values;
    }
    return \@comments;
}

sub get_dbxrefs {
    my $features = shift;
    my @dbxrefs;
    foreach my $feature (@{$features}) {
        my $values = $feature->{attributes}->{Dbxref};
        push @dbxrefs, @{$values} if $values;
    }
    return \@dbxrefs;
}

sub get_ontology_terms {
    my $features = shift;
    my @ontology_terms;
    foreach my $feature (@{$features}) {
        my $values = $feature->{attributes}->{Ontology_term};
        push @ontology_terms, @{$values} if $values;
    }
    return \@ontology_terms;
}

sub get_seq_id {
    my $features = shift;
    return $features->[0]->{seq_id};
}

sub get_start {
    my $features = shift;
    my $start = undef;
    foreach my $feature (@{$features}) {
        if (!defined $start || $feature->{start} < $start) {
            $start = $feature->{start};
        }
    }
    return int($start);
}

sub get_end {
    my $features = shift;
    my $end = undef;
    foreach my $feature (@{$features}) {
        if (!defined $end || $feature->{end} > $end) {
            $end = $feature->{end};
        }
    }
    return int($end);
}

sub get_strand {
    my $features = shift;
    my $strand = $features->[0]->{strand};
    if ($strand eq "+") {
        return 1;
    }
    elsif ($strand eq "-") {
        return -1;
    }
    return 0;
}

sub get_id {
    my $features = shift;
    return $features->[0]->{attributes}->{ID}->[0];
}

sub get_type {
    my $features = shift;
    return $features->[0]->{type};
}

sub validate_feature {
    my $features = shift;
    my $type = shift;
    my $seq_id = get_seq_id($features);
    my $source_feature = get_source_feature($seq_id);
    my $start = get_start($features);
    my $end = get_end($features);
    my $id = get_id($features);
    my $strand = get_strand($features);
    if (!$source_feature) {
        printf("Error writing $id: Refseq %s does not exist [%s, %d, %d, %d]\n", $seq_id, $id, $start, $end, $strand);
        print $error_log "$id\n" if $error_log;
        return 0;
    }
    if ($start < $source_feature->{fmin} + 1 || $end > $source_feature->{fmax}) {
        printf("Error writing $id: $type extends beyond refseq boundaries [%s, %d, %d] [%s, %d, %d, %d]\n", $source_feature->{uniquename}, $source_feature->{fmin}, $source_feature->{fmax}, $id, $start, $end, $strand);
        print $error_log "$id\n" if $error_log;
        return 0;
    }
    return 1;
}

sub process_gene {
    my $features = shift;
    if (!validate_feature($features, "Gene")) {
        return undef;
    }
    my $gene = convert_feature($features, $gene_type_out, $name_attributes);
    my $subfeatures = get_subfeatures($features);
    foreach my $subfeature (@{$subfeatures}) {
        my $type = get_type($subfeature);
        if ($type =~ /$transcript_types_in/) {
            my $transcript = process_transcript($subfeature);
            push(@{$gene->{children}}, $transcript) if $transcript;
        }   
    }
    return $gene;
}

sub process_transcript {
    my $features = shift;
    if (!validate_feature($features, "Transcript")) {
        return undef;
    }
    my $transcript = convert_feature($features, $transcript_type_out, $name_attributes);
    my $cds_feature = undef;
    my $subfeatures = get_subfeatures($features);
    foreach my $subfeature (@{$subfeatures}) {
        my $type = get_type($subfeature);
        if ($type =~ /$exon_types_in/) {
            my $exon = convert_feature($subfeature, $exon_type_out,$name_attributes);
            push(@{$transcript->{children}}, $exon);
        }   
        elsif ($type =~ /$cds_types_in/) {
            my $start = get_start($subfeature);
            my $end = get_end($subfeature);
            if (!defined $cds_feature) {
                $cds_feature = $subfeature;
            }
            else {
                my $cds_start = get_start($cds_feature);
                my $cds_end = get_end($cds_feature);
                if ($start < $cds_start) {
                    $cds_feature->[0]->{start} = $start;
                }
                if ($end > $cds_end) {
                    $cds_feature->[0]->{end} = $end;
                }
            }
        }
    }
    if ($cds_feature) {
        my $cds = convert_feature($cds_feature, $cds_type_out, $name_attributes);
        push(@{$transcript->{children}}, $cds);
    }
    return $transcript;
}

sub get_subfeatures {
    my @subfeatures;
    my $features = shift;
    foreach my $feature (@{$features}) {
        push @subfeatures, @{$feature->{child_features}};
    }
    return \@subfeatures;
}

sub process_gff_entry {
    my $features = shift;
    my $seq_ids_to_genes = shift;
    my $seq_ids_to_transcripts = shift;
    my $type = get_type($features);
    if ($type !~ /$gene_types_in/ && $type !~ /$transcript_types_in/) {
        foreach my $subfeature (@{get_subfeatures($features)}) {
            process_gff_entry($subfeature, $seq_ids_to_genes, $seq_ids_to_transcripts);
        }
    }
    else {
        my $json_feature = process_feature($features);
        if ($json_feature) {
            my $id = get_id($features);
            if ($skip_ids{$id}) {
                print "Skipping $id\n";
                next;
            }
            my $seq_id = get_seq_id($features);
            if ($json_feature->{type}->{name} =~ /$gene_types_in/) {
                push(@{$seq_ids_to_genes->{$seq_id}}, [$json_feature, $id]);
            }
            else {
                push(@{$seq_ids_to_transcripts->{$seq_id}}, [$json_feature, $id]);
            }
        }
    }
}
