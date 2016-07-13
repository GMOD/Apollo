#!/usr/bin/env groovy
scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent
evaluate(new File("${scriptDir}/SampleFeatures.groovy"))
evaluate(new File("${scriptDir}/Apollo2Operations.groovy"))

import net.sf.json.JSONArray
import net.sf.json.JSONObject


@Grab(group = 'org.json', module = 'json', version = '20140107')
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7')

String usageString = "stress_test.groovy <options>" +
        "Example: \n" +
        "./stress_test.groovy -iter 100 -concurrency 3 -username ndunn@me.com -password demo  -organism Honey2 -destinationurl http://localhost:8080/Apollo2/ -load 10 -showHeader"

def cli = new CliBuilder(usage: 'stress_test.groovy <options>')
cli.setStopAtNonOption(true)
cli.destinationurl('URL of Apollo instance to which annotations are to be loaded', required: true, args: 1)
cli.organism('organism common name', required: true, args: 1)
cli.username('username', required: true, args: 1)
cli.password('password', required: true, args: 1)
cli.concurrency('concurrent transaction', required: false, args: 1)
cli.iter('iter', required: false, args: 1)
cli.load('load level (0-10)', required: false, args: 1)
cli.showHeader('Show the output header file', required: false, args: 0)

OptionAccessor options

try {
    options = cli.parse(args)

    if (!(options?.destinationurl && options?.organism && options?.username && options?.password)) {
        println "\n" + usageString
        return
    }
} catch (e) {
    println(e)
    return
}

String sequenceName = "Group1.10"
int concurrency = options.concurrency ? Integer.parseInt(options.concurrency) : 1
int iter = options.iter ? Integer.parseInt(options.iter) : 1
int load = options.load ? Integer.parseInt(options.load) : 1
Boolean showHeader = options.showHeader ? true : false
load = load < 1 ? 1 : load
load = load > 10 ? 10 : load



def inputArray = SampleFeatures.getSampleFeatures()
def sampleFeaturesArray = new JSONArray()
for(int i = 0 ; i < load ; i++){
    sampleFeaturesArray.add(inputArray[i])
}
int sampleFeaturesSize = sampleFeaturesArray.size()

List<Long> timingsArray = new ArrayList<>()


for (int i = 0; i < (int) iter; i++) {
    long startTime = System.currentTimeMillis()

    def threads = []
    for (int j = 0; j < (int) concurrency; j++) {

        def thread = new Thread({
            JSONArray deleteArray = new JSONArray()
            def response = Apollo2Operations.triggerAddTranscript(options.destinationurl, options.username, options.password, options.organism, sequenceName, sampleFeaturesArray)
//            println "response ${response.features.collect { it.uniquename }}"
            response.features.collect { it.uniquename }.each() { uniquename ->
                JSONObject jsonObject = new JSONObject()
                jsonObject.put("uniquename", uniquename)
                deleteArray.add(jsonObject)
            }
            Apollo2Operations.triggerRemoveTranscript(options.destinationurl, options.username, options.password, options.organism, sequenceName, deleteArray)
        })
        threads << thread
    }
    threads.each { it.start() }

    boolean threadsRunning = true
    while(threadsRunning){
        threadsRunning = false
        threads.each { Thread it ->
            if(it?.alive){
                threadsRunning = true
            }
        }
        sleep(50l)
    }
    long iterTime = System.currentTimeMillis() - startTime
//    System.out.println("i: "+i+ " time: "+iterTime)
    timingsArray.add(iterTime)
}

long totalTime = (long) timingsArray.sum()
float meanIterTime = totalTime / (float) iter
float variance = timingsArray.sum(){
    Math.pow((it - meanIterTime),2)
}
int totalFeatures = iter * concurrency * load
int totalTransactions = iter * concurrency

//println "concurrency: ${concurrency}"
//println "iter: ${iter}"
//println "load: ${load}"
if(showHeader){
    println "concurrency,iter,load,total,mean-iter,stdv,per_iter,per_trans,per_feature"
}
println "${concurrency},${iter},${load},${totalTime/1000f},${meanIterTime/1000f},${Math.sqrt(variance)/1000f},${totalTime/(iter*1000f)},${totalTime/(totalTransactions*1000f)},${totalTime/(totalFeatures*1000f)}"

//println("total: "+totalTime/1000f)
//println("mean per iteration : "+meanIterTime / 1000f)
//println("stddev: "+Math.sqrt(variance)/1000f)
//println("per iteration : "+totalTime/(iter*1000f))
//println("per transaction: "+totalTime/(totalTransactions*1000f))
//println("per feature: "+totalTime/(totalFeatures*1000f))



