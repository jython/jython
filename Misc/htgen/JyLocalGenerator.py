"""Generates the documentation for the Jython distribution"""

import JyGenerator


class JyLocalGenerator(JyGenerator.JyGenerator):

    sitelinks = [
        ('http://www.jython.org/',               'Home'),
        ('http://www.python.org/',                'www.python.org'),
        ('http://www.jython.org/download.html',  'Download'),
        ('%(rootdir)s/index.html',                'Documentation'),
        ]

    webmaster = 0
    
    def __init__(self, file, rootdir, relthis):
        if rootdir == '..':
            rootdir = '.'
        JyGenerator.JyGenerator.__init__(self, file, rootdir, relthis)

    def get_corner(self):
        # it is important not to have newlines between the img tag and the end
        # anchor and end center tags, otherwise layout gets messed up
        return '''
<center>
    <a href="http://www.jython.org/">
    <img border="0" src="%(rootdir)s/images/jpython-new-small.gif"></a></center>''' % \
    self._d
