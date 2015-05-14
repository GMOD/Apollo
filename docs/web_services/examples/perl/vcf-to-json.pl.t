use strict;
use warnings;

use JBlibs;

use Test::More;
use Test::Warn;

use Bio::WebApollo::Cmd::VcfToJson;

use File::Spec::Functions qw( catfile catdir );
use File::Temp ();
use File::Copy::Recursive 'dircopy';

use lib 'tests/perl_tests/lib';
use FileSlurping 'slurp';

sub run_with(@) {
    #system $^X, 'bin/flatfile-to-json.pl', @_;
    #ok( ! $?, 'flatfile-to-json.pl ran ok' );
    my @args = @_;
    warnings_are {
      Bio::WebApollo::Cmd::VcfToJson->new( @args )->run;
    } [], 'ran without warnings';
}

sub tempdir {
    my $tempdir   = File::Temp->newdir( CLEANUP => $ENV{KEEP_ALL} ? 0 : 1 );
    #diag "using temp dir $tempdir";
    return $tempdir;
}

{
    my $snv_tempdir = tempdir();

    run_with (
        '--out' => $snv_tempdir,
        '--vcf' => "tests/data/heterozygous_snv.vcf",
        '--trackLabel' => 'testSNV',
        '--key' => 'test SNV',
        '--autocomplete' => 'all',
        );

    my $read_json = sub { slurp( $snv_tempdir, @_ ) };
    my $snv_trackdata = $read_json->(qw( tracks testSNV chr1 trackData.json ));
    is( $snv_trackdata->{intervals}->{nclist}->[0]->[1], 15882, 'start set correctly in json from VCF') or diag explain $snv_trackdata->{intervals}->{nclist}->[1];
    is( $snv_trackdata->{intervals}->{nclist}->[0]->[2], 15883, 'end set correctly in json from VCF') or diag explain $snv_trackdata->{intervals}->{nclist}->[2];
    is( $snv_trackdata->{intervals}->{minStart}, 15882, 'minStart set correctly in json from VCF') or diag explain $snv_trackdata->{intervals}->{minStart};
    is( $snv_trackdata->{intervals}->{maxEnd}, 15883, 'maxEnd set correctly in json from VCF') or diag explain $snv_trackdata->{intervals}->{maxEnd};
    is( $snv_trackdata->{intervals}->{nclist}->[0]->[10], "SNV", 'should set type as SNV in json from VCF') or diag explain $snv_trackdata->{nclist}->[0]->[10];
}


done_testing;
