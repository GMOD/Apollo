import re
import StringIO
import ConfigParser


def processLink(app, docname, source):
    original = source[0]


    ini_str = '[root]\n' + open('application.properties', 'r').read()
    config = ConfigParser.ConfigParser()
    ini_fp = StringIO.StringIO(ini_str)
    config.readfp(ini_fp)
    app_version = config.get('root', 'app.version')

    subbed = re.sub(r"\.md", r"\.html", original)
    subbed = re.sub(r"\|version\|", app_version, original)
    source[0] = subbed

def setup(app):
    app.connect('source-read', processLink)
                                               
