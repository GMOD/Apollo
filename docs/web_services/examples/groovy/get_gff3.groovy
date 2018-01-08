#!/usr/bin/env groovy
scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent

@Grab(group = 'org.json', module = 'json', version = '20140107')
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7.2')


import groovyx.net.http.HTTPBuilder


String usageString = "get_gff3.groovy <options>" +
        "Example: \n" +
        "./get_gff3.groovy -username admin -password somepass -organism honeybee -url http://localhost/apollo "

def cli = new CliBuilder(usage: 'get_gff3.groovy <options>')
cli.setStopAtNonOption(true)
cli.url('URL of Apollo from which GFF3 is to be fetched', required: true, args: 1)
cli.username('username', required: false, args: 1)
cli.password('password', required: false, args: 1)
cli.output('output file', required: false, args: 1)
cli.organism('organism', required: false, args: 1)
cli.sequences('sequences / chromosomes to export from', required: false, args: 1)
cli.export_sequence('export raw genome FASTA sequence', required: false)
cli.ignoressl('Use this flag to ignore SSL issues', required: false)
OptionAccessor options
def admin_username
def admin_password
def export_all_sequences = true 
try {
    options = cli.parse(args)

    if (!(options?.url)) {
        println "Requires destination URL\n" + usageString
        return
    }

    def cons = System.console()
    if(cons) {
        if (!(admin_username=options?.username)) {
            admin_username = new String(cons.readPassword('Username: ') )
        }
        if (!(admin_password=options?.password)) {
            admin_password = new String(cons.readPassword('Password: ') )
        }
    }
    else if(!options?.username||!options?.password) {
        System.err.println("Error: missing -username and -password and can't read them when using redirect");
        if(!options.output) throw "Require output file"
    }
    else {
        admin_password=options.password
        admin_username=options.username
    }

	if(options.sequences){
		export_all_sequences = false
	}
	else{
		sequences = []
	}
} catch (e) {
    println(e)
    return
}


def http = new HTTPBuilder(options.url)


// just get data
def post=[
    username: admin_username,
    password: admin_password,
    format: 'plain',
    type: 'GFF3',
    exportGff3Fasta: options.export_sequence,
    exportAllSequences: export_all_sequences,
    sequences: options.sequences,
    organism: options.organism,
    output:'text'
]

http.get(path: options.url+'/IOService/write/',query: post) { resp, reader ->
  if(options.output) {
      def file=new File(options.output)
      def writer = new PrintWriter(file)
      writer << reader
      writer.close()
  }
  else
      System.out << reader
}

