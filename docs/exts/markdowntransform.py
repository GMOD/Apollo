import re

def processLink(app, docname, source):
    original = source[0]
    subbed = re.sub(r"\[(.+)\]\((.+)\)", r"`\1 <\2>`_", original)
    source[0] = subbed

def setup(app):
    app.connect('source-read', processLink)
                                               
